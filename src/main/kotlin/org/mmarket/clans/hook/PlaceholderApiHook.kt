package org.mmarket.clans.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.mmarket.clans.Clans
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.system.manager.AdvertisementManager
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDateTime

/**
 * Хук для PlaceholderAPI, предоставляющий плейсхолдеры для информации о кланах.
 * 
 * Доступные плейсхолдеры:
 * 
 * Общие:
 * - %clans_has_clan% - состоит ли игрок в клане (true/false)
 * - %clans_invites_count% - количество приглашений в кланы для игрока
 * 
 * Основная информация о клане:
 * - %clans_name% - название клана
 * - %clans_colorless_name% - название клана без цветовых кодов
 * - %clans_id% - уникальный идентификатор клана
 * - %clans_treasury% - казна клана
 * - %clans_score% - базовый счет клана
 * - %clans_total_score% - общий счет клана (сумма очков всех участников)
 * - %clans_created_at% - дата создания клана
 * - %clans_top_position% - позиция клана в общем рейтинге
 * 
 * Информация о владельце:
 * - %clans_owner_name% - имя владельца клана
 * - %clans_owner_uuid% - UUID владельца клана
 * - %clans_creator_name% - имя создателя клана
 * - %clans_creator_uuid% - UUID создателя клана
 * 
 * Информация о членах:
 * - %clans_members_count% - количество участников клана
 * - %clans_max_members% - максимальное количество участников
 * - %clans_online_members% - количество онлайн участников
 * 
 * Информация о роли игрока:
 * - %clans_role% - роль игрока в клане
 * - %clans_role_priority% - приоритет роли игрока
 * - %clans_joined_at% - дата вступления игрока в клан
 * 
 * Информация о покупках:
 * - %clans_has_chat% - куплен ли клановый чат (true/false)
 * - %clans_has_motd% - куплен ли MOTD (true/false)
 * - %clans_has_party% - куплена ли функция пати (true/false)
 * - %clans_slots_level% - уровень слотов клана
 * 
 * Информация о MOTD:
 * - %clans_motd% - полный текст MOTD
 * - %clans_motd_line_1% - первая строка MOTD
 * - %clans_motd_line_2% - вторая строка MOTD
 * - %clans_motd_line_3% - третья строка MOTD
 * 
 * Информация о новостях:
 * - %clans_news_count% - количество новостей клана
 * - %clans_latest_news% - последняя новость клана
 * 
 * Информация о рекламе:
 * - %clans_has_advertisement% - есть ли активная реклама (true/false)
 * - %clans_advertisement_type% - тип рекламы (По заявке/Открытое)
 * - %clans_advertisement_tariff% - тариф рекламы
 * - %clans_advertisement_expires% - время до истечения рекламы (часы:минуты)
 * 
 * Информация об очках игрока:
 * - %clans_player_score_total% - общее количество очков игрока
 * - %clans_player_score_TYPE% - количество очков игрока определенного типа (замените TYPE на тип очков)
 * 
 * Позиция игрока в клане:
 * - %clans_player_clan_position% - позиция игрока в клане по общему количеству очков
 * - %clans_player_clan_position_TYPE% - позиция игрока в клане по определенному типу очков (замените TYPE на тип очков)
 */
class PlaceholderApiHook(val clans: Clans) : PlaceholderExpansion() {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    
    override fun getAuthor(): String {
        return "mMarket"
    }

