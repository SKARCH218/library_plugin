package lightstudio.library_plugin.commands

import lightstudio.library_plugin.Library_plugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BookMeta

class BookCommand(private val plugin: Library_plugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.messageManager.getMessage("player_only_command"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.messageManager.getMessage("book_command.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "등록" -> {
                val item = sender.inventory.itemInMainHand
                if (item.type != Material.WRITTEN_BOOK) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.register.no_book_in_hand"))
                    return true
                }

                val bookMeta = item.itemMeta as BookMeta

                if (bookMeta.title.isNullOrBlank()) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.register.empty_title"))
                    return true
                }
                if (bookMeta.pages().isEmpty()) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.register.empty_content"))
                    return true
                }

                try {
                    val bookId = plugin.bookRepository.addBook(sender, bookMeta)
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.register.success", mapOf("book_id" to bookId.toString())))
                } catch (e: Exception) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.register.error", mapOf("error_message" to (e.message ?: "알 수 없는 오류"))))
                    plugin.logger.severe("책 등록 오류: " + e.stackTraceToString())
                }
            }
            "삭제" -> {
                if (args.size < 2) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.delete.usage"))
                    return true
                }
                val bookId = args[1].toIntOrNull()
                if (bookId == null) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.invalid_book_id"))
                    return true
                }

                val existingBook = plugin.bookRepository.getBookById(bookId)

                if (existingBook == null) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.not_found"))
                    return true
                }

                if (existingBook.authorUuid != sender.uniqueId.toString()) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.no_permission_to_delete"))
                    return true
                }

                try {
                    plugin.bookRepository.deleteBook(bookId)
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.delete.success", mapOf("book_id" to bookId.toString())))
                } catch (e: Exception) {
                    sender.sendMessage(plugin.messageManager.getMessage("book_command.delete.error", mapOf("error_message" to (e.message ?: "알 수 없는 오류"))))
                    plugin.logger.severe("책 삭제 오류: " + e.stackTraceToString())
                }
            }
            else -> {
                sender.sendMessage(plugin.messageManager.getMessage("book_command.unknown_subcommand"))
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String>? {
        if (sender !is Player) return null

        return when (args.size) {
            1 -> {
                // 첫 번째 인자: 등록, 수정, 삭제, 수정완료
                mutableListOf("등록", "삭제").filter { it.startsWith(args[0], true) }.toMutableList()
            }
            2 -> {
                // 두 번째 인자: 수정, 삭제, 수정완료 시 책 ID
                when (args[0].lowercase()) {
                    "삭제" -> {
                        plugin.bookRepository.getBooksByAuthor(sender.uniqueId.toString()).map { it.id.toString() }.filter { it.startsWith(args[1], true) }.toMutableList()
                    }
                    else -> null
                }
            }
            else -> null
        }
    }
}
