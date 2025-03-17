package org.mmarket.clans.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.files.Messages
import org.bukkit.plugin.Plugin

/**
 * Слушатель события входа игрока на сервер
 * Отображает MOTD клана при входе игрока
 */
class PlayerJoinListener : Listener {
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Проверяем, состоит ли игрок в клане
        val clan = ClanManager.Members.getClan(player.uniqueId) ?: return
        
        // Проверяем, приобретена ли функция MOTD
        if (!clan.motdPurchased) return
        
        // Проверяем, есть ли MOTD
        if (clan.motd.isBlank()) return
        
        // Отображаем MOTD с небольшой задержкой (чтобы игрок успел загрузиться)
        player.server.scheduler.runTaskLater(
            player.server.pluginManager.getPlugin("mMarketClans") ?: return,
            Runnable {
                // Показываем заголовок MOTD
                player.sendMessage(Messages.get("clan.motd.header", mapOf("clan" to clan.name)))
                
                // Выводим каждую строку MOTD
                clan.motd.split("\n").forEach { line ->
                    if (line.isNotBlank()) {
                        player.sendMessage(line)
                    }
                }
            },
            20L // Задержка в тиках (1 секунда)
        )
    }
} 