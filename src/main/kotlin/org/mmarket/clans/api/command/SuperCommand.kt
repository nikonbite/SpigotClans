package org.mmarket.clans.api.command

import org.bukkit.Bukkit.getLogger
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class SuperCommand(val names: List<String>, val subcommands: List<SuperSubcommand>) : Command(names[0], "", "", names.drop(0)) {

    abstract fun perform(player: Player, args: List<String>);
    abstract fun help(player: Player);

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>?): Boolean {
        if (args.isNullOrEmpty()) {
            return true
        }

        if (sender !is Player) {
            getLogger().severe("Команду может исполнить только игрок!")
            return true
        }

        perform(sender, args.toList())
        return true
    }

    fun performSubCommands(player: Player, args: List<String>) {
        val subcommand = subcommands.firstOrNull { it.names.contains(args[0]) }

        if (subcommand == null) {
            help(player)
            return
        }

        subcommand.perform(player, args)
    }
}