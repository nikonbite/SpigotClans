package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.mmarket.clans.api.interfaces.Ui
import org.mmarket.clans.files.Interfaces
import org.mmarket.clans.system.manager.ClanManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * UI для отображения приглашений в кланы для игрока
 */
class InvitesUi(private val player: Player) : Ui {    
    val invites = ClanManager.Invites.getPlayerInviteModels(player.uniqueId)
        .filter { ChronoUnit.DAYS.between(it.createdAt, LocalDateTime.now()) < 7 } // Фильтруем приглашения старше недели
        .sortedByDescending { it.createdAt }
        .toMutableList()
    
    val gui: Gui = Gui.gui()
        .title(Interfaces.string("player_invites.title").component())
        .rows(if (invites.size > 9) 2 else 1)
        .create()

    override fun open() {
        if (invites.isEmpty()) {
            gui.setItem(
                4,
                ItemBuilder.from(Material.valueOf(Interfaces.string("common.no_invites_item")))
                    .name(Interfaces.string("player_invites.no_invites_text").component())
                    .asGuiItem { event ->
                        event.isCancelled = true
                    }
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
            val clan = ClanManager.get(invite.clanId)

            if (clan == null) continue

            gui.setItem(
                i,
                ItemBuilder.from(Material.valueOf(Interfaces.string("player_invites.invite_item")))
                    .name(Interfaces.string("player_invites.invite_name", 
                        "clan_name" to clan.name).component())
                    .lore(
                        Interfaces.string("player_invites.invite_lore_accept").component(),
                        Interfaces.string("player_invites.invite_lore_decline").component()
                    )
                    .asGuiItem { event ->
                        event.isCancelled = true

                        if (event.click == ClickType.LEFT) {
                            ClanManager.Invites.acceptInvite(invite.clanId, player.uniqueId)
                            player.closeInventory()
                            player.sendMessage(Interfaces.string("player_invites.invite_accepted_message", 
                                "clan_name" to clan.name).component())
                        } else if (event.click == ClickType.RIGHT) {
                            ClanManager.Invites.declineInvite(invite.clanId, player.uniqueId)
                            player.closeInventory()
                            player.sendMessage(Interfaces.string("player_invites.invite_declined_message", 
                                "clan_name" to clan.name).component())
                        }
                    }
            )
        }
        
        gui.open(player)
    }

    fun String.component() = LegacyComponentSerializer.legacySection().deserialize(this).decoration(TextDecoration.ITALIC, false)
}
