package org.mmarket.clans.system.model

import org.mmarket.clans.files.Settings

enum class ClanSlots(val plus: Int, val cost: Double) {
    INITIAL(Settings.long("slots.initial.plus").toInt(), Settings.double("slots.initial.cost")),
    FIRST(Settings.long("slots.first.plus").toInt(), Settings.double("slots.first.cost")),
    SECOND(Settings.long("slots.second.plus").toInt(), Settings.double("slots.second.cost")),
    THIRD(Settings.long("slots.third.plus").toInt(), Settings.double("slots.third.cost")),
    FOURTH(Settings.long("slots.fourth.plus").toInt(), Settings.double("slots.fourth.cost")),
    FIFTH(Settings.long("slots.fifth.plus").toInt(), Settings.double("slots.fifth.cost")),
    SIXTH(Settings.long("slots.sixth.plus").toInt(), Settings.double("slots.sixth.cost"));

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