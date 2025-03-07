package org.mmarket.clans.system.model

import org.mmarket.clans.api.utility.FixedSizeList
import java.time.LocalDateTime
import java.util.UUID

data class ClanModel(
    val id: UUID,
    val name: String,
    val colorlessName: String,
    val treasury: Long,
    var news: FixedSizeList<String>,
    var motd: String,
    val creator: UUID,
    var owner: UUID,
    var members: MutableSet<ClanMemberModel>,
    val createdAt: LocalDateTime,
) {}
