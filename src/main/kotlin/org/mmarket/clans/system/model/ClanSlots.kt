package org.mmarket.clans.system.model

import org.mmarket.clans.files.Settings

enum class ClanSlots(val plus: Int, val cost: Long) {
    INITIAL(Settings.long("slots.initial.plus").toInt(), Settings.long("slots.initial.cost")),
    FIRST(Settings.long("slots.first.plus").toInt(), Settings.long("slots.first.cost")),
    SECOND(Settings.long("slots.second.plus").toInt(), Settings.long("slots.second.cost")),
    THIRD(Settings.long("slots.third.plus").toInt(), Settings.long("slots.third.cost")),
    FOURTH(Settings.long("slots.fourth.plus").toInt(), Settings.long("slots.fourth.cost")),
    FIFTH(Settings.long("slots.fifth.plus").toInt(), Settings.long("slots.fifth.cost")),
    SIXTH(Settings.long("slots.sixth.plus").toInt(), Settings.long("slots.sixth.cost"));

    fun next(): ClanSlots {
        return when (this) {
            INITIAL -> FIRST
            FIRST -> SECOND
            SECOND -> THIRD
            THIRD -> FOURTH
            FOURTH -> FIFTH
            FIFTH -> SIXTH
            SIXTH -> SIXTH
        }
    }

    fun calculateSlots(): Int {
        return when (this) {
            INITIAL -> INITIAL.plus
            FIRST -> INITIAL.plus + FIRST.plus
            SECOND -> INITIAL.plus + FIRST.plus + SECOND.plus
            THIRD -> INITIAL.plus + FIRST.plus + SECOND.plus + THIRD.plus
            FOURTH -> INITIAL.plus + FIRST.plus + SECOND.plus + THIRD.plus + FOURTH.plus
            FIFTH -> INITIAL.plus + FIRST.plus + SECOND.plus + THIRD.plus + FOURTH.plus + FIFTH.plus
            SIXTH -> INITIAL.plus + FIRST.plus + SECOND.plus + THIRD.plus + FOURTH.plus + FIFTH.plus + SIXTH.plus
        }
    }
    
}