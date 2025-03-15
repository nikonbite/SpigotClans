package org.mmarket.clans.system.entity

import org.ktorm.entity.Entity
import org.mmarket.clans.system.model.ClanMemberRole
import java.time.LocalDateTime
import java.util.UUID

interface ClanMemberEntity : Entity<ClanMemberEntity> {
    companion object : Entity.Factory<ClanMemberEntity>()
    
    var id: Int
    var clanId: UUID
    var uuid: UUID
    var role: ClanMemberRole
    var joinedAt: LocalDateTime
} 