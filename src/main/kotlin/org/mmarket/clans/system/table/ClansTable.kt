package org.mmarket.clans.system.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ClansTable : Table("clans") {
    val id = uuid("id").uniqueIndex()
    val name = varchar("name", length = 256)
    val colorlessName = varchar("colorless_name", length = 12)
    val treasury = long("age").default(0)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}