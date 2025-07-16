package lightstudio.library_plugin

import lightstudio.library_plugin.commands.AdminCommand
import lightstudio.library_plugin.commands.BookCommand
import lightstudio.library_plugin.commands.LibraryCommand
import lightstudio.library_plugin.database.BookRepository
import lightstudio.library_plugin.database.DatabaseManager
import lightstudio.library_plugin.gui.GuiManager
import lightstudio.library_plugin.manager.ConfigManager
import lightstudio.library_plugin.manager.MessageManager
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

class Library_plugin : JavaPlugin() {

    lateinit var dbManager: DatabaseManager
        private set
    lateinit var bookRepository: BookRepository
        private set
    lateinit var guiManager: GuiManager
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var configManager: ConfigManager
        private set

    fun namespacedKey(key: String) = NamespacedKey(this, key)

    lateinit var ratingRepository: lightstudio.library_plugin.database.RatingRepository
        private set
    lateinit var ratingManager: lightstudio.library_plugin.manager.RatingManager
        private set

    override fun onEnable() {
        // 설정 파일 및 메시지 매니저 초기화
        messageManager = MessageManager(this)
        configManager = ConfigManager(this)

        // 데이터베이스 매니저 초기화
        dbManager = DatabaseManager(this)
        dbManager.getConnection() // 플러그인 시작 시 데이터베이스 연결 및 테이블 생성

        // 리포지토리 및 매니저 초기화
        bookRepository = BookRepository(this)
        ratingRepository = lightstudio.library_plugin.database.RatingRepository(this)
        guiManager = GuiManager(this)
        ratingManager = lightstudio.library_plugin.manager.RatingManager(this, ratingRepository)

        // 명령어 등록
        val bookCommandExecutor = BookCommand(this)
        getCommand("책")?.setExecutor(bookCommandExecutor)
        getCommand("책")?.setTabCompleter(bookCommandExecutor)
        getCommand("도서관")?.setExecutor(LibraryCommand(this, guiManager))
        getCommand("libadmin")?.setExecutor(AdminCommand(this))

        // 이벤트 리스너 등록
        server.pluginManager.registerEvents(lightstudio.library_plugin.events.GuiListener(this, guiManager, ratingManager), this)

        logger.info(messageManager.getMessage("plugin_enabled"))
    }

    override fun onDisable() {
        // 플러그인 비활성화 시 데이터베이스 연결 종료
        dbManager.closeConnection()
        logger.info(messageManager.getMessage("plugin_disabled"))
    }
}