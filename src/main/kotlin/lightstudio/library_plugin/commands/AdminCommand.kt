package lightstudio.library_plugin.commands

import lightstudio.library_plugin.Library_plugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminCommand(private val plugin: Library_plugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.messageManager.getMessage("admin.player_only"))
            return true
        }

        if (!sender.hasPermission("library.admin")) {
            sender.sendMessage(plugin.messageManager.getMessage("no_permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.messageManager.getMessage("admin.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                plugin.messageManager.loadMessages()
                plugin.configManager.loadConfig()
                sender.sendMessage(plugin.messageManager.getMessage("admin.reloaded"))
            }
            "setreward" -> {
                if (args.size < 3) {
                    sender.sendMessage(plugin.messageManager.getMessage("admin.setreward_usage"))
                    return true
                }
                val bookId = args[1].toIntOrNull()
                val status = args[2].toIntOrNull()

                if (bookId == null || status == null || status !in 0..2) {
                    sender.sendMessage(plugin.messageManager.getMessage("admin.setreward_invalid_args"))
                    return true
                }

                try {
                    plugin.bookRepository.updateRewardStatus(bookId, status)
                    sender.sendMessage(plugin.messageManager.getMessage("admin.setreward_success", mapOf("book_id" to bookId.toString(), "status" to status.toString())))
                } catch (e: Exception) {
                    sender.sendMessage(plugin.messageManager.getMessage("admin.setreward_error", mapOf("error_message" to (e.message ?: "알 수 없는 오류"))))
                    plugin.logger.severe("보상 상태 설정 오류: " + e.stackTraceToString())
                }
            }
            "givebookreward" -> {
                if (args.size < 2) {
                    sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_usage"))
                    return true
                }
                val bookId = args[1].toIntOrNull()
                if (bookId == null) {
                    sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_invalid_args"))
                    return true
                }

                try {
                    val book = plugin.bookRepository.getBookById(bookId)
                    if (book == null) {
                        sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_book_not_found", mapOf("book_id" to bookId.toString())))
                        return true
                    }

                    val rewardStatus = plugin.bookRepository.getRewardStatus(bookId)
                    if (rewardStatus < 2) { // 2는 5점 보상이 지급된 상태
                        sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_not_eligible", mapOf("book_id" to bookId.toString())))
                        return true
                    }

                    val author = plugin.server.getOfflinePlayer(java.util.UUID.fromString(book.authorUuid))
                    if (author.isOnline) {
                        plugin.configManager.getRewardCommands("five_star").forEach { cmd ->
                            plugin.server.dispatchCommand(plugin.server.consoleSender, cmd.replace("%player%", author.name ?: ""))
                        }
                        sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_success", mapOf("book_title" to book.title, "player_name" to (author.name ?: "알 수 없는 플레이어"))))
                    } else {
                        sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_author_offline", mapOf("player_name" to (author.name ?: "알 수 없는 플레이어"))))
                    }

                } catch (e: Exception) {
                    sender.sendMessage(plugin.messageManager.getMessage("admin.givebookreward_error", mapOf("error_message" to (e.message ?: "알 수 없는 오류"))))
                    plugin.logger.severe("책 보상 수동 지급 오류: " + e.stackTraceToString())
                }
            }
            else -> {
                sender.sendMessage(plugin.messageManager.getMessage("admin.unknown_command"))
            }
        }

        return true
    }
}
