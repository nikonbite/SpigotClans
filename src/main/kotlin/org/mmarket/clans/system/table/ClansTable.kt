package org.mmarket.clans.system.table

import org.ktorm.schema.*

object ClansTable : Table<Nothing>("clans") {
    val id = uuid("id").primaryKey()
    val name = varchar("name")
    val colorlessName = varchar("colorless_name")
    val treasury = long("treasury")
    val score = long("score")
    val news = text("news")
    val motd = varchar("motd")
    val creator = uuid("creator")
    val owner = uuid("owner")
    val createdAt = datetime("created_at")
}