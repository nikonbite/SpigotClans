package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.mmarket.clans.api.interfaces.Ui
import org.mmarket.clans.files.Interfaces
import org.mmarket.clans.system.manager.ClanManager
import java.util.UUID

/**
 * UI для магазина клана
 * Позволяет игрокам покупать предметы и улучшения для клана
 * Золотой слиток - 4 слот (Клановая казна, просто отображает количество валюты в наличии клана)
 * Алмаз - 10 слот (Купить рекламу)
 * Огненный порошок - 16 слот (Клановое приветствие)
 * Яблоко - 18 слот (Клановый чат)
 * Мембрана фантома - 20 слот (Пати-призыв)
 * Сердце моря - 22 слот (Места для клана [1 уровень])
 * Кварц - 24 слот (Повышение соклановцев)
 * Джунглевая табличка - 26 слот (Переименовать клан)
 */
class ClanShopUi(private val player: Player, private val clanId: UUID? = null) : Ui {
    private val clan = clanId?.let { ClanManager.get(it) }
    
    val gui: Gui = Gui.gui()
        .title(Interfaces.string("clan_shop.title").component())
        .rows(3)
        .create()

    override fun open() {
        setupItems()
        
        gui.open(player)
    }
    
    private fun setupItems() {
        // Золотой слиток - 4 слот (Клановая казна)
        gui.setItem(
            4,
            ItemBuilder.from(Material.GOLD_INGOT)
                .name(Interfaces.string("clan_shop.treasury_name").component())
                .lore(
                    Interfaces.string("clan_shop.treasury_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.treasury_click").component())
                    }
                }
        )
        
        // Алмаз - 10 слот (Купить рекламу)
        gui.setItem(
            10,
            ItemBuilder.from(Material.DIAMOND)
                .name(Interfaces.string("clan_shop.ads_name").component())
                .lore(
                    Interfaces.string("clan_shop.ads_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.ads_click").component())
                    }
                }
        )
        
        // Огненный порошок - 16 слот (Клановое приветствие)
        gui.setItem(
            16,
            ItemBuilder.from(Material.BLAZE_POWDER)
                .name(Interfaces.string("clan_shop.motd_name").component())
                .lore(
                    Interfaces.string("clan_shop.motd_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.motd_click").component())
                    }
                }
        )
        
        // Яблоко - 18 слот (Клановый чат)
        gui.setItem(
            18,
            ItemBuilder.from(Material.APPLE)
                .name(Interfaces.string("clan_shop.chat_name").component())
                .lore(
                    Interfaces.string("clan_shop.chat_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.chat_click").component())
                    }
                }
        )
        
        // Мембрана фантома - 20 слот (Пати-призыв)
        gui.setItem(
            20,
            ItemBuilder.from(Material.PHANTOM_MEMBRANE)
                .name(Interfaces.string("clan_shop.party_name").component())
                .lore(
                    Interfaces.string("clan_shop.party_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.party_click").component())
                    }
                }
        )
        
        // Сердце моря - 22 слот (Места для клана [1 уровень])
        gui.setItem(
            22,
            ItemBuilder.from(Material.HEART_OF_THE_SEA)
                .name(Interfaces.string("clan_shop.slots_name").component())
                .lore(
                    Interfaces.string("clan_shop.slots_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.slots_click").component())
                    }
                }
        )
        
        // Кварц - 24 слот (Повышение соклановцев)
        gui.setItem(
            24,
            ItemBuilder.from(Material.QUARTZ)
                .name(Interfaces.string("clan_shop.upgrade_name").component())
                .lore(
                    Interfaces.string("clan_shop.upgrade_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.upgrade_click").component())
                    }
                }
        )
        
        // Джунглевая табличка - 26 слот (Переименовать клан)
        gui.setItem(
            26,
            ItemBuilder.from(Material.JUNGLE_SIGN)
                .name(Interfaces.string("clan_shop.rename_name").component())
                .lore(
                    Interfaces.string("clan_shop.rename_lore").component()
                )
                .asGuiItem { event ->
                    event.isCancelled = true

                    if (event.click == ClickType.LEFT) {
                        // Заглушка для клика
                        player.sendMessage(Interfaces.string("clan_shop.rename_click").component())
                    }
                }
        )
    }

    fun String.component() = LegacyComponentSerializer.legacySection().deserialize(this)
}
