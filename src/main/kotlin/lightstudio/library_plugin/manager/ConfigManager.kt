package lightstudio.library_plugin.manager

import lightstudio.library_plugin.Library_plugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: Library_plugin) {

    private lateinit var pluginConfig: FileConfiguration
    private val configFile: File = File(plugin.dataFolder, "config.yml")

    init {
        loadConfig()
    }

    fun loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
        pluginConfig = YamlConfiguration.loadConfiguration(configFile)
    }

    fun getRewardCommands(rewardType: String): List<String> {
        return pluginConfig.getStringList("rewards.$rewardType.commands")
    }

    fun getRewardAmount(rewardType: String): Double {
        return pluginConfig.getDouble("rewards.$rewardType.amount", 0.0)
    }

    fun getRewardItem(rewardType: String): String? {
        return pluginConfig.getString("rewards.$rewardType.item")
    }

    fun getRewardPermission(rewardType: String): String? {
        return pluginConfig.getString("rewards.$rewardType.permission")
    }

    fun getInt(path: String, defaultValue: Int): Int {
        return pluginConfig.getInt(path, defaultValue)
    }

    fun getBoolean(path: String, defaultValue: Boolean): Boolean {
        return pluginConfig.getBoolean(path, defaultValue)
    }

    fun getString(path: String, defaultValue: String): String {
        return pluginConfig.getString(path, defaultValue) ?: defaultValue
    }

    fun getGuiTitle(key: String, defaultValue: String): String {
        return pluginConfig.getString("gui_titles.$key", defaultValue) ?: defaultValue
    }
}