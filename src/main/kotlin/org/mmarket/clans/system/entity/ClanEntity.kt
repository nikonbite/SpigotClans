package org.mmarket.clans.system.entity

import org.ktorm.entity.Entity
import org.mmarket.clans.system.model.ClanMemberModel
import java.time.LocalDateTime
import java.util.UUID

interface ClanEntity : Entity<ClanEntity> {
    companion object : Entity.Factory<ClanEntity>()
    
    var id: UUID
    var name: String
    var colorlessName: String
    var treasury: Long
    var news: List<String>
    var motd: String
    var creator: UUID
    var owner: UUID
    var createdAt: LocalDateTime
    
    // Не хранится в базе данных, загружается отдельно
    var members: MutableSet<ClanMemberModel>
} 