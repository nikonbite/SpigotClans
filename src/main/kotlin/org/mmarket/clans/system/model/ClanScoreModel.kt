package org.mmarket.clans.system.model

import java.util.UUID

data class ClanScoreModel(
    val playerUuid: UUID,
    val playerName: String,
    val scores: Map<String, Int>
)