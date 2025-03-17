package org.mmarket.clans.command

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.mmarket.clans.Clans
import org.mmarket.clans.api.command.SuperCommand
import org.mmarket.clans.api.command.SuperSubcommand
import org.mmarket.clans.api.utility.NamingUtility.removeColors
import org.mmarket.clans.api.utility.NamingUtility.validateClanName
import org.mmarket.clans.files.Messages.message
import org.mmarket.clans.files.Settings
import org.mmarket.clans.hook.VaultHook
import org.mmarket.clans.interfaces.ClanShopUi
import org.mmarket.clans.interfaces.InvitesUi
import org.mmarket.clans.interfaces.SentInvitesUi
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.system.model.ClanModel

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
        if (args.isEmpty()) {
            help(player)
            return
        }
        
        performSubCommands(player, args)
    }

    override fun help(player: Player) {
        player.message("clan.header")
        
        // Проверяем, состоит ли игрок в клане
        val inClan = ClanManager.Members.inClan(player.uniqueId)
        val role = if (inClan) ClanManager.Members.role(player.uniqueId) else null
        
        // Команды для всех игроков
        player.message("clan.info.usage")
        player.message("clan.top.usage")
        player.message("clan.score.usage")
        
        if (!inClan) {
            // Команды для игроков без клана
            if (player.hasPermission(Settings.string("permissions.create"))) {
                player.message("clan.create.usage")
            }
            player.message("clan.invites.usage")
        } else {
            // Базовые команды для всех участников клана
            player.message("clan.list.usage")
            player.message("clan.online.usage")
            player.message("clan.news.usage")
            player.message("clan.topmembers.usage")
            player.message("clan.antitopmembers.usage")
            player.message("clan.shop.usage")
            
            // Проверяем, приобретена ли функция чата
            val clan = ClanManager.Members.getClan(player.uniqueId)
            if (clan != null && clan.chatPurchased) {
                player.message("clan.chat.usage")
            }
            
            // Проверяем, приобретена ли функция MOTD
            if (clan != null && clan.motdPurchased) {
                player.message("clan.motd.usage")
            }
            
            // Проверяем, приобретена ли функция party
            if (clan != null && clan.partyPurchased) {
                player.message("clan.party.usage")
            }
            
            // Команды в зависимости от роли
            if (role != null) {
                // Рекрут может покинуть клан и пополнить казну
                if (role.priority >= ClanMemberRole.RECRUIT.priority) {
                    player.message("clan.leave.usage")
                    player.message("clan.donate.usage")
                }
                
                // Старшина может приглашать и отменять приглашения
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.invite")).priority) {
                    player.message("clan.invite.usage")
                }
                
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.cancel")).priority) {
                    player.message("clan.cancel.usage")
                }
                
                // Коммодор может выгнать, повысить и понизить
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.kick")).priority) {
                    player.message("clan.kick.usage")
                }
                
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.promote")).priority) {
                    player.message("clan.promote.usage")
                }
                
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.demote")).priority) {
                    player.message("clan.demote.usage")
                }
                
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.invites")).priority) {
                    player.message("clan.invites.usage")
                }
                
                // Адмирал может переименовать и распустить клан
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.rename")).priority) {
                    player.message("clan.rename.usage")
                }
                
                if (role.priority >= ClanMemberRole.get(Settings.string("actions.disband")).priority) {
                    player.message("clan.disband.usage")
                    player.message("clan.admiral.usage")
                }
            }
        }
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

        fun getClanOrSendError(player: Player): ClanModel? {
            val clan = ClanManager.Members.getClan(player.uniqueId)
            if (clan == null) {
                player.message("general.player_not_in_clan")
                return null
            }
            return clan
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
                        mapOf("old_name" to oldName, "name" to clanName)
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
            val target = Bukkit.getPlayer(targetName)

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
                player.message("clan.cancel.success", mapOf("player" to target.playerName))

                // Уведомляем игрока, если он онлайн
                Bukkit.getPlayer(target.playerUuid)
                        ?.message(
                                "clan.cancel.notify",
                                mapOf(
                                        "clan" to clan.name,
                                        "player" to target.playerName,
                                        "executor" to player.name
                        )
                )
            } else {
                player.message("clan.cancel.not_invited", mapOf("player" to target.playerName))
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
                Bukkit.getPlayer(it.uuid)
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
                // Показываем информацию о клане игрока
                if (Utils.notInClan(player)) return

                val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
                showClanInfo(player, clan)
            } else {
                // Показываем информацию о клане указанного игрока
                val targetName = args[0]
                val targetPlayer = Bukkit.getOfflinePlayer(targetName)

                if (targetPlayer == null) {
                    player.message("general.player_not_found", mapOf("player" to targetName))
                    return
                }

                val clan = ClanManager.Members.getClan(targetPlayer.uniqueId)
                if (clan == null) {
                    player.message("general.player_not_in_clan")
                    return
                }

                showClanInfo(player, clan)
            }
        }

        private fun showClanInfo(player: Player, clan: ClanModel) {
            val onlineMembers =
                    clan.members.filter { Bukkit.getPlayer(it.uuid)?.isOnline == true }.size

            // Рассчитываем общий счет клана
            val totalScore = ClanManager.Scores.calculateClanScore(clan.id)

            // Получаем позицию клана в топе
                val clans = ClanManager.clans()
            val clansWithScores =
                    clans
                            .map { c -> Pair(c, ClanManager.Scores.calculateClanScore(c.id)) }
                            .sortedByDescending { it.second }

            val topPosition = clansWithScores.indexOfFirst { it.first.id == clan.id } + 1

                player.message(
                        "clan.info.info",
                        mapOf(
                            "clan" to clan.name,
                            "owner" to (Bukkit.getOfflinePlayer(clan.owner).name ?: ""),
                            "created_at" to
                                    clan.createdAt.format(
                                            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                    ),
                            "score" to totalScore.toString(),
                            "top_position" to topPosition.toString(),
                            "treasury" to clan.treasury.toString(),
                            "members" to clan.members.size.toString(),
                            "max_members" to clan.slots.calculateSlots().toString(),
                            "online" to onlineMembers.toString()
                        )
                )
        }
    }

    /** Посмотреть топ-15 лучших кланов */
    class TopSubcommand : SuperSubcommand(listOf("top", "ещз")) {
        override fun perform(player: Player, args: List<String>) {
            val clans = ClanManager.clans().toMutableList()

            // Рассчитываем общий счет для каждого клана и сортируем
            val clansWithScores =
                    clans
                            .map { clan ->
                                val totalScore = ClanManager.Scores.calculateClanScore(clan.id)
                                Pair(clan, totalScore)
                            }
                            .sortedByDescending { it.second }

            player.message("clan.top.header")
            clansWithScores.take(15).forEachIndexed { index, (clan, score) ->
                player.message(
                    "clan.top.format", 
                    mapOf(
                        "index" to (index + 1).toString(),
                        "clan" to clan.name,
                                "score" to score.toString(),
                        "members" to clan.members.size.toString()
                    )
                )
            }
            player.message("clan.top.footer")
        }
    }

    /** Посмотреть клановые очки игрока */
    class ScoreSubcommand : SuperSubcommand(listOf("score", "ысщку")) {
        override fun perform(player: Player, args: List<String>) {
            if (args.isEmpty()) {
                // Показываем очки текущего игрока
                val playerUuid = player.uniqueId
                val playerScores = ClanManager.Scores.getPlayerScores(playerUuid)

                if (playerScores == null || playerScores.scores.isEmpty()) {
                    player.message("clan.score.no_scores")
                    return
                }

                val scoreEntries = playerScores.scores.entries.sortedByDescending { it.value }

                player.message("clan.score.header", mapOf("player" to player.name))
                scoreEntries.forEach { (type, score) ->
                    player.message(
                            "clan.score.format",
                            mapOf("type" to type, "score" to score.toString())
                    )
                }
                player.message("clan.score.footer", mapOf("count" to scoreEntries.size.toString()))
            } else {
                // Показываем очки указанного игрока
                val targetName = args[0]

                // Ищем игрока по имени
                val targetPlayer = Bukkit.getOfflinePlayer(targetName)
                if (targetPlayer == null) {
                    player.message("clan.score.player_not_found", mapOf("player" to targetName))
                    return
                }

                val targetUuid = targetPlayer.uniqueId
                val playerScores = ClanManager.Scores.getPlayerScores(targetUuid)

                if (playerScores == null || playerScores.scores.isEmpty()) {
                    player.message("clan.score.no_scores_other", mapOf("player" to targetName))
                    return
                }

                val scoreEntries = playerScores.scores.entries.sortedByDescending { it.value }

                player.message("clan.score.header", mapOf("player" to targetName))
                scoreEntries.forEach { (type, score) ->
                    player.message(
                            "clan.score.format",
                            mapOf("type" to type, "score" to score.toString())
                    )
                }
                player.message("clan.score.footer", mapOf("count" to scoreEntries.size.toString()))
            }
        }
    }

    /** Список лучших игроков клана */
    class TopMembersSubcommand : SuperSubcommand(listOf("topmembers", "ещзьуьиукы", "tm", "еь")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val scoreType = if (args.isNotEmpty()) args[0] else "total"

            if (scoreType == "total") {
                // Показываем топ по общему количеству очков
                val memberScores = ClanManager.Scores.getClanMembersScores(clan.id)
                val sortedMembers =
                        memberScores
                                .map { scoreModel ->
                                    Pair(scoreModel.playerName, scoreModel.scores.values.sum())
                                }
                                .sortedByDescending { it.second }
                                .take(10)

                player.message(
                        "clan.topmembers.header",
                        mapOf("clan" to clan.name, "type" to "общим")
                )
                sortedMembers.forEachIndexed { index, (name, score) ->
                    player.message(
                            "clan.topmembers.format",
                            mapOf(
                                    "index" to (index + 1).toString(),
                                    "player" to name,
                                    "score" to score.toString()
                            )
                    )
                }
                player.message(
                        "clan.topmembers.footer",
                        mapOf("count" to clan.members.size.toString())
                )
            } else {
                // Показываем топ по конкретному типу очков
                val topMembers = ClanManager.Scores.getClanTopByScoreType(clan.id, scoreType, 10)

                player.message(
                        "clan.topmembers.header",
                        mapOf("clan" to clan.name, "type" to scoreType)
                )
                topMembers.forEachIndexed { index, (name, score) ->
                    player.message(
                            "clan.topmembers.format",
                            mapOf(
                                    "index" to (index + 1).toString(),
                                    "player" to name,
                                    "score" to score.toString()
                            )
                    )
                }
                player.message(
                        "clan.topmembers.footer",
                        mapOf("count" to clan.members.size.toString())
                )
            }
        }
    }

    /** Список худших игроков клана */
    class AntiTopMembersSubcommand :
            SuperSubcommand(listOf("antitopmembers", "ытешещзьуьиукы", "atm", "феь")) {
        override fun perform(player: Player, args: List<String>) {
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
            val scoreType = if (args.isNotEmpty()) args[0] else "total"

            if (scoreType == "total") {
                // Показываем антитоп по общему количеству очков
                val memberScores = ClanManager.Scores.getClanMembersScores(clan.id)
                val sortedMembers =
                        memberScores
                                .map { scoreModel ->
                                    Pair(scoreModel.playerName, scoreModel.scores.values.sum())
                                }
                                .sortedBy { it.second } // Сортируем по возрастанию
                                .take(10)

                player.message(
                        "clan.antitopmembers.header",
                        mapOf("clan" to clan.name, "type" to "общим")
                )
                sortedMembers.forEachIndexed { index, (name, score) ->
                    player.message(
                            "clan.antitopmembers.format",
                            mapOf(
                                    "index" to (index + 1).toString(),
                                    "player" to name,
                                    "score" to score.toString()
                            )
                    )
                }
                player.message(
                        "clan.antitopmembers.footer",
                        mapOf("count" to clan.members.size.toString())
                )
            } else {
                // Показываем антитоп по конкретному типу очков
                val memberScores = ClanManager.Scores.getClanMembersScores(clan.id)
                val sortedMembers =
                        memberScores
                                .map { scoreModel ->
                                    Pair(scoreModel.playerName, scoreModel.scores[scoreType] ?: 0)
                                }
                                .sortedBy { it.second } // Сортируем по возрастанию
                                .take(10)

                player.message(
                        "clan.antitopmembers.header",
                        mapOf("clan" to clan.name, "type" to scoreType)
                )
                sortedMembers.forEachIndexed { index, (name, score) ->
                    player.message(
                            "clan.antitopmembers.format",
                            mapOf(
                                    "index" to (index + 1).toString(),
                                    "player" to name,
                                    "score" to score.toString()
                            )
                    )
                }
                player.message(
                        "clan.antitopmembers.footer",
                        mapOf("count" to clan.members.size.toString())
                )
            }
        }
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
            if (args.isEmpty()) {
                // Показываем список участников клана игрока
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
                showClanMembers(player, clan)
            } else {
                // Показываем список участников клана указанного игрока
                val targetName = args[0]
                val targetPlayer = Bukkit.getOfflinePlayer(targetName)

                if (targetPlayer == null) {
                    player.message("general.player_not_found", mapOf("player" to targetName))
                    return
                }

                val clan = ClanManager.Members.getClan(targetPlayer.uniqueId)
                if (clan == null) {
                    player.message("general.player_not_in_clan")
                    return
                }

                showClanMembers(player, clan)
            }
        }

        private fun showClanMembers(player: Player, clan: ClanModel) {
            val members = clan.members.toList()
            val roledMembersMap = mutableMapOf<ClanMemberRole, MutableList<Pair<String, Boolean>>>()

            // Группируем участников по ролям и отмечаем их онлайн-статус
            members.forEach { member ->
                if (roledMembersMap[member.role] == null) {
                    roledMembersMap[member.role] = mutableListOf()
                }

                val isOnline = Bukkit.getPlayer(member.uuid)?.isOnline == true
                roledMembersMap[member.role]?.add(Pair(member.name, isOnline))
            }

            player.message("clan.list.header", mapOf("clan" to clan.name))

            // Выводим участников по ролям, сортируя роли по приоритету
            roledMembersMap.entries.sortedByDescending { it.key.priority }.forEach {
                    (role, namesList) ->
                if (namesList.isNotEmpty()) {
                    // Форматируем имена с цветами в зависимости от онлайн-статуса
                    val formattedNames =
                            namesList.joinToString(", ") { (name, isOnline) ->
                                if (isOnline) "&a$name" else "&c$name"
                            }

                player.message(
                        "clan.list.format",
                            mapOf("role" to role.role, "names" to formattedNames)
                )
            }
            }

            player.message("clan.list.footer", mapOf("count" to members.size.toString()))
        }
    }

    /** Список участников клана в сети */
    class OnlineSubcommand : SuperSubcommand(listOf("online", "щтдшту")) {
        override fun perform(player: Player, args: List<String>) {
            if (args.isEmpty()) {
                // Показываем список онлайн участников клана игрока
            if (Utils.notInClan(player)) return

            val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
                showOnlineMembers(player, clan)
            } else {
                // Показываем список онлайн участников клана указанного игрока
                val targetName = args[0]
                val targetPlayer = Bukkit.getOfflinePlayer(targetName)

                if (targetPlayer == null) {
                    player.message("general.player_not_found", mapOf("player" to targetName))
                    return
                }

                val clan = ClanManager.Members.getClan(targetPlayer.uniqueId)
                if (clan == null) {
                    player.message("general.player_not_in_clan")
                    return
                }

                showOnlineMembers(player, clan)
            }
        }

        private fun showOnlineMembers(player: Player, clan: ClanModel) {
            val members = clan.members.toList()
            val roledMembersMap = mutableMapOf<ClanMemberRole, MutableList<String>>()

            // Группируем только онлайн участников по ролям
            members.forEach { member ->
                if (Bukkit.getPlayer(member.uuid)?.isOnline == true) {
                    if (roledMembersMap[member.role] == null) {
                        roledMembersMap[member.role] = mutableListOf()
                    }

                    roledMembersMap[member.role]?.add(member.name)
                }
            }

            player.message("clan.online.header", mapOf("clan" to clan.name))

            // Выводим онлайн участников по ролям, сортируя роли по приоритету
            roledMembersMap.entries.sortedByDescending { it.key.priority }.forEach {
                    (role, namesList) ->
                if (namesList.isNotEmpty()) {
                    // Все имена зеленые, так как все онлайн
                    val formattedNames = namesList.joinToString(", ") { name -> "&a$name" }

                player.message(
                        "clan.online.format",
                            mapOf("role" to role.role, "names" to formattedNames)
                    )
                }
            }

            val onlineCount = members.count { Bukkit.getPlayer(it.uuid)?.isOnline == true }
            player.message("clan.online.footer", mapOf("count" to onlineCount.toString()))
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

            // Добавляем новость в клан
            val newsMessage =
                    "[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}] ${player.name} пополнил казну клана на $amount СтарКоинов"
            clan.news.add(newsMessage)
            ClanManager.update(clan)
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
        override fun perform(player: Player, args: List<String>) {
            // Проверяем, состоит ли игрок в клане
            val clan = Utils.getClanOrSendError(player) ?: return

            // Проверяем, приобретена ли функция чата
            if (!clan.chatPurchased) {
                player.message("clan.chat.not_purchased")
                return
            }

            // Проверяем, указано ли сообщение
            if (args.isEmpty()) {
                player.message("clan.chat.usage")
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
                            mapOf("player" to player.name, "message" to message)
                    )
                }
            }
        }
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
