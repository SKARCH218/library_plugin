package lightstudio.library_plugin.database

import lightstudio.library_plugin.Library_plugin

data class RatingInfo(val average: Double, val count: Int)

class RatingRepository(private val plugin: Library_plugin) {

    fun addRating(bookId: Int, raterUuid: String, rating: Int) {
        val sql = "INSERT INTO ratings (book_id, rater_uuid, rating) VALUES (?, ?, ?)"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, bookId)
            pstmt.setString(2, raterUuid)
            pstmt.setInt(3, rating)
            pstmt.executeUpdate()
        }
    }

    fun hasPlayerRated(bookId: Int, raterUuid: String): Boolean {
        val sql = "SELECT 1 FROM ratings WHERE book_id = ? AND rater_uuid = ?"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, bookId)
            pstmt.setString(2, raterUuid)
            val rs = pstmt.executeQuery()
            return rs.next()
        }
    }

    fun getAverageRating(bookId: Int): RatingInfo {
        val sql = "SELECT AVG(rating), COUNT(rating) FROM ratings WHERE book_id = ?"
        plugin.dbManager.getConnection().prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, bookId)
            val rs = pstmt.executeQuery()
            if (rs.next()) {
                return RatingInfo(rs.getDouble(1), rs.getInt(2))
            }
        }
        return RatingInfo(0.0, 0)
    }
}
