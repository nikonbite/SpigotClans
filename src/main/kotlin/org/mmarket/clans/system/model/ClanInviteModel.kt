package org.mmarket.clans.system.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Модель приглашения в клан
 *
 * @property clanId ID клана
 * @property playerUuid UUID игрока
 * @property createdAt Дата создания приглашения
 */
data class ClanInviteModel(
    val clanId: UUID,
    val playerUuid: UUID,
    val playerName: String,
    val createdAt: LocalDateTime
) 