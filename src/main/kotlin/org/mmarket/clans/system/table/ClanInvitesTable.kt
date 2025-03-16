package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.util.binaryUuid
import java.time.LocalDateTime
import java.util.UUID

object ClanInvitesTable : Table<Nothing>("clan_invites") {
    val id = int("id").primaryKey()
    val clanId = binaryUuid("clan_id")
    val uuid = binaryUuid("uuid")
    val createdAt = datetime("created_at")
}