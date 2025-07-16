package lightstudio.library_plugin.database

import lightstudio.library_plugin.Library_plugin
import org.bukkit.inventory.meta.BookMeta
import java.sql.Statement
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

data class Book(val id: Int, val title: String, val authorUuid: String, val authorName: String, val content: String)

enum class BookSortOrder {
    LATEST, // 최신순
    POPULAR // 인기순 (평균 별점 및 평가 수 기준)
}

class BookRepository(private val plugin: Library_plugin) {

    fun addBook(player: org.bukkit.entity.Player, bookMeta: BookMeta): Int {
        val sql = "INSERT INTO books (title, author_uuid, author_name, content, status) VALUES (?, ?, ?, ?, 'PUBLISHED')"
        val connection = plugin.dbManager.getConnection()
        
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setString(1, bookMeta.title)
            pstmt.setString(2, player.uniqueId.toString())
            pstmt.setString(3, player.name)
            pstmt.setString(4, bookMeta.pages().map { LegacyComponentSerializer.legacyAmpersand().serialize(it) }.joinToString("---PAGEBREAK---")) // 페이지 내용을 문자열로 저장
            
            pstmt.executeUpdate()
            
            pstmt.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                } else {
                    throw RuntimeException("책 등록 후 ID를 가져오지 못했습니다.")
                }
            }
        }
    }

    fun getPublishedBooks(page: Int, pageSize: Int, sortOrder: BookSortOrder = BookSortOrder.LATEST): List<Book> {
        val books = mutableListOf<Book>()
        val connection = plugin.dbManager.getConnection()

        val sql = when (sortOrder) {
            BookSortOrder.LATEST -> "SELECT id, title, author_uuid, author_name, content FROM books WHERE status = 'PUBLISHED' ORDER BY publication_date DESC LIMIT ? OFFSET ?"
            BookSortOrder.POPULAR -> """
                SELECT b.id, b.title, b.author_uuid, b.author_name, b.content
                FROM books b
                LEFT JOIN ratings r ON b.id = r.book_id
                WHERE b.status = 'PUBLISHED'
                GROUP BY b.id
                ORDER BY AVG(r.rating) DESC, COUNT(r.rating) DESC, b.publication_date DESC
                LIMIT ? OFFSET ?
            """
        }

        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, pageSize)
            pstmt.setInt(2, (page - 1) * pageSize)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                books.add(Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author_uuid"),
                    rs.getString("author_name"),
                    rs.getString("content")
                ))
            }
        }
        return books
    }

    fun getBookById(bookId: Int): Book? {
        val sql = "SELECT id, title, author_uuid, author_name, content FROM books WHERE id = ?"
        val connection = plugin.dbManager.getConnection()

        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, bookId)
            val rs = pstmt.executeQuery()
            if (rs.next()) {
                return Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author_uuid"),
                    rs.getString("author_name"),
                    rs.getString("content")
                )
            }
        }
        return null
    }

    fun updateRewardStatus(bookId: Int, status: Int) {
        val sql = "UPDATE books SET reward_status = ? WHERE id = ?"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, status)
            pstmt.setInt(2, bookId)
            pstmt.executeUpdate()
        }
    }

    fun getRewardStatus(bookId: Int): Int {
        val sql = "SELECT reward_status FROM books WHERE id = ?"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, bookId)
            val rs = pstmt.executeQuery()
            if (rs.next()) {
                return rs.getInt("reward_status")
            }
        }
        return 0
    }

    fun getTotalPublishedBooksCount(): Int {
        val sql = "SELECT COUNT(*) FROM books WHERE status = 'PUBLISHED'"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            val rs = pstmt.executeQuery()
            if (rs.next()) {
                return rs.getInt(1)
            }
        }
        return 0
    }

    fun updateBookStatus(bookId: Int, status: String) {
        val sql = "UPDATE books SET status = ? WHERE id = ?"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, status)
            pstmt.setInt(2, bookId)
            pstmt.executeUpdate()
        }
    }

    

    fun deleteBook(bookId: Int) {
        val sql = "DELETE FROM books WHERE id = ?"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, bookId)
            pstmt.executeUpdate()
        }
    }

    fun getBooksByAuthor(authorUuid: String): List<Book> {
        val books = mutableListOf<Book>()
        val sql = "SELECT id, title, author_uuid, author_name, content FROM books WHERE author_uuid = ?"
        val connection = plugin.dbManager.getConnection()

        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, authorUuid)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                books.add(Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author_uuid"),
                    rs.getString("author_name"),
                    rs.getString("content")
                ))
            }
        }
        return books
    }
}
