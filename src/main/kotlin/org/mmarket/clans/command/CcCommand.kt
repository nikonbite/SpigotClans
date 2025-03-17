package org.mmarket.clans.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.mmarket.clans.api.command.SuperCommand
import org.mmarket.clans.files.Messages.message
import org.mmarket.clans.system.manager.ClanManager

/** Команда для быстрой отправки сообщений в чат клана Аналог /clan chat, но короче */
class CcCommand : SuperCommand(listOf("cc", "сс"), listOf()) {
    override fun perform(player: Player, args: List<String>) {
        // Проверяем, состоит ли игрок в клане
        val clan = ClanManager.Members.getClan(player.uniqueId)
        if (clan == null) {
            player.message("general.not_in_clan")
            return
        }

        // Проверяем, приобретена ли функция чата
        if (!clan.chatPurchased) {
            player.message("clan.chat.not_purchased")
            return
        }

        // Проверяем, указано ли сообщение
        if (args.isEmpty()) {
            help(player)
            return
        }

        // Собираем сообщение из аргументов
        val message = args.joinToString(" ")

        // Отправляем сообщение всем онлайн-участникам клана
        clan.members.forEach { member ->
            val onlinePlayer = Bukkit.getPlayer(member.uuid)
            if (onlinePlayer != null && onlinePlayer.isOnline) {
                onlinePlayer.message(
                        "clan.chat.format",
                        mapOf("player" to player.name, "message" to message, "clan" to clan.name)
                )
            }
        }
    }

    override fun help(player: Player) {
        player.message("clan.chat.usage")
    }
}
