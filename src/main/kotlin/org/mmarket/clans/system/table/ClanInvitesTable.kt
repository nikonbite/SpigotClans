package org.mmarket.clans.system.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ClanInvitesTable : Table() {
    val id = integer("id").autoIncrement()
    val clanId = reference("clan_id", ClansTable.id)
    val userId = integer("user_id")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}