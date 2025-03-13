package org.mmarket.clans.system.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ClanInvitesTable : Table("clan_invites") {
    val id = integer("id").autoIncrement()
    val clanId = reference("clan_id", ClansTable.id, onDelete = ReferenceOption.CASCADE)
    val uuid = uuid("uuid")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}