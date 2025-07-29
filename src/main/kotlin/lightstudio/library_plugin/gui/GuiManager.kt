package lightstudio.library_plugin.gui

import lightstudio.library_plugin.Library_plugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiManager(private val plugin: Library_plugin) {

    private val pageSize = 45 // 54칸 인벤토리에서 책이 표시될 슬롯 수


    fun openLibraryGui(player: Player, page: Int = 1, sortOrder: lightstudio.library_plugin.database.BookSortOrder = lightstudio.library_plugin.database.BookSortOrder.LATEST) {
        val inventory = Bukkit.createInventory(LibraryGuiHolder(GuiType.LIBRARY, Bukkit.createInventory(null, 54)), 54, plugin.configManager.getGuiTitle("library_gui_title", "도서관 (페이지: %page%)").replace("%page%", page.toString()))

        // 데이터베이스에서 책 목록 가져오기
        val books = plugin.bookRepository.getPublishedBooks(page, pageSize, sortOrder)

        // GUI에 책 아이템 채우기
        books.forEachIndexed { index, book ->
            val bookItem = ItemStack(Material.WRITTEN_BOOK)
            val meta = bookItem.itemMeta
            val ratingInfo = plugin.ratingRepository.getAverageRating(book.id)
            meta.setDisplayName("§e${book.title}")
            meta.lore = listOf(
                plugin.messageManager.getMessage("gui.library.book_lore.author", mapOf("author_name" to book.authorName)),
                plugin.messageManager.getMessage("gui.library.book_lore.rating", mapOf("star_rating" to getStarRating(ratingInfo.average), "rating_count" to ratingInfo.count.toString())),
                "",
                plugin.messageManager.getMessage("gui.library.book_lore.click_for_details")
            )
            // 아이템에 책 ID 저장
            meta.persistentDataContainer.set(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER, book.id)
            bookItem.itemMeta = meta
            inventory.setItem(index, bookItem)
        }

        // 페이지네이션 버튼 추가
        val totalBooks = plugin.bookRepository.getTotalPublishedBooksCount()
        val totalPages = (totalBooks + pageSize - 1) / pageSize

        // 이전 페이지 버튼 (슬롯 45)
        val fillEmptySlots = plugin.configManager.getBoolean("gui_settings.items-all-structure-void", false)

        // 이전 페이지 버튼 (슬롯 45)
        val prevButtonMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else (if (page > 1) Material.ARROW else Material.GRAY_STAINED_GLASS_PANE)
        val prevButton = ItemStack(prevButtonMaterial)
        val prevMeta = prevButton.itemMeta
        prevMeta.setDisplayName(if (page > 1) plugin.messageManager.getMessage("gui.library.prev_page_button") else plugin.messageManager.getMessage("gui.library.no_prev_page"))
        prevMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "prev_page")
        prevMeta.persistentDataContainer.set(plugin.namespacedKey("page"), PersistentDataType.INTEGER, if (page > 1) page - 1 else page)
        prevMeta.persistentDataContainer.set(plugin.namespacedKey("sort_order"), PersistentDataType.STRING, sortOrder.name)
        prevButton.itemMeta = prevMeta
        inventory.setItem(45, prevButton)

        // 다음 페이지 버튼 (슬롯 53)
        val nextButtonMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else (if (page < totalPages) Material.ARROW else Material.GRAY_STAINED_GLASS_PANE)
        val nextButton = ItemStack(nextButtonMaterial)
        val nextMeta = nextButton.itemMeta
        nextMeta.setDisplayName(if (page < totalPages) plugin.messageManager.getMessage("gui.library.next_page_button") else plugin.messageManager.getMessage("gui.library.no_next_page"))
        nextMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "next_page")
        nextMeta.persistentDataContainer.set(plugin.namespacedKey("page"), PersistentDataType.INTEGER, if (page < totalPages) page + 1 else page)
        nextMeta.persistentDataContainer.set(plugin.namespacedKey("sort_order"), PersistentDataType.STRING, sortOrder.name)
        nextButton.itemMeta = nextMeta
        inventory.setItem(53, nextButton)

        // 정렬 버튼 추가
        // 최신순 정렬 버튼 (슬롯 48)
        val latestSortButtonMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.CLOCK
        val latestSortButton = ItemStack(latestSortButtonMaterial)
        val latestSortMeta = latestSortButton.itemMeta
        latestSortMeta.setDisplayName(plugin.messageManager.getMessage("gui.library.sort_latest_button"))
        latestSortMeta.lore = if (sortOrder == lightstudio.library_plugin.database.BookSortOrder.LATEST) listOf(plugin.messageManager.getMessage("gui.library.current_sort_indicator")) else null
        latestSortMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "sort_latest")
        latestSortButton.itemMeta = latestSortMeta
        inventory.setItem(48, latestSortButton)

        // 인기순 정렬 버튼 (슬롯 50)
        val popularSortButtonMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.NETHER_STAR
        val popularSortButton = ItemStack(popularSortButtonMaterial)
        val popularSortMeta = popularSortButton.itemMeta
        popularSortMeta.setDisplayName(plugin.messageManager.getMessage("gui.library.sort_popular_button"))
        popularSortMeta.lore = if (sortOrder == lightstudio.library_plugin.database.BookSortOrder.POPULAR) listOf(plugin.messageManager.getMessage("gui.library.current_sort_indicator")) else null
        popularSortMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "sort_popular")
        popularSortButton.itemMeta = popularSortMeta
        inventory.setItem(50, popularSortButton)

        val fillerMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.BLACK_STAINED_GLASS_PANE
        val fillerItem = ItemStack(fillerMaterial)
        val fillerMeta = fillerItem.itemMeta
        fillerMeta.setDisplayName(" ") // 빈 이름
        fillerItem.itemMeta = fillerMeta

        for (i in 0 until inventory.size) { // 모든 슬롯을 순회
            if (inventory.getItem(i) == null) { // 아이템이 없는 경우에만 채움
                inventory.setItem(i, fillerItem)
            }
        }



        player.openInventory(inventory)
    }

    private fun getStarRating(average: Double): String {
        val fullStars = average.toInt()
        val halfStar = if (average - fullStars >= 0.5) "½" else ""
        val emptyStars = 5 - fullStars - (if (halfStar.isNotEmpty()) 1 else 0)
        return "★".repeat(fullStars) + halfStar + "☆".repeat(emptyStars)
    }

    fun openBookDetailGui(player: Player, bookId: Int) {
        val book = plugin.bookRepository.getBookById(bookId) ?: return

        val inventory = Bukkit.createInventory(LibraryGuiHolder(GuiType.BOOK_DETAIL, Bukkit.createInventory(null, 9)), 9, plugin.configManager.getGuiTitle("book_detail_gui_title", "책 정보: %book_title%").replace("%book_title%", book.title))

        val fillEmptySlots = plugin.configManager.getBoolean("gui_settings.items-all-structure-void", false)

        // 책 읽기 아이템 (2번 슬롯)
        val readBookMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.BOOK
        val readBookItem = ItemStack(readBookMaterial)
        val readMeta = readBookItem.itemMeta
        readMeta.setDisplayName(plugin.messageManager.getMessage("gui.book_detail.read_book_button"))
        readMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "read_book")
        readMeta.persistentDataContainer.set(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER, bookId)
        readBookItem.itemMeta = readMeta
        inventory.setItem(2, readBookItem)

        // 별점 주기 아이템 (4번 슬롯)
        val rateBookMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.NETHER_STAR
        val rateBookItem = ItemStack(rateBookMaterial)
        val rateMeta = rateBookItem.itemMeta
        rateMeta.setDisplayName(plugin.messageManager.getMessage("gui.book_detail.rate_book_button"))
        rateMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "rate_book")
        rateMeta.persistentDataContainer.set(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER, bookId)
        rateBookItem.itemMeta = rateMeta
        inventory.setItem(4, rateBookItem)

        // 뒤로가기 아이템 (8번 슬롯)
        val backButtonMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.ARROW
        val backButton = ItemStack(backButtonMaterial)
        val backMeta = backButton.itemMeta
        backMeta.setDisplayName(plugin.messageManager.getMessage("gui.book_detail.back_button"))
        backMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "back_to_library")
        backButton.itemMeta = backMeta
        inventory.setItem(8, backButton)

        // 작성자일 경우 추가 버튼
        if (player.uniqueId.toString() == book.authorUuid) {
            // 게시 해제 아이템 (0번 슬롯)
            val unpublishMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.REDSTONE_BLOCK
            val unpublishItem = ItemStack(unpublishMaterial)
            val unpublishMeta = unpublishItem.itemMeta
            unpublishMeta.setDisplayName(plugin.messageManager.getMessage("gui.book_detail.unpublish_button"))
            unpublishMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "unpublish_book")
            unpublishMeta.persistentDataContainer.set(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER, bookId)
            unpublishItem.itemMeta = unpublishMeta
            inventory.setItem(0, unpublishItem)
        }

        val fillerMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.BLACK_STAINED_GLASS_PANE
        val fillerItem = ItemStack(fillerMaterial)
        val fillerMeta = fillerItem.itemMeta
        fillerMeta.setDisplayName(" ") // 빈 이름
        fillerItem.itemMeta = fillerMeta

        for (i in 0 until inventory.size) { // 모든 슬롯을 순회
            if (inventory.getItem(i) == null) { // 아이템이 없는 경우에만 채움
                inventory.setItem(i, fillerItem)
            }
        }



        player.openInventory(inventory)
    }

    fun openRatingGui(player: Player, bookId: Int) {
        val inventory = Bukkit.createInventory(LibraryGuiHolder(GuiType.RATING, Bukkit.createInventory(null, 9)), 9, plugin.configManager.getGuiTitle("rating_gui_title", "별점 주기: 책 ID %book_id%").replace("%book_id%", bookId.toString()))

        val fillEmptySlots = plugin.configManager.getBoolean("gui_settings.items-all-structure-void", false)

        for (i in 1..5) {
            val starMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.NETHER_STAR
            val starItem = ItemStack(starMaterial)
            val meta = starItem.itemMeta
            meta.setDisplayName(plugin.messageManager.getMessage("gui.rating.star_item_name", mapOf("rating" to i.toString())))
            meta.lore = listOf(plugin.messageManager.getMessage("gui.rating.star_item_lore", mapOf("rating" to i.toString())))
            meta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "rate_star")
            meta.persistentDataContainer.set(plugin.namespacedKey("book_id"), PersistentDataType.INTEGER, bookId)
            meta.persistentDataContainer.set(plugin.namespacedKey("rating"), PersistentDataType.INTEGER, i)
            starItem.itemMeta = meta
            inventory.setItem(i + 1, starItem) // 슬롯 2부터 6까지 (1점부터 5점)
        }

        val closeButtonMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.BARRIER
        val closeItem = ItemStack(closeButtonMaterial)
        val closeMeta = closeItem.itemMeta
        closeMeta.setDisplayName(plugin.messageManager.getMessage("gui.rating.cancel_button"))
        closeMeta.persistentDataContainer.set(plugin.namespacedKey("action"), PersistentDataType.STRING, "close_rating_gui")
        closeItem.itemMeta = closeMeta
        inventory.setItem(8, closeItem) // 슬롯 8에 취소 버튼

        val fillerMaterial = if (fillEmptySlots) Material.STRUCTURE_VOID else Material.BLACK_STAINED_GLASS_PANE
        val fillerItem = ItemStack(fillerMaterial)
        val fillerMeta = fillerItem.itemMeta
        fillerMeta.setDisplayName(" ") // 빈 이름
        fillerItem.itemMeta = fillerMeta

        for (i in 0 until inventory.size) { // 모든 슬롯을 순회
            if (inventory.getItem(i) == null) { // 아이템이 없는 경우에만 채움
                inventory.setItem(i, fillerItem)
            }
        }



        player.openInventory(inventory)
    }


}