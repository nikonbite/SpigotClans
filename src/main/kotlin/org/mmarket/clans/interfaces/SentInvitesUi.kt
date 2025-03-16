package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.mmarket.clans.api.interfaces.Ui
import org.mmarket.clans.files.Interfaces
import org.mmarket.clans.system.manager.ClanManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * UI для отображения отправленных приглашений в клан
 * Доступно только для коммодоров и адмиралов клана
 */
class SentInvitesUi(private val player: Player, private val clanId: UUID) : Ui {    
    val invites = ClanManager.Invites.getClanInviteModels(clanId)
        .filter { ChronoUnit.DAYS.between(it.createdAt, LocalDateTime.now()) < 7 } // Фильтруем приглашения старше недели
        .sortedByDescending { it.createdAt }
        .toMutableList()
    
    val gui: Gui = Gui.gui()
        .title(Interfaces.string("sent_invites.title").component())
        .rows(if (invites.size > 9) 2 else 1)
        .create()

    override fun open() {
        if (invites.isEmpty()) {
            gui.setItem(
                4,
                ItemBuilder.from(Material.valueOf(Interfaces.string("common.no_invites_item")))
                    .name(Interfaces.string("sent_invites.no_invites_text").component())
                    .asGuiItem()
            )
            gui.open(player)
            return
        }

        // Ограничиваем количество приглашений до 18
        if (invites.size > 18) {
            invites.subList(18, invites.size).clear()
        }

        for (i in 0 until invites.size) {
            val invite = invites[i]
            val playerName = Bukkit.getOfflinePlayer(invite.playerUuid).name ?: invite.playerUuid.toString()
            
            gui.setItem(
                i,
                ItemBuilder.from(Material.valueOf(Interfaces.string("sent_invites.invite_item")))
                    .name(Interfaces.string("sent_invites.invite_name", 
                        "player_name" to playerName).component())
                    .lore(
                        Interfaces.string("sent_invites.invite_lore_cancel").component()
                    )
                    .asGuiItem { event ->
                        if (event.click == ClickType.LEFT) {
                            ClanManager.Invites.removeInvite(clanId, invite.playerUuid)
                            player.closeInventory()
                            player.sendMessage(Interfaces.string("sent_invites.invite_cancelled_message", 
                                "player_name" to playerName).component())
                            open() 
                        }
                    }
            )
        }
        
        gui.open(player)
    }

    fun String.component() = LegacyComponentSerializer.legacySection().deserialize(this)
} 