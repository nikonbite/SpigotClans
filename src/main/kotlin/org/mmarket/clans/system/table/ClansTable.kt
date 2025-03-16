package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.util.binaryUuid
import org.mmarket.clans.system.model.ClanSlots

object ClansTable : Table<Nothing>("clans") {
    val id = binaryUuid("id").primaryKey()
    val name = varchar("name")
    val colorlessName = varchar("colorless_name")
    val treasury = long("treasury")
    val score = long("score")
    val news = text("news")
    val motd = varchar("motd")
    val creator = binaryUuid("creator")
    val owner = binaryUuid("owner")
    val createdAt = datetime("created_at")
    val slots = enum<ClanSlots>("slots")
    val chatPurchased = boolean("chat_purchased")
    val motdPurchased = boolean("motd_purchased")
    val partyPurchased = boolean("party_purchased")
}