package lightstudio.library_plugin.database

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseManager(private val plugin: JavaPlugin) {

    private var connection: Connection? = null

    fun getConnection(): Connection {
        if (connection == null || connection!!.isClosed) {
            connect()
        }
        return connection!!
    }

    private fun connect() {
        try {
            val dataFolder = plugin.dataFolder
            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }
            val dbFile = File(dataFolder, "library.db")
            val url = "jdbc:sqlite:${dbFile.absolutePath}"
            connection = DriverManager.getConnection(url)
            plugin.logger.info("데이터베이스에 성공적으로 연결되었습니다.")
            createTables()
        } catch (e: SQLException) {
            plugin.logger.severe("데이터베이스 연결에 실패했습니다: ${e.message}")
        }
    }

    fun closeConnection() {
        try {
            if (connection != null && !connection!!.isClosed) {
                connection!!.close()
                plugin.logger.info("데이터베이스 연결이 종료되었습니다.")
            }
        } catch (e: SQLException) {
            plugin.logger.severe("데이터베이스 연결 종료 중 오류가 발생했습니다: ${e.message}")
        }
    }

    private fun createTables() {
        val booksTable = """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                author_uuid TEXT NOT NULL,
                author_name TEXT NOT NULL,
                content TEXT NOT NULL,
                publication_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                status TEXT NOT NULL CHECK(status IN ('PUBLISHED', 'UNPUBLISHED')),
                reward_status INTEGER DEFAULT 0
            );
        """
        val ratingsTable = """
            CREATE TABLE IF NOT EXISTS ratings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                rater_uuid TEXT NOT NULL,
                rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
                FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
            );
        """
        try {
            getConnection().createStatement().use { stmt ->
                stmt.execute(booksTable)
                stmt.execute(ratingsTable)
                plugin.logger.info("테이블이 성공적으로 생성되거나 이미 존재합니다.")
            }
        } catch (e: SQLException) {
            plugin.logger.severe("테이블 생성에 실패했습니다: ${e.message}")
        }
    }
}
