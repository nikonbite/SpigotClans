package org.mmarket.clans.command

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.Bukkit.getPlayer
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
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.interfaces.InvitesUi
import org.mmarket.clans.interfaces.SentInvitesUi
import org.mmarket.clans.interfaces.ClanShopUi

class ClanCommand :
        SuperCommand(
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
                        ConfirmSubcommand(),
                        RecallSubcommand()
                )
        ) {
    override fun perform(player: Player, args: List<String>) {
        performSubCommands(player, args)
    }

    override fun help(player: Player) {}

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
                player.message("clan.name_is_not_match")
                true
            } else false
        }

        fun doNotHasNeededAmount(amount: Double, player: Player): Boolean {
            return if (!VaultHook.has(player, amount)) {
                val required = amount - VaultHook.balance(player)
                player.message("general.not_enough_money", mapOf("amount" to "$required"))
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
            val cost = Settings.double("pricing.create")
            if (Utils.doNotHasNeededAmount(cost, player)) return

            if (args.isEmpty()) {
                player.message("clan.create.usage")
                return
            }

            // Валидация имени клана на соответствие требованиям
            val clanName = args[0]
            if (Utils.isNameInvalid(clanName, player)) return

            
            // Создание клана
            val colorlessClanName = removeColors(clanName)
            VaultHook.withdraw(player, cost)
            ClanManager.create(clanName, colorlessClanName, player.uniqueId, player.name)
            player.message("clan.create.success", mapOf("name" to clanName))
        }
    }

    /** Переименовать клан */
    class RenameSubcommand() : SuperSubcommand(listOf("rename", "кутфьу")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.rename")).priority) {
                player.message("clan.rename.no_permission")
                return
            }

            // Есть ли у игрока необходимая сумма для переименования
            val cost = Settings.double("pricing.rename")
            if (Utils.doNotHasNeededAmount(cost, player)) return

            if (args.isEmpty()) {
                player.message("clan.rename.usage")
                return
            }

            // Валидация имени клана на соответствие требованиям
            val clanName = args[0]
            if (Utils.isNameInvalid(clanName, player)) return
            val colorlessClanName = removeColors(clanName)
            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val oldName = clan.name

            player.message(
                    "clan.rename.request",
                    mapOf("old_name" to oldName, "name" to clanName, "cost" to "$cost")
            )

            // Переименование клана
            Clans.instance.actionManager.addAction(player) {
                if (Utils.doNotHasNeededAmount(cost, player)) return@addAction

                clan.name = clanName
                clan.colorlessName = colorlessClanName
                ClanManager.update(clan)

                VaultHook.withdraw(player, cost)
                player.message(
                        "clan.rename.success",
                        mapOf("oldName" to oldName, "name" to clanName)
                )
            }
        }
    }

    /** Расформировать клан */
    class DisbandSubcommand : SuperSubcommand(listOf("disband", "вшыифтв")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.disband")).priority) {
                player.message("clan.disband.no_permission")
                return
            }

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return

            player.message("clan.disband.request", mapOf("name" to clan.name))

            // Переименование клана
            Clans.instance.actionManager.addAction(player) {
                ClanManager.delete(clan.id)

                player.message("clan.disband.success", mapOf("name" to clan.name))
            }
        }
    }

    /** Покинуть клан */
    class LeaveSubcommand : SuperSubcommand(listOf("leave", "дуфму")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority != ClanMemberRole.RECRUIT.priority) {
                player.message("clan.leave.no_permission")
                return
            }

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return

            player.message("clan.leave.request", mapOf("name" to clan.name))

            // Покинуть клан
            Clans.instance.actionManager.addAction(player) {
                ClanManager.removeMember(player.uniqueId)
                player.message("clan.leave.success", mapOf("name" to clan.name))
            }
        }
    }

    /** Пригласить игрока в клан */
    class InviteSubcommand : SuperSubcommand(listOf("invite", "штмшеу")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.invite")).priority) {
                player.message("clan.invite.no_permission")
                return
            }

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return

            if (args.isEmpty()) {
                player.message("clan.invite.usage")
                return
            }

            val targetName = args[0]
            val target = getPlayer(targetName)

            if (target == null) {
                player.message("general.player_not_found", mapOf("player" to targetName))
                return
            }

            if (ClanManager.Members.inClan(target.uniqueId)) {
                player.message("general.player_already_in_clan", mapOf("player" to target.name))
                return
            }

            val maxSlots = clan.slots.calculateSlots()
            if ((clan.members.size + 1) > maxSlots) {
                player.message("clan.invite.max_slots", mapOf("max_slots" to maxSlots.toString()))
                return
            }

            if (ClanManager.createInvite(clan.id, target.uniqueId, target.name)) {
                player.message("clan.invite.success", mapOf("player" to target.name))
                target.message(
                        "clan.invite.received",
                        mapOf("clan" to clan.name, "player" to player.name)
                )
            } else {
                player.message("clan.invite.already_invited", mapOf("player" to target.name))
            }
        }
    }

    /** Отменить приглашение игроку в клан */
    class CancelSubcommand : SuperSubcommand(listOf("cancel", "сфтсуд")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.cancel")).priority) {
                player.message("clan.cancel.no_permission")
                return
            }

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return

            if (args.isEmpty()) {
                player.message("clan.cancel.usage")
                return
            }

            val targetName = args[0]
            val target = ClanManager.Invites.getInvite(clan.id, targetName)

            if (target == null) {
                player.message("clan.cancel.not_invited", mapOf("player" to targetName))
                return
            }

            if (ClanManager.removeInvite(clan.id, target.playerUuid)) {
                player.message(
                        "clan.cancel.success",
                        mapOf("player" to target.playerName)
                )

                // Уведомляем игрока, если он онлайн
                getPlayer(target.playerUuid)
                        ?.message(
                                "clan.cancel.notify",
                                mapOf("clan" to clan.name, "player" to target.playerName, "executor" to player.name)
                        )
            } else {
                player.message(
                        "clan.cancel.not_invited",
                        mapOf("player" to target.playerName)
                )
            }
        }
    }

    /** Выгнать игрока из клана */
    class KickSubcommand : SuperSubcommand(listOf("kick", "лшсл")) {
        override fun perform(player: Player, args: List<String>) {
            // Состоит ли игрок в клане
            if (Utils.notInClan(player)) return

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.kick")).priority) {
                player.message("clan.kick.no_permission")
                return
            }

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return

            if (args.isEmpty()) {
                player.message("clan.kick.usage")
                return
            }

            val targetName = args[0]
            val target = ClanManager.Members.member(clan.id, targetName) ?: return

            if (target.uuid == player.uniqueId) {
                player.message("clan.kick.self")
                return
            }

            ClanManager.removeMember(target.uuid)
            player.message("clan.kick.success", mapOf("player" to target.name))

            clan.members.forEach {
                getPlayer(it.uuid)
                        ?.message(
                                "clan.kick.notify",
                                mapOf("executor" to player.name, "player" to target.name)
                        )
            }
        }
    }

    /** Открыть меню приглашений */
    class InvitesSubcommand : SuperSubcommand(listOf("invites", "штмшеуы")) {
        override fun perform(player: Player, args: List<String>) {
            val clan = ClanManager.Members.getClan(player.uniqueId)

            if (clan == null) {
                InvitesUi(player).open()
                return
            }

            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            if (role.priority >= ClanMemberRole.get(Settings.string("actions.invites")).priority) {
                SentInvitesUi(player, clan.id).open()
                return
            } else {
                player.message("general.already_in_clan")
            }
        }
    }

    /** Посмотреть информацию о клане */
    class InfoSubcommand : SuperSubcommand(listOf("info", "штащ")) {
        override fun perform(player: Player, args: List<String>) {
            if (args.isEmpty()) {
                if (Utils.notInClan(player)) return

                val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
                val onlineMembers = clan.members.filter { getPlayer(it.uuid)?.isOnline == true }.size

                val clans = ClanManager.clans()
                val topPosition = clans.sortedByDescending { it.score }.indexOf(clan) + 1

                player.message(
                        "clan.info.info",
                        mapOf(
                            "clan" to clan.name,
                            "owner" to (getOfflinePlayer(clan.owner).name ?: ""),
                            "created_at" to clan.createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                            "score" to clan.score.toString(),
                            "top_position" to topPosition.toString(),
                            "treasury" to clan.treasury.toString(),
                            "members" to clan.members.size.toString(),
                            "max_members" to clan.slots.calculateSlots().toString(),
                            "online" to onlineMembers.toString()
                        )
                )
            } else {}
        }
    }

    /** Посмотреть топ-15 лучших кланов */
    class TopSubcommand : SuperSubcommand(listOf("top", "ещз")) {
        override fun perform(player: Player, args: List<String>) {
            val clans = ClanManager.clans().toMutableList()
            clans.sortByDescending { it.score }

            player.message("clan.top.header")
            clans.take(15).forEachIndexed { index, clan ->
                player.message(
                    "clan.top.format", 
                    mapOf(
                        "index" to (index + 1).toString(),
                        "clan" to clan.name,
                        "score" to clan.score.toString(),
                        "members" to clan.members.size.toString()
                    )
                )
            }
            player.message("clan.top.footer")
        }
    }

    /** Посмотреть клановые очки игрока */
    class ScoreSubcommand : SuperSubcommand(listOf("score", "ысщку")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Список лучших игроков клана */
    class TopMembersSubcommand : SuperSubcommand(listOf("topmembers", "ещзьуьиукы", "tm", "еь")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Список худших игроков клана */
    class AntiTopMembersSubcommand :
            SuperSubcommand(listOf("antitopmembers", "ытешещзьуьиукы", "atm", "феь")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Новости клана */
    class NewsSubcommand : SuperSubcommand(listOf("news", "туцы")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val news = clan.news.toList()

            player.message("clan.news.header", mapOf("clan" to clan.name))
            news.forEach { player.sendMessage(it) }
            player.message("clan.news.footer", mapOf("count" to news.size.toString()))
        }
    }

    /** Список участников клана */
    class ListSubcommand : SuperSubcommand(listOf("list", "дшые")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val members = clan.members.toList()
            val roledMembersMap = mutableMapOf<ClanMemberRole, MutableList<String>>()
            members.forEach {
                if (roledMembersMap[it.role] == null) roledMembersMap[it.role] = mutableListOf()

                roledMembersMap[it.role]?.add(it.name)
            }

            player.message("clan.list.header", mapOf("clan" to clan.name))
            roledMembersMap.entries.sortedByDescending { it.key.priority }.forEach { (role, names)
                ->
                player.message(
                        "clan.list.format",
                        mapOf("role" to role.toString(), "names" to names.joinToString(", "))
                )
            }
            player.message("clan.list.footer", mapOf("count" to members.size.toString()))
        }
    }

    /** Список участников клана в сети */
    class OnlineSubcommand : SuperSubcommand(listOf("online", "щтдшту")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val members = clan.members.toList()
            val roledMembersMap = mutableMapOf<ClanMemberRole, MutableList<String>>()
            members.forEach {
                if (roledMembersMap[it.role] == null) roledMembersMap[it.role] = mutableListOf()

                if (getPlayer(it.uuid)?.isOnline == true) roledMembersMap[it.role]?.add(it.name)
            }

            player.message("clan.online.header", mapOf("clan" to clan.name))
            roledMembersMap.entries.sortedByDescending { it.key.priority }.forEach { (role, names)
                ->
                player.message(
                        "clan.online.format",
                        mapOf("role" to role.toString(), "names" to names.joinToString(", "))
                )
            }
            player.message(
                    "clan.online.footer",
                    mapOf(
                            "count" to
                                    members
                                            .filter { getPlayer(it.uuid)?.isOnline == true }
                                            .size
                                            .toString()
                    )
            )
        }
    }

    /** Передать Адмирала соклановцу */
    class AdmiralSubcommand : SuperSubcommand(listOf("admiral", "фвьшкфд")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Пополнить бюджет клана */
    class DonateSubcommand : SuperSubcommand(listOf("donate", "вщтфеу")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.donate")).priority) {
                player.message("clan.donate.no_permission")
                return
            }

            val amount = args[0].toLongOrNull() ?: return
            if (amount <= 0) {
                player.message("clan.donate.invalid_amount")
                return
            }

            if (Utils.doNotHasNeededAmount(amount.toDouble(), player)) return

            VaultHook.withdraw(player, amount.toDouble())
            ClanManager.addTreasury(clan.id, amount)
            player.message("clan.donate.success", mapOf("amount" to amount.toString()))
        }
    }

    /** Открывает меню - магазин клана */
    class ShopSubcommand : SuperSubcommand(listOf("shop", "ырщз")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            ClanShopUi(player).open()
        }
    }

    /** Повысить соклановца */
    class PromoteSubcommand : SuperSubcommand(listOf("promote", "зкщьщеу")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.promote")).priority) {
                player.message("clan.promote.no_permission")
                return
            }

            val targetName = args[0]
            val target = ClanManager.Members.member(clan.id, targetName) ?: return

            if (target.uuid == player.uniqueId) {
                player.message("clan.promote.self")
                return
            }

            ClanManager.updateMemberRole(target.uuid, target.role.next())
            player.message("clan.promote.success", mapOf("player" to target.name))

            clan.news.add(
                    "[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}] ${player.name} повысил ${target.name} до ${target.role.next().toString()}"
            )
            ClanManager.update(clan)
        }
    }

    /** Понизить соклановца */
    class DemoteSubcommand : SuperSubcommand(listOf("demote", "вуьщеу")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val role = ClanManager.Members.role(player.uniqueId)
            if (role == null) {
                return
            }

            // Проверка на соответствие минимальной роли
            if (role.priority < ClanMemberRole.get(Settings.string("actions.demote")).priority) {
                player.message("clan.demote.no_permission")
                return
            }

            val targetName = args[0]
            val target = ClanManager.Members.member(clan.id, targetName) ?: return

            if (target.uuid == player.uniqueId) {
                player.message("clan.demote.self")
                return
            }

            ClanManager.updateMemberRole(target.uuid, target.role.previous())
            player.message("clan.demote.success", mapOf("player" to target.name))

            clan.news.add(
                    "[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}] ${player.name} понизил ${target.name} до ${target.role.previous().toString()}"
            )
            ClanManager.update(clan)
        }
    }

    /** Отправить сообщение в клановый чат */
    class ChatSubcommand : SuperSubcommand(listOf("chat", "срфе")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Отправить приглашение в пати всем соклановцам */
    class PartySubcommand : SuperSubcommand(listOf("party", "зфкен")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Управление MOTD клана */
    class MotdSubcommand : SuperSubcommand(listOf("motd", "ьщев")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Просмотр список кланов, которые купили рекламу (GUI) */
    class AdSubcommand : SuperSubcommand(listOf("ad", "фв")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Отредактировать купленную рекламу */
    class AdeSubcommand : SuperSubcommand(listOf("ade", "фву")) {
        override fun perform(player: Player, args: List<String>) {}
    }

    /** Поодтвердить действие */
    class ConfirmSubcommand : SuperSubcommand(listOf("confirm", "фвы")) {
        override fun perform(player: Player, args: List<String>) {
            Clans.instance.actionManager.confirmAction(player)
        }
    }

    /** Отменить действие */
    class RecallSubcommand : SuperSubcommand(listOf("recall", "кусфдд")) {
        override fun perform(player: Player, args: List<String>) {
            Clans.instance.actionManager.cancelAction(player)
        }
    }
}
