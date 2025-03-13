package org.mmarket.clans.command

import org.bukkit.entity.Player
import org.mmarket.clans.Clans
import org.mmarket.clans.api.command.SuperCommand
import org.mmarket.clans.api.command.SuperSubcommand
import org.mmarket.clans.api.utility.NamingUtility.removeColors
import org.mmarket.clans.api.utility.NamingUtility.validateClanName
import org.mmarket.clans.files.Messages.message
import org.mmarket.clans.files.Settings
import org.mmarket.clans.hook.VaultHook
import org.mmarket.clans.system.manager.ClanManager
import java.util.UUID

class ClanCommand : SuperCommand(
    listOf("clan", "сдфт"),
    listOf(
        CreateSubcommand(),
        RenameSubcommand(),
        DisbandSubcommand(),
        LeaveSubcommand(),
        InviteSubcommand(),
        CancelSubcommand(),
        KickSubcommand(),
        InvitesSubcommand(),
        InfoSubcommand(),
        TopSubcommand(),
        ScoreSubcommand(),
        TopMembersSubcommand(),
        AntiTopMembersSubcommand(),
        NewsSubcommand(),
        ListSubcommand(),
        OnlineSubcommand(),
        AdmiralSubcommand(),
        DonateSubcommand(),
        ShopSubcommand(),
        PromoteSubcommand(),
        DemoteSubcommand(),
        ChatSubcommand(),
        PartySubcommand(),
        MotdSubcommand(),
        AdSubcommand(),
        AdeSubcommand(),
    )
) {
    override fun perform(player: Player, args: List<String>) {
        performSubCommands(player, args)
    }

    override fun help(player: Player) {

    }

    object Utils {
        fun inClan(player: Player): Boolean {
            return if (ClanManager.Members.inClan(player.uniqueId)) {
                player.message("general.already_in_clan")
                true
            } else false
        }

        fun notInClan(player: Player): Boolean {
            return if (!ClanManager.Members.inClan(player.uniqueId)) {
                player.message("general.not_in_clan")
                true
            } else false
        }

        fun isNameInvalid(name: String, player: Player): Boolean {
            return if (!validateClanName(name)) {
                player.message("clan.create.name_is_not_match")
                true
            } else false
        }

        fun doNotHasNeededAmount(amount: Double, player: Player): Boolean {
            return if (!VaultHook.has(player, amount)) {
                val required = amount - VaultHook.balance(player)
                player.message("general.not_enough_money", mapOf("amount" to "${required}"))
                return true
            } else false
        }

        fun doNotHasPermission(perm: String, player: Player): Boolean {
            return if (!player.hasPermission(perm)) {
                player.message("general.no_permission")
                true
            } else false
        }
    }

    /** Создать клан */
    class CreateSubcommand : SuperSubcommand(listOf("create", "скуфеу")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.inClan(player)) return

            // Имеет ли игрок право на создание
            if (Utils.doNotHasPermission(Settings.string("permissions.create"), player)) return

            // Есть ли у игрока необходимая сумма для создания
            if (Utils.doNotHasNeededAmount(Settings.double("pricing.create"), player)) return

            // Валидация имени клана на соответствие требованиям
            val clanName = args[0]
            if (Utils.isNameInvalid(clanName, player)) return

            // Создание клана
            val colorlessClanName = removeColors(clanName)
            ClanManager.create(clanName, colorlessClanName, player.uniqueId)
            player.message("clan.create.success", mapOf("name" to clanName))
        }
    }

    /** Переименовать клан */
    class RenameSubcommand : SuperSubcommand(listOf("rename", "кутфьу")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }


        }
    }

    /** Расформировать клан */
    class DisbandSubcommand : SuperSubcommand(listOf("disband", "вшыифтв")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Покинуть клан */
    class LeaveSubcommand : SuperSubcommand(listOf("leave", "дуфму")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Пригласить игрока в клан */
    class InviteSubcommand : SuperSubcommand(listOf("invite", "штмшеу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Отменить приглашение игроку в клан */
    class CancelSubcommand : SuperSubcommand(listOf("cancel", "сфтсуд")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Выгнать игрока из клана */
    class KickSubcommand : SuperSubcommand(listOf("kick", "лшсл")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Открыть меню приглашений */
    class InvitesSubcommand : SuperSubcommand(listOf("invites", "штмшеуы")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Посмотреть информацию о клане */
    class InfoSubcommand : SuperSubcommand(listOf("info", "штащ")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Посмотреть топ-15 лучших кланов */
    class TopSubcommand : SuperSubcommand(listOf("top", "ещз")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Посмотреть клановые очки игрока */
    class ScoreSubcommand : SuperSubcommand(listOf("score", "ысщку")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Список лучших игроков клана */
    class TopMembersSubcommand : SuperSubcommand(listOf("topmembers", "ещзьуьиукы", "tm", "еь")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Список худших игроков клана */
    class AntiTopMembersSubcommand : SuperSubcommand(listOf("antitopmembers", "ытешещзьуьиукы", "atm", "феь")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Новости клана */
    class NewsSubcommand : SuperSubcommand(listOf("news", "туцы")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Список участников клана */
    class ListSubcommand : SuperSubcommand(listOf("list", "дшые")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Список участников клана в сети */
    class OnlineSubcommand : SuperSubcommand(listOf("online", "щтдшту")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Передать Адмирала соклановцу */
    class AdmiralSubcommand : SuperSubcommand(listOf("admiral", "фвьшкфд")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Пополнить бюджет клана */
    class DonateSubcommand : SuperSubcommand(listOf("donate", "вщтфеу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Открывает меню - магазин клана */
    class ShopSubcommand : SuperSubcommand(listOf("shop", "ырщз")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Повысить соклановца */
    class PromoteSubcommand : SuperSubcommand(listOf("promote", "зкщьщеу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Понизить соклановца */
    class DemoteSubcommand : SuperSubcommand(listOf("demote", "вуьщеу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Отправить сообщение в клановый чат */
    class ChatSubcommand : SuperSubcommand(listOf("chat", "срфе")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Отправить приглашение в пати всем соклановцам */
    class PartySubcommand : SuperSubcommand(listOf("party", "зфкен")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Управление MOTD клана */
    class MotdSubcommand : SuperSubcommand(listOf("motd", "ьщев")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Просмотр список кланов, которые купили рекламу (GUI) */
    class AdSubcommand : SuperSubcommand(listOf("ad", "фв")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Отредактировать купленную рекламу */
    class AdeSubcommand : SuperSubcommand(listOf("ade", "фву")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
}