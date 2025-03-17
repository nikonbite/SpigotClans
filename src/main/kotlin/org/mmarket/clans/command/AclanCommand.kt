package org.mmarket.clans.command

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.mmarket.clans.Clans
import org.mmarket.clans.api.command.SuperCommand
import org.mmarket.clans.api.command.SuperSubcommand
import org.mmarket.clans.api.utility.NamingUtility.removeColors
import org.mmarket.clans.api.utility.NamingUtility.validateClanName
import org.mmarket.clans.files.Messages.message
import org.mmarket.clans.files.Settings
import org.mmarket.clans.hook.VaultHook
import org.mmarket.clans.interfaces.AdvertiseUi
import org.mmarket.clans.interfaces.ClanShopUi
import org.mmarket.clans.interfaces.InvitesUi
import org.mmarket.clans.interfaces.SentInvitesUi
import org.mmarket.clans.system.manager.AdvertisementManager
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.system.model.ClanModel
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.SkinTrait

/**
 * Административная команда для управления кланами
 * Доступна только для администраторов и модераторов
 */
class AclanCommand :
        SuperCommand(
    listOf("aclan", "фсдфт"),
    listOf(
        KickSubcommand(),
        DisbandSubcommand(),
                        ClearMotdSubcommand(),
                        DemoteSubcommand(),
        PromoteSubcommand(),
        ShopSubcommand(),
                        NpcSpawnSubcommand(),
                        NpcRemoveSubcommand(),
                        NpcListSubcommand(),
                        NpcNameSubcommand(),
                        NpcSkinSubcommand(),
        RenameSubcommand(),
        UuidSubcommand(),
                        AdRemoveSubcommand(),
                        AdInvitesSubcommand(),
                        ScoreSubcommand()
                )
        ) {
    override fun perform(player: Player, args: List<String>) {
        // Проверяем права администратора
        if (!player.hasPermission(Settings.string("permissions.admin"))) {
            player.message("general.no_permission")
            return
        }

        if (args.isEmpty()) {
            help(player)
            return
        }

        performSubCommands(player, args)
    }

    override fun help(player: Player) {
        player.message("aclan.header")
        
        // Выводим список всех доступных команд
        player.message("aclan.kick.usage")
        player.message("aclan.disband.usage")
        player.message("aclan.clearmotd.usage")
        player.message("aclan.demote.usage")
        player.message("aclan.promote.usage")
        player.message("aclan.shop.usage")
        player.message("aclan.npc.usage")
        player.message("aclan.rename.usage")
        player.message("aclan.uuid.usage")
        player.message("aclan.ad.usage")
        player.message("aclan.score.usage")
    }

    private object Utils {
        fun getClanByIdentifier(identifier: String): ClanModel? {
            // Пробуем получить клан по UUID
            try {
                val uuid = UUID.fromString(identifier)
                val clan = ClanManager.get(uuid)
                if (clan != null) {
                    return clan
                }
            } catch (e: IllegalArgumentException) {
                // Если не UUID, пробуем по colorless_name
                val clans = ClanManager.clans()
                return clans.find { it.colorlessName.equals(identifier, ignoreCase = true) }
            }
            
            return null
        }
    }

    /** Принудительно удалить участника из клана */
    class KickSubcommand : SuperSubcommand(listOf("kick", "лшсл")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.kick.usage")
                return
            }

            val targetName = args[0]
            val target = Bukkit.getOfflinePlayer(targetName)

            if (target == null || !target.hasPlayedBefore()) {
                player.message("general.player_not_found", mapOf("player" to targetName))
                return
            }

            val clan = ClanManager.Members.getClan(target.uniqueId)
            if (clan == null) {
                player.message("general.player_not_in_clan")
                return
            }

            // Удаляем игрока из клана
            ClanManager.removeMember(target.uniqueId)
            player.message("aclan.kick.success", mapOf("player" to targetName, "clan" to clan.name))

            // Уведомляем игрока, если он онлайн
            Bukkit.getPlayer(target.uniqueId)?.message(
                "aclan.kick.notify", 
                mapOf("admin" to player.name)
            )
        }
    }

    /** Принудительно удалить клан */
    class DisbandSubcommand : SuperSubcommand(listOf("disband", "вшыифтв")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.disband.usage")
                return
            }

            val identifier = args[0]
            val clan = Utils.getClanByIdentifier(identifier)
                

            if (clan == null) {
                player.message("aclan.disband.not_found", mapOf("identifier" to identifier))
                return
            }

            // Сохраняем имя клана для сообщения
            val clanName = clan.name

            // Удаляем клан
            ClanManager.delete(clan.id)
            player.message("aclan.disband.success", mapOf("clan" to clanName))
        }
    }

    /** Принудительно очистить клановое приветствие */
    class ClearMotdSubcommand : SuperSubcommand(listOf("clearmotd", "сдуфкьщев")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.clearmotd.usage")
                return
            }

            val identifier = args[0]
            val clan = Utils.getClanByIdentifier(identifier)


            if (clan == null) {
                player.message("aclan.clearmotd.not_found", mapOf("identifier" to identifier))
                return
            }

            // Очищаем MOTD
            clan.motd = ""
            ClanManager.update(clan)
            player.message("aclan.clearmotd.success", mapOf("clan" to clan.name))
        }
    }

    /** Принудительно понизить участника клана */
    class DemoteSubcommand : SuperSubcommand(listOf("demote", "вуьщеу")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.demote.usage")
                return
            }

            val targetName = args[0]
            val target = Bukkit.getOfflinePlayer(targetName)

            if (target == null || !target.hasPlayedBefore()) {
                player.message("general.player_not_found", mapOf("player" to targetName))
                return
            }

            val clan = ClanManager.Members.getClan(target.uniqueId)
            if (clan == null) {
                player.message("general.player_not_in_clan")
                return
            }

            val member = ClanManager.Members.member(clan.id, target.uniqueId)
            if (member == null) {
                player.message("general.player_not_in_clan")
                return
            }

            // Нельзя понизить рекрута
            if (member.role == ClanMemberRole.RECRUIT) {
                player.message("aclan.demote.already_lowest", mapOf("player" to targetName))
                return
            }

            // Понижаем участника
            val oldRole = member.role
            val newRole = oldRole.previous()
            member.role = newRole
            ClanManager.Members.update(clan.id, member)

            player.message(
                "aclan.demote.success", 
                mapOf(
                    "player" to targetName, 
                    "old_role" to oldRole.role, 
                    "new_role" to newRole.role
                )
            )

            // Уведомляем игрока, если он онлайн
            Bukkit.getPlayer(target.uniqueId)?.message(
                "aclan.demote.notify", 
                mapOf(
                    "admin" to player.name, 
                    "old_role" to oldRole.role, 
                    "new_role" to newRole.role
                )
            )
        }
    }

    /** Принудительно повысить участника клана */
    class PromoteSubcommand : SuperSubcommand(listOf("promote", "зкщьщеу")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.promote.usage")
                return
            }

            val targetName = args[0]
            val target = Bukkit.getOfflinePlayer(targetName)

            if (target == null || !target.hasPlayedBefore()) {
                player.message("general.player_not_found", mapOf("player" to targetName))
                return
            }

            val clan = ClanManager.Members.getClan(target.uniqueId)
            if (clan == null) {
                player.message("general.player_not_in_clan")
                return
            }

            val member = ClanManager.Members.member(clan.id, target.uniqueId)
            if (member == null) {
                player.message("general.player_not_in_clan")
                return
            }

            // Нельзя повысить адмирала
            if (member.role == ClanMemberRole.ADMIRAL) {
                player.message("aclan.promote.already_highest", mapOf("player" to targetName))
                return
            }

            // Повышаем участника
            val oldRole = member.role
            val newRole = oldRole.next()
            
            // Если повышаем до адмирала, то текущий адмирал становится коммодором
            if (newRole == ClanMemberRole.ADMIRAL) {
                val currentAdmiral = clan.members.find { it.role == ClanMemberRole.ADMIRAL }
                if (currentAdmiral != null) {
                    currentAdmiral.role = ClanMemberRole.COMMODORE
                    ClanManager.Members.update(clan.id, currentAdmiral)
                    
                    // Обновляем владельца клана
                    clan.owner = member.uuid
                    ClanManager.update(clan)
                    
                    // Уведомляем бывшего адмирала, если он онлайн
                    Bukkit.getPlayer(currentAdmiral.uuid)?.message(
                        "aclan.promote.admiral_demoted", 
                        mapOf("admin" to player.name, "new_admiral" to targetName)
                    )
                }
            }
            
            member.role = newRole
            ClanManager.Members.update(clan.id, member)

            player.message(
                "aclan.promote.success", 
                mapOf(
                    "player" to targetName, 
                    "old_role" to oldRole.role, 
                    "new_role" to newRole.role
                )
            )

            // Уведомляем игрока, если он онлайн
            Bukkit.getPlayer(target.uniqueId)?.message(
                "aclan.promote.notify", 
                mapOf(
                    "admin" to player.name, 
                    "old_role" to oldRole.role, 
                    "new_role" to newRole.role
                )
            )
        }
    }

    /** Принудительно взаимодействовать с магазином клана */
    class ShopSubcommand : SuperSubcommand(listOf("shop", "ырщз")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.shop.usage")
                return
            }

            val identifier = args[0]
            val clan = Utils.getClanByIdentifier(identifier)


            if (clan == null) {
                player.message("aclan.shop.not_found", mapOf("identifier" to identifier))
                return
            }

            // Открываем магазин клана
            ClanShopUi(player, clan.id).open()
        }
    }

    /** Заспавнить NPC с рекламой кланов */
    class NpcSpawnSubcommand : SuperSubcommand(listOf("npc", "тзс")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.npc.usage")
                return
            }

            if (args[0] != "spawn" || args.size < 2) {
                player.message("aclan.npc.spawn.usage")
                return
            }

            // Проверяем, установлен ли плагин Citizens
            if (player.server.pluginManager.getPlugin("Citizens") == null) {
                player.message("aclan.npc.citizens_not_found")
                return
            }

            val npcName = args[1]
            
            // Создаем NPC
            val npcRegistry = CitizensAPI.getNPCRegistry()
            val npc = npcRegistry.createNPC(org.bukkit.entity.EntityType.PLAYER, npcName)
            
            // Устанавливаем позицию NPC
            npc.spawn(player.location)
            
            player.message("aclan.npc.spawn.success", mapOf("name" to npcName))
        }
    }

    /** Удалить NPC */
    class NpcRemoveSubcommand : SuperSubcommand(listOf("npc", "тзс")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.npc.usage")
                return
            }

            if (args[0] != "remove" || args.size < 2) {
                player.message("aclan.npc.remove.usage")
                return
            }

            // Проверяем, установлен ли плагин Citizens
            if (player.server.pluginManager.getPlugin("Citizens") == null) {
                player.message("aclan.npc.citizens_not_found")
                return
            }

            val npcName = args[1]
            
            // Ищем NPC по имени
            val npcRegistry = CitizensAPI.getNPCRegistry()
            val npc = npcRegistry.toList().find { it.name.equals(npcName, ignoreCase = true) }
            
            if (npc == null) {
                player.message("aclan.npc.remove.not_found", mapOf("name" to npcName))
                return
            }
            
            // Удаляем NPC
            npc.destroy()
            
            player.message("aclan.npc.remove.success", mapOf("name" to npcName))
        }
    }

    /** Просмотр всех заспавненых NPC */
    class NpcListSubcommand : SuperSubcommand(listOf("npc", "тзс")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.npc.usage")
                return
            }

            if (args[0] != "list") {
                player.message("aclan.npc.list.usage")
                return
            }

            // Проверяем, установлен ли плагин Citizens
            if (player.server.pluginManager.getPlugin("Citizens") == null) {
                player.message("aclan.npc.citizens_not_found")
                return
            }

            // Получаем список всех NPC
            val npcRegistry = CitizensAPI.getNPCRegistry()
            val npcs = npcRegistry.toList()
            
            if (npcs.isEmpty()) {
                player.message("aclan.npc.list.empty")
                return
            }
            
            player.message("aclan.npc.list.header")
            
            // Выводим список NPC
            npcs.forEachIndexed { index, npc ->
                val location = npc.storedLocation
                val world = location.world.name
                val x = location.x.toInt()
                val y = location.y.toInt()
                val z = location.z.toInt()
                
                player.message(
                    "aclan.npc.list.format", 
                    mapOf(
                        "index" to (index + 1).toString(),
                        "world" to world,
                        "x" to x.toString(),
                        "y" to y.toString(),
                        "z" to z.toString(),
                        "name" to npc.name
                    )
                )
            }
            
            player.message("aclan.npc.list.footer")
        }
    }

    /** Задать ВСЕМ NPC отображаемое над ними имя */
    class NpcNameSubcommand : SuperSubcommand(listOf("npc", "тзс")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.npc.usage")
                return
            }

            if (args[0] != "name" || args.size < 2) {
                player.message("aclan.npc.name.usage")
                return
            }

            // Проверяем, установлен ли плагин Citizens
            if (player.server.pluginManager.getPlugin("Citizens") == null) {
                player.message("aclan.npc.citizens_not_found")
                return
            }

            val newName = args.subList(1, args.size).joinToString(" ")
            
            // Получаем список всех NPC
            val npcRegistry = CitizensAPI.getNPCRegistry()
            val npcs = npcRegistry.toList()
            
            if (npcs.isEmpty()) {
                player.message("aclan.npc.name.no_npcs")
                return
            }
            
            // Устанавливаем имя всем NPC
            npcs.forEach { it.name = newName }
            
            player.message("aclan.npc.name.success", mapOf("name" to newName, "count" to npcs.size.toString()))
        }
    }

    /** Задать ВСЕМ NPC отображаемый скин */
    class NpcSkinSubcommand : SuperSubcommand(listOf("npc", "тзс")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.npc.usage")
                return
            }

            if (args[0] != "skin" || args.size < 2) {
                player.message("aclan.npc.skin.usage")
                return
            }

            // Проверяем, установлен ли плагин Citizens
            if (player.server.pluginManager.getPlugin("Citizens") == null) {
                player.message("aclan.npc.citizens_not_found")
                return
            }

            val skinName = args[1]
            
            // Получаем список всех NPC
            val npcRegistry = CitizensAPI.getNPCRegistry()
            val npcs = npcRegistry.toList()
            
            if (npcs.isEmpty()) {
                player.message("aclan.npc.skin.no_npcs")
                return
            }
            
            // Устанавливаем скин всем NPC
            npcs.forEach { npc ->
                if (npc.isSpawned && npc.entity.type == org.bukkit.entity.EntityType.PLAYER) {
                    val skinTrait = npc.getOrAddTrait(SkinTrait::class.java)
                    skinTrait.setSkinName(skinName)
                }
            }
            
            player.message("aclan.npc.skin.success", mapOf("skin" to skinName, "count" to npcs.size.toString()))
        }
    }

    /** Принудительно переименовать клан */
    class RenameSubcommand : SuperSubcommand(listOf("rename", "кутфьу")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.size < 2) {
                player.message("aclan.rename.usage")
                return
            }

            val identifier = args[0]
            val newName = args[1]
            
            // Проверяем валидность имени
            if (!validateClanName(newName)) {
                player.message("clan.name_is_not_match")
                return
            }
            
            val clan = Utils.getClanByIdentifier(identifier)


            if (clan == null) {
                player.message("aclan.rename.not_found", mapOf("identifier" to identifier))
                return
            }

            // Сохраняем старое имя для сообщения
            val oldName = clan.name
            
            // Переименовываем клан
            clan.name = newName
            clan.colorlessName = removeColors(newName)
            ClanManager.update(clan)
            
            player.message("aclan.rename.success", mapOf("old_name" to oldName, "new_name" to newName))
        }
    }

    /** Посмотреть UUID клана */
    class UuidSubcommand : SuperSubcommand(listOf("uuid", "ггшв")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.uuid.usage")
                return
            }

            val targetName = args[0]
            val target = Bukkit.getOfflinePlayer(targetName)

            if (target == null || !target.hasPlayedBefore()) {
                player.message("general.player_not_found", mapOf("player" to targetName))
                return
            }

            val clan = ClanManager.Members.getClan(target.uniqueId)
            if (clan == null) {
                player.message("general.player_not_in_clan")
                return
            }

            player.message(
                "aclan.uuid.success", 
                mapOf(
                    "player" to targetName, 
                    "clan" to clan.name, 
                    "uuid" to clan.id.toString()
                )
            )
        }
    }

    /** Принудительно удалить рекламу клана */
    class AdRemoveSubcommand : SuperSubcommand(listOf("ad", "фв")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty() || args[0] != "remove" || args.size < 2) {
                player.message("aclan.ad.remove.usage")
                return
            }

            val identifier = args[1]
            val clan = Utils.getClanByIdentifier(identifier)

            if (clan == null) {
                player.message("aclan.ad.remove.not_found", mapOf("identifier" to identifier))
                return
            }

            // Проверяем, есть ли реклама у клана
            if (!AdvertisementManager.hasActiveAdvertisement(clan.id)) {
                player.message("aclan.ad.remove.no_ad", mapOf("clan" to clan.name))
                return
            }

            // Удаляем рекламу
            AdvertisementManager.removeAdvertisement(clan.id)
            player.message("aclan.ad.remove.success", mapOf("clan" to clan.name))
        }
    }

    /** Принудительно взаимодействовать с заявками в клан */
    class AdInvitesSubcommand : SuperSubcommand(listOf("ad", "фв")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty() || args[0] != "invites" || args.size < 2) {
                player.message("aclan.ad.invites.usage")
                return
            }

            val identifier = args[1]
            val clan = Utils.getClanByIdentifier(identifier)


            if (clan == null) {
                player.message("aclan.ad.invites.not_found", mapOf("identifier" to identifier))
                return
            }

            // Открываем интерфейс приглашений
            SentInvitesUi(player, clan.id).open()
        }
    }

    /** Управление очками игроков */
    class ScoreSubcommand : SuperSubcommand(listOf("score", "ысщку")) {
        override fun perform(player: Player, args: List<String>) {
            if (!player.hasPermission(Settings.string("permissions.admin"))) {
                player.message("general.no_permission")
                return
            }

            if (args.isEmpty()) {
                player.message("aclan.score.usage")
                return
            }

            // Если первый аргумент "info", то показываем информацию об очках игрока
            if (args[0].equals("info", ignoreCase = true)) {
                if (args.size < 2) {
                    player.message("aclan.score.info.usage")
                    return
                }

                val targetName = args[1]
                val target = Bukkit.getOfflinePlayer(targetName)
                if (target == null || !target.hasPlayedBefore()) {
                    player.message("general.player_not_found", mapOf("player" to targetName))
                    return
                }

                // Получаем очки игрока
                val scoreModel = ClanManager.Scores.getPlayerScores(target.uniqueId)
                if (scoreModel == null || scoreModel.scores.isEmpty()) {
                    player.message("aclan.score.info.no_scores", mapOf("player" to targetName))
                    return
                }

                // Выводим информацию об очках
                player.message("aclan.score.info.header", mapOf("player" to targetName))
                
                // Сортируем очки по убыванию
                val sortedScores = scoreModel.scores.entries
                    .sortedByDescending { it.value }
                
                sortedScores.forEach { (gameType, score) ->
                    player.message(
                        "aclan.score.info.entry", 
                        mapOf(
                            "game_type" to gameType,
                            "score" to score.toString()
                        )
                    )
                }
                
                // Выводим общую сумму очков
                val totalScore = scoreModel.scores.values.sum()
                player.message(
                    "aclan.score.info.total", 
                    mapOf("total" to totalScore.toString())
                )
                
                return
            }

            if (args.size < 4) {
                player.message("aclan.score.usage")
                return
            }

            val action = args[0].lowercase()
            val targetName = args[1]
            val gameType = args[2]
            val amountStr = args[3]
            
            // Проверяем, что действие корректное
            if (action !in listOf("add", "remove", "set")) {
                player.message("aclan.score.invalid_action")
                return
            }
            
            // Проверяем, что тип игры существует
            val availableGames = Settings.getStringList("values.games")
            if (gameType !in availableGames) {
                player.message(
                    "aclan.score.invalid_game_type", 
                    mapOf("available_games" to availableGames.joinToString(", "))
                )
                return
            }
            
            // Проверяем, что количество очков - число
            val amount = try {
                amountStr.toInt()
            } catch (e: NumberFormatException) {
                player.message("aclan.score.invalid_amount")
                return
            }
            
            // Проверяем, что количество очков положительное
            if (amount <= 0) {
                player.message("aclan.score.amount_must_be_positive")
                return
            }
            
            // Получаем игрока
            val target = Bukkit.getOfflinePlayer(targetName)
            if (target == null || !target.hasPlayedBefore()) {
                player.message("general.player_not_found", mapOf("player" to targetName))
                return
            }
            
            // Выполняем действие с очками
            when (action) {
                "add" -> {
                    ClanManager.Scores.addScore(target.uniqueId, targetName, gameType, amount)
                    player.message(
                        "aclan.score.added", 
                        mapOf(
                            "amount" to amount.toString(),
                            "game_type" to gameType,
                            "player" to targetName
                        )
                    )
                }
                "remove" -> {
                    ClanManager.Scores.removeScore(target.uniqueId, targetName, gameType, amount)
                    player.message(
                        "aclan.score.removed", 
                        mapOf(
                            "amount" to amount.toString(),
                            "game_type" to gameType,
                            "player" to targetName
                        )
                    )
                }
                "set" -> {
                    ClanManager.Scores.setScore(target.uniqueId, targetName, gameType, amount)
                    player.message(
                        "aclan.score.set", 
                        mapOf(
                            "amount" to amount.toString(),
                            "game_type" to gameType,
                            "player" to targetName
                        )
                    )
                }
            }
            
            // Выводим текущее количество очков
            val currentScore = ClanManager.Scores.getPlayerScore(target.uniqueId, gameType)
            player.message(
                "aclan.score.current", 
                mapOf(
                    "player" to targetName,
                    "game_type" to gameType,
                    "score" to currentScore.toString()
                )
            )
            
            // Уведомляем игрока, если он онлайн
            Bukkit.getPlayer(target.uniqueId)?.message(
                "aclan.score.notify", 
                mapOf(
                    "action" to when(action) {
                        "add" -> "добавил"
                        "remove" -> "убавил"
                        "set" -> "установил"
                        else -> "изменил"
                    },
                    "admin" to player.name,
                    "amount" to amount.toString(),
                    "game_type" to gameType,
                    "current" to currentScore.toString()
                )
            )
        }
    }
}