package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.model.ClanMemberRole
import java.time.LocalDateTime
import java.util.UUID

object ClanMembersTable : Table<Nothing>("clan_members") {
    val id = int("id").primaryKey()
    val clanId = uuid("clan_id")
    val uuid = uuid("uuid")
    val name = text("name")
    val role = enum<ClanMemberRole>("role")
    val joinedAt = datetime("joined_at")
}