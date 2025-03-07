package org.mmarket.clans.system.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.mmarket.clans.system.model.ClanMemberRole

object ClanMembersTable : Table("clan_members") {
    val id = integer("id").autoIncrement()
    val clanId = reference("clan_id", ClansTable.id, onDelete = ReferenceOption.CASCADE)
    val uuid = uuid("uuid").uniqueIndex()
    val role = enumeration("role", ClanMemberRole::class)
    val joinedAt = datetime("joined_at")

    override val primaryKey = PrimaryKey(id)
}