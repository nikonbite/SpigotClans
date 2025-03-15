package org.mmarket.clans.system.entity

import org.ktorm.entity.Entity
import java.time.LocalDateTime
import java.util.UUID

interface ClanInviteEntity : Entity<ClanInviteEntity> {
    companion object : Entity.Factory<ClanInviteEntity>()
    
    var id: Int
    var clanId: UUID
    var uuid: UUID
    var createdAt: LocalDateTime
} 