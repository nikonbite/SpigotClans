package org.mmarket.clans.system.table

import org.ktorm.schema.*
import org.mmarket.clans.system.util.binaryUuid
import org.mmarket.clans.system.util.mapColumn
import java.util.UUID

object ClanScoresTable : Table<Nothing>("clan_scores") {
    val id = int("id").primaryKey()
    val playerUuid = binaryUuid("player_uuid")
    val playerName = text("player_name")
    val scores = mapColumn("scores")
}