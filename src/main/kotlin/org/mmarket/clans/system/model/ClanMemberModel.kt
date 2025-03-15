package org.mmarket.clans.system.model

import java.time.LocalDateTime
import java.util.UUID

data class ClanMemberModel(
    val uuid: UUID,
    val name: String,
    var role: ClanMemberRole,
    val joinedAt: LocalDateTime,
)
