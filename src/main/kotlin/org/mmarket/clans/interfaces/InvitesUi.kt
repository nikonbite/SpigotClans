package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import org.bukkit.Material
import org.bukkit.entity.Player
import org.mmarket.clans.api.interfaces.Ui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

class InvitesUi : Ui {
    val gui = Gui.gui().title("&bПриглашения в кланы".component())
        .rows(6)
        .create();

    override fun open(player: Player) {
        val playerInvitesItem = ItemBuilder.from(Material.PAPER).name("&bПриглашения в кланы".component()).asGuiItem()
    }

    fun String.component() = LegacyComponentSerializer.legacySection().deserialize(this)
}