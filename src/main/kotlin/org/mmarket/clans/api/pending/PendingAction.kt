package org.mmarket.clans.api.pending

import org.bukkit.entity.Player

data class PendingAction(
    val player: Player,
    val action: () -> Unit,
    val expirationTime: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expirationTime

    companion object {
        fun create(player: Player, action: () -> Unit, delaySeconds: Long): PendingAction {
            return PendingAction(
                player,
                action,
                System.currentTimeMillis() + delaySeconds * 1000
            )
        }
    }
}