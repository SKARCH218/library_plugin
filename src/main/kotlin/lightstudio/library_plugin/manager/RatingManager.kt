package lightstudio.library_plugin.manager

import lightstudio.library_plugin.Library_plugin
import lightstudio.library_plugin.database.RatingRepository
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RatingManager(private val plugin: Library_plugin, private val ratingRepository: RatingRepository) {

    fun processRating(player: Player, bookId: Int, rating: Int) {
        if (ratingRepository.hasPlayerRated(bookId, player.uniqueId.toString())) {
            player.sendMessage(plugin.messageManager.getMessage("rating.already_rated"))
            return
        }

        ratingRepository.addRating(bookId, player.uniqueId.toString(), rating)
        player.sendMessage(plugin.messageManager.getMessage("rating.success", mapOf("rating" to rating.toString())))

        val book = plugin.bookRepository.getBookById(bookId) ?: return
        val author = plugin.server.getOfflinePlayer(java.util.UUID.fromString(book.authorUuid))

        val ratingInfo = ratingRepository.getAverageRating(bookId)
        val currentAvgRating = ratingInfo.average
        val rewardStatus = plugin.bookRepository.getRewardStatus(bookId)

        // 첫 4점 이상 돌파 보상 (10명 이상 평가, 평균 4점 이상)
        if (ratingInfo.count >= 10 && currentAvgRating >= 4.0 && rewardStatus == 0) {
            plugin.bookRepository.updateRewardStatus(bookId, 1) // 4점 이상 보상 지급됨
            if (author.isOnline) {
                author.player?.sendMessage(plugin.messageManager.getMessage("reward.first_4_star_achieved", mapOf("book_title" to book.title)))
                plugin.configManager.getRewardCommands("first_4_star").forEach { cmd ->
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd.replace("%player%", author.name ?: ""))
                }
            }
        }

        // 5점 획득 보상 (10명 이상 평가, 평균 4점 이상)
        if (rating == 5 && ratingInfo.count >= 10 && currentAvgRating >= 4.0 && rewardStatus < 2) {
            plugin.bookRepository.updateRewardStatus(bookId, 2) // 5점 보상 지급됨
            if (author.isOnline) {
                author.player?.sendMessage(plugin.messageManager.getMessage("reward.five_star_achieved", mapOf("book_title" to book.title)))
                plugin.configManager.getRewardCommands("five_star").forEach { cmd ->
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd.replace("%player%", author.name ?: ""))
                }
            }
        }
    }
}
