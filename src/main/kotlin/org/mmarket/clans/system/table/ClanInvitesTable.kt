package org.mmarket.clans.system.table

import org.ktorm.schema.*
import java.time.LocalDateTime
import java.util.UUID

object ClanInvitesTable : Table<Nothing>("clan_invites") {
    val id = int("id").primaryKey()
    val clanId = uuid("clan_id")
    val uuid = uuid("uuid")
    val createdAt = datetime("created_at")
}