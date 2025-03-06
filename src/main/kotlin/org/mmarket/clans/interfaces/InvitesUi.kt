package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import org.bukkit.Material
import org.mmarket.clans.api.interfaces.Ui


class InvitesUi : Ui {
    val inviteItem = ItemBuilder.from(Material.STONE).asGuiItem();
}