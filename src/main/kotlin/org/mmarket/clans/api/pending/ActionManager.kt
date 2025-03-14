package org.mmarket.clans.api.pending

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.mmarket.clans.files.Messages.message
import org.mmarket.clans.files.Settings
import java.util.UUID

class ActionManager(private val plugin: JavaPlugin) {
    private val pendingActions = mutableMapOf<UUID, PendingAction>()

    fun addAction(player: Player, action: () -> Unit) {
        val playerId = player.uniqueId
        cleanupExpired()

        val delaySeconds = Settings.long("general.action_expire")
        val pendingAction = PendingAction.create(player, action, delaySeconds)
        pendingActions[playerId] = pendingAction

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            pendingActions.remove(playerId)?.let {
                if (player.isOnline)
                    player.message("pending.timeout")
            }
        }, delaySeconds * 20L)
    }

    fun confirmAction(player: Player) {
        val action = pendingActions.remove(player.uniqueId)
        when {
            action == null    ->  player.message("pending.no_tasks")
            action.isExpired  ->  player.message("pending.timeout")
            else              ->  action.action()
        }
    }

    fun cancelAction(player: Player): Boolean {
        val action = pendingActions.remove(player.uniqueId)
        return if (action != null) {
            player.message("pending.cancelled")
            true
        } else {
            player.message("pending.no_tasks")
            false
        }
    }

    private fun cleanupExpired() {
        pendingActions.values.removeIf { it.isExpired }
    }
}