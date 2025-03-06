package org.mmarket.clans.api.command

import org.bukkit.entity.Player

abstract class SuperSubcommand(val names: List<String>) {
    abstract fun perform(player: Player, args: List<String>);
}