package org.mmarket.clans.system.model

import org.mmarket.clans.api.utility.FixedSizeList
import java.time.LocalDateTime
import java.util.UUID

data class ClanModel(
    val id: UUID,
    var name: String,
    var colorlessName: String,
    var treasury: Long,
    var score: Long,
    var news: FixedSizeList<String>,
    var motd: String,
    val creator: UUID,
    var owner: UUID,
    var members: MutableSet<ClanMemberModel>,
    val createdAt: LocalDateTime,
    var slots: ClanSlots,
    var chatPurchased: Boolean,
    var motdPurchased: Boolean,
    var partyPurchased: Boolean,
) {}
