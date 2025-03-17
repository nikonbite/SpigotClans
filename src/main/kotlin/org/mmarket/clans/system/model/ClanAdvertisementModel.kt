package org.mmarket.clans.system.model

import org.mmarket.clans.system.table.ClanAdvertisementsTable
import java.util.UUID
import java.time.LocalDateTime

data class ClanAdvertisementModel(
    val clanId: UUID,
    var joinType: ClanAdvertisementJoinType,
    var tariff: ClanAdvertisementTariff,
    var lines: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime
)