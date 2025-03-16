package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.system.util.binaryUuid
import java.time.LocalDateTime
import java.util.UUID

object ClanMembersTable : Table<Nothing>("clan_members") {
    val id = int("id").primaryKey()
    val clanId = binaryUuid("clan_id")
    val uuid = binaryUuid("uuid")
    val name = text("name")
    val role = enum<ClanMemberRole>("role")
    val joinedAt = datetime("joined_at")
}