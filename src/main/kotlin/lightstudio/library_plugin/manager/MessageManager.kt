package lightstudio.library_plugin.manager

import lightstudio.library_plugin.Library_plugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MessageManager(private val plugin: Library_plugin) {

    private lateinit var langConfig: FileConfiguration
    private val langFile: File = File(plugin.dataFolder, "lang.yml")

    init {
        loadMessages()
    }

    fun loadMessages() {
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false)
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile)
    }

    fun getMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        var message = langConfig.getString("messages.$key", "§c메시지를 찾을 수 없습니다: $key")
        for ((placeholder, value) in placeholders) {
            message = message?.replace("%$placeholder%", value)
        }
        return message?.replace("&", "§") ?: "§c메시지 처리 오류: $key"
    }
}
