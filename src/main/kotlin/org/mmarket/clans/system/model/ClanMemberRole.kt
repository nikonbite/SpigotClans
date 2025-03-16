package org.mmarket.clans.system.model

enum class ClanMemberRole(val role: String, val priority: Long) {
    RECRUIT("Рекрут", 0),
    SENIOR("Старшина", 1),
    COMMODORE("Коммодор", 2),
    ADMIRAL("Адмирал", 3);

    fun next(): ClanMemberRole {
        return when (this) {
            RECRUIT -> SENIOR
            SENIOR -> COMMODORE
            COMMODORE -> ADMIRAL
            ADMIRAL -> ADMIRAL
        }
    }

    fun previous(): ClanMemberRole {
        return when (this) {
            RECRUIT -> RECRUIT
            SENIOR -> RECRUIT
            COMMODORE -> SENIOR
            ADMIRAL -> COMMODORE
        }
    }

    companion object {
        fun get(value: String): ClanMemberRole {
            return try {
                ClanMemberRole.valueOf(value)
            } catch (ex: Exception) {
                RECRUIT
            }
        }
    }
}