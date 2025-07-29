package lightstudio.library_plugin.events

import lightstudio.library_plugin.Library_plugin
import lightstudio.library_plugin.gui.GuiManager
import lightstudio.library_plugin.gui.GuiType
import lightstudio.library_plugin.gui.LibraryGuiHolder
import lightstudio.library_plugin.manager.RatingManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.persistence.PersistentDataType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

class GuiListener(private val plugin: Library_plugin, private val guiManager: GuiManager, private val ratingManager: RatingManager) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clickedInventory = event.clickedInventory ?: return
        val holder = clickedInventory.holder

        if (holder is LibraryGuiHolder) {
            when (holder.guiType) {
                GuiType.LIBRARY -> handleLibraryGuiClick(event, player)
                GuiType.BOOK_DETAIL -> handleBookDetailGuiClick(event, player)
                GuiType.RATING -> handleRatingGuiClick(event, player)
                GuiType.EDIT_BOOK -> handleEditBookGuiClick(event, player)
            }
        }
    }

    private fun handleEditBookGuiClick(event: InventoryClickEvent, player: Player) {
        if (event.clickedInventory != event.view.topInventory) return
        event.isCancelled = true
        val clickedItem = event.currentItem ?: return

        val actionKey = plugin.namespacedKey("action")
        val action = clickedItem.itemMeta?.persistentDataContainer?.get(actionKey, PersistentDataType.STRING)

        when (action) {
            // TODO: Implement edit book GUI click handling
        }
    }

    private fun handleLibraryGuiClick(event: InventoryClickEvent, player: Player) {
        if (event.clickedInventory != event.view.topInventory) return // Only handle clicks in the top inventory
        event.isCancelled = true
        val clickedItem = event.currentItem ?: return

        val actionKey = plugin.namespacedKey("action")
        val action = clickedItem.itemMeta?.persistentDataContainer?.get(actionKey, PersistentDataType.STRING)

        when (action) {
            "prev_page" -> {
                val page = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("page"), PersistentDataType.INTEGER) ?: return
                val sortOrderString = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("sort_order"), PersistentDataType.STRING) ?: lightstudio.library_plugin.database.BookSortOrder.LATEST.name
                val sortOrder = lightstudio.library_plugin.database.BookSortOrder.valueOf(sortOrderString)
                guiManager.openLibraryGui(player, page, sortOrder)
            }
            "next_page" -> {
                val page = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("page"), PersistentDataType.INTEGER) ?: return
                val sortOrderString = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("sort_order"), PersistentDataType.STRING) ?: lightstudio.library_plugin.database.BookSortOrder.LATEST.name
                val sortOrder = lightstudio.library_plugin.database.BookSortOrder.valueOf(sortOrderString)
                guiManager.openLibraryGui(player, page, sortOrder)
            }
            "sort_latest" -> {
                guiManager.openLibraryGui(player, 1, lightstudio.library_plugin.database.BookSortOrder.LATEST)
            }
            "sort_popular" -> {
                guiManager.openLibraryGui(player, 1, lightstudio.library_plugin.database.BookSortOrder.POPULAR)
            }
            else -> {
                val bookIdKey = plugin.namespacedKey("book_id")
                val bookId = clickedItem.itemMeta?.persistentDataContainer?.get(bookIdKey, PersistentDataType.INTEGER)

                if (bookId != null) {
                    guiManager.openBookDetailGui(player, bookId)
                }
            }
        }
    }

    private fun handleBookDetailGuiClick(event: InventoryClickEvent, player: Player) {
        if (event.clickedInventory != event.view.topInventory) return // Only handle clicks in the top inventory
        event.isCancelled = true
        val clickedItem = event.currentItem ?: return

        val actionKey = plugin.namespacedKey("action")
        val action = clickedItem.itemMeta?.persistentDataContainer?.get(actionKey, PersistentDataType.STRING)

        when (action) {
            "read_book" -> {
                val bookId = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER) ?: return
                val book = plugin.bookRepository.getBookById(bookId) ?: return

                val bookItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK)
                val meta = bookItem.itemMeta as org.bukkit.inventory.meta.BookMeta
                meta.title(LegacyComponentSerializer.legacyAmpersand().deserialize(book.title))
                meta.author(LegacyComponentSerializer.legacyAmpersand().deserialize(book.authorName))
                meta.pages(book.content.split("---PAGEBREAK---").map { LegacyComponentSerializer.legacyAmpersand().deserialize(it) })
                bookItem.itemMeta = meta

                player.closeInventory()
                player.openBook(bookItem)
            }
            "rate_book" -> {
                val bookId = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER) ?: return
                val playerUuid = player.uniqueId.toString()

                if (plugin.ratingRepository.hasPlayerRated(bookId, playerUuid)) {
                    player.sendMessage(plugin.messageManager.getMessage("rating.already_rated"))
                    player.closeInventory()
                    return
                }
                guiManager.openRatingGui(player, bookId)
            }
            "back_to_library" -> {
                player.closeInventory()
                guiManager.openLibraryGui(player)
            }
            "unpublish_book" -> {
                val bookId = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER) ?: return
                plugin.bookRepository.updateBookStatus(bookId, "UNPUBLISHED")
                player.sendMessage(plugin.messageManager.getMessage("gui.book_detail.unpublish_success"))
                player.closeInventory()
                guiManager.openLibraryGui(player) // 도서관 GUI 새로고침
            }
            
        }
    }

    private fun handleRatingGuiClick(event: InventoryClickEvent, player: Player) {
        if (event.clickedInventory != event.view.topInventory) return // Only handle clicks in the top inventory
        event.isCancelled = true
        val clickedItem = event.currentItem ?: return

        val actionKey = plugin.namespacedKey("action")
        val action = clickedItem.itemMeta?.persistentDataContainer?.get(actionKey, PersistentDataType.STRING)

        when (action) {
            "rate_star" -> {
                val bookId = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER) ?: return
                val rating = clickedItem.itemMeta?.persistentDataContainer?.get(plugin.namespacedKey("rating"), PersistentDataType.INTEGER) ?: return

                val playerUuid = player.uniqueId.toString()
                if (plugin.ratingRepository.hasPlayerRated(bookId, playerUuid)) {
                    player.sendMessage(plugin.messageManager.getMessage("rating.already_rated"))
                    player.closeInventory()
                    return
                }

                ratingManager.processRating(player, bookId, rating)
                player.closeInventory()
            }
            "close_rating_gui" -> {
                player.closeInventory()
            }
        }
    }

    
}