    override fun getIdentifier(): String {
        return "clans"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun onRequest(player: OfflinePlayer?, placeholder: String): String? {
        if (player == null) return null
        
        // Получаем клан игрока
        val clan = ClanManager.Members.getClan(player.uniqueId)
        
        // Плейсхолдеры, связанные с наличием клана
        if (placeholder == "has_clan") {
            return if (clan != null) "true" else "false"
        }
        
        // Если игрок не в клане, возвращаем пустую строку для большинства плейсхолдеров
        if (clan == null) {
            return when {
                placeholder == "has_clan" -> "false"
                placeholder == "invites_count" -> ClanManager.Invites.getPlayerInviteModels(player.uniqueId).size.toString()
                placeholder == "player_score_total" -> {
                    val playerScores = ClanManager.Scores.getPlayerScores(player.uniqueId)
                    playerScores?.scores?.values?.sum()?.toString() ?: "0"
                }
                placeholder.startsWith("player_score_") -> {
                    val scoreType = placeholder.substring("player_score_".length)
                    val playerScores = ClanManager.Scores.getPlayerScores(player.uniqueId)
                    playerScores?.scores?.get(scoreType)?.toString() ?: "0"
                }
                else -> ""
            }
        }
        
        // Плейсхолдеры для информации о клане
        return when {
            // Основная информация о клане
            placeholder == "name" -> clan.name
            placeholder == "colorless_name" -> clan.colorlessName
            placeholder == "id" -> clan.id.toString()
            placeholder == "treasury" -> clan.treasury.toString()
            placeholder == "score" -> clan.score.toString()
            placeholder == "total_score" -> ClanManager.Scores.calculateClanScore(clan.id).toString()
            placeholder == "created_at" -> clan.createdAt.format(dateFormatter)
            
            // Информация о владельце
            placeholder == "owner_name" -> ClanManager.Members.member(clan.id, clan.owner)?.name ?: ""
            placeholder == "owner_uuid" -> clan.owner.toString()
            placeholder == "creator_name" -> ClanManager.Members.member(clan.id, clan.creator)?.name ?: ""
            placeholder == "creator_uuid" -> clan.creator.toString()
            
            // Информация о членах
            placeholder == "members_count" -> clan.members.size.toString()
            placeholder == "max_members" -> clan.slots.calculateSlots().toString()
            placeholder == "online_members" -> clan.members.count { 
                val offlinePlayer = clans.server.getOfflinePlayer(it.uuid)
                offlinePlayer.isOnline
            }.toString()
            
            // Информация о роли игрока
            placeholder == "role" -> ClanManager.Members.role(player.uniqueId)?.role ?: ""
            placeholder == "role_priority" -> ClanManager.Members.role(player.uniqueId)?.priority?.toString() ?: ""
            placeholder == "joined_at" -> ClanManager.Members.member(clan.id, player.uniqueId)?.joinedAt?.format(dateFormatter) ?: ""
            
            // Информация о покупках
            placeholder == "has_chat" -> clan.chatPurchased.toString()
            placeholder == "has_motd" -> clan.motdPurchased.toString()
            placeholder == "has_party" -> clan.partyPurchased.toString()
            placeholder == "slots_level" -> clan.slots.name
            
            // Информация о MOTD
            placeholder == "motd" -> clan.motd
            placeholder == "motd_line_1" -> clan.motd.split("\n").getOrElse(0) { "" }
            placeholder == "motd_line_2" -> clan.motd.split("\n").getOrElse(1) { "" }
            placeholder == "motd_line_3" -> clan.motd.split("\n").getOrElse(2) { "" }
            
            // Информация о новостях
            placeholder == "news_count" -> clan.news.toList().size.toString()
            placeholder == "latest_news" -> clan.news.toList().firstOrNull() ?: ""
            
            // Информация о рейтинге
            placeholder == "top_position" -> {
                val clans = ClanManager.clans()
                val clansWithScores = clans
                    .map { c -> Pair(c, ClanManager.Scores.calculateClanScore(c.id)) }
                    .sortedByDescending { it.second }
                
                (clansWithScores.indexOfFirst { it.first.id == clan.id } + 1).toString()
            }
            
            // Информация о рекламе
            placeholder == "has_advertisement" -> AdvertisementManager.hasActiveAdvertisement(clan.id).toString()
            placeholder == "advertisement_type" -> AdvertisementManager.getActiveAdvertisement(clan.id)?.joinType?.displayName ?: ""
            placeholder == "advertisement_tariff" -> AdvertisementManager.getActiveAdvertisement(clan.id)?.tariff?.name ?: ""
            placeholder == "advertisement_expires" -> {
                val ad = AdvertisementManager.getActiveAdvertisement(clan.id)
                if (ad != null) {
                    val now = LocalDateTime.now()
                    val hours = ChronoUnit.HOURS.between(now, ad.expiresAt)
                    val minutes = ChronoUnit.MINUTES.between(now, ad.expiresAt) % 60
                    "$hours:${minutes.toString().padStart(2, '0')}"
                } else {
                    ""
                }
            }
            
            // Информация о приглашениях
            placeholder == "invites_count" -> ClanManager.Invites.getClanInviteModels(clan.id).size.toString()
            
            // Информация об очках игрока
            placeholder == "player_score_total" -> {
                val playerScores = ClanManager.Scores.getPlayerScores(player.uniqueId)
                playerScores?.scores?.values?.sum()?.toString() ?: "0"
            }
            
            placeholder.startsWith("player_score_") -> {
                val scoreType = placeholder.substring("player_score_".length)
                val playerScores = ClanManager.Scores.getPlayerScores(player.uniqueId)
                playerScores?.scores?.get(scoreType)?.toString() ?: "0"
            }
            
            // Позиция игрока в клане по очкам
            placeholder == "player_clan_position" -> {
                val memberScores = ClanManager.Scores.getClanMembersScores(clan.id)
                val sortedMembers = memberScores
                    .map { scoreModel -> 
                        Pair(scoreModel.playerUuid, scoreModel.scores.values.sum()) 
                    }
                    .sortedByDescending { it.second }
                
                (sortedMembers.indexOfFirst { it.first == player.uniqueId } + 1).toString()
            }
            
            placeholder.startsWith("player_clan_position_") -> {
                val scoreType = placeholder.substring("player_clan_position_".length)
                val memberScores = ClanManager.Scores.getClanMembersScores(clan.id)
                val sortedMembers = memberScores
                    .map { scoreModel -> 
                        Pair(scoreModel.playerUuid, scoreModel.scores[scoreType] ?: 0) 
                    }
                    .sortedByDescending { it.second }
                
                (sortedMembers.indexOfFirst { it.first == player.uniqueId } + 1).toString()
            }
            
            // Если плейсхолдер не найден
            else -> null
        }
    }
}