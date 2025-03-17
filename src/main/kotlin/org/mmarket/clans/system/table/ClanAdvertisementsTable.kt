package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.util.binaryUuid
import org.mmarket.clans.system.model.ClanAdvertisementJoinType
import org.mmarket.clans.system.model.ClanAdvertisementTariff
import java.time.LocalDateTime
import java.util.UUID

object ClanAdvertisementsTable : Table<Nothing>("clan_advertisements") {
    val id = int("id").primaryKey()
    val clanId = binaryUuid("clan_id")
    val joinType = enum<ClanAdvertisementJoinType>("join_type")
    val tariff = enum<ClanAdvertisementTariff>("tariff")
    val lines = text("ad_lines")
    val createdAt = datetime("created_at")
    val expiresAt = datetime("expires_at")
}