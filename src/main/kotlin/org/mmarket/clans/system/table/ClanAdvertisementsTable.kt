package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.util.binaryUuid
import java.time.LocalDateTime
import java.util.UUID

object ClanAdvertisementsTable : Table<Nothing>("clan_advertisements") {
    val id = int("id").primaryKey()
    val clanId = binaryUuid("clan_id")
    val joinType = text("join_type")
    val tariff = text("tariff")
    val lines = text("lines")
    val createdAt = datetime("created_at")
    val expiresAt = datetime("expires_at")
}