package lightstudio.library_plugin.commands

import lightstudio.library_plugin.Library_plugin
import lightstudio.library_plugin.gui.GuiManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LibraryCommand(private val plugin: Library_plugin, private val guiManager: GuiManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.messageManager.getMessage("player_only_command"))
            return true
        }

        guiManager.openLibraryGui(sender)
        return true
    }
}
