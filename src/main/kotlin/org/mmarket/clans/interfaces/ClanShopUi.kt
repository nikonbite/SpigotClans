package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import java.util.UUID
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.mmarket.clans.api.interfaces.Ui
import org.mmarket.clans.files.Interfaces
import org.mmarket.clans.files.Messages
import org.mmarket.clans.files.Settings
import org.mmarket.clans.hook.VaultHook
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.system.model.ClanSlots
import org.mmarket.clans.files.Messages.message

/**
 * UI для магазина клана. Позволяет игрокам покупать предметы и улучшения для клана
 */
class ClanShopUi(private val player: Player, private val clanId: UUID? = null) : Ui {
    private val clan = clanId ?: ClanManager.Members.getClan(player.uniqueId)?.id

    val gui: Gui = Gui.gui().title(Interfaces.string("clan_shop.title").component()).rows(3).create()

    override fun open() {
        if (clan == null) {
            player.sendMessage(Messages.get("general.not_in_clan"))
            return
        }

        setupItems()
        gui.open(player)
    }

    private fun setupItems() {
        val clanModel = ClanManager.get(clan!!) ?: return
        val purchasedLabel = Messages.get("general.purchased_label")
        
        // Золотой слиток - 4 слот (Клановая казна)
        gui.setItem(
                4,
                ItemBuilder.from(Material.GOLD_INGOT)
                .name(Interfaces.string("clan_shop.treasury.name", "balance" to clanModel.treasury.toString()).component())
                .lore(splitLore(Interfaces.string("clan_shop.treasury.lore")).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                        }
        )

        // Алмаз - 10 слот (Купить рекламу)
        gui.setItem(
                10,
                ItemBuilder.from(Material.DIAMOND)
                .name(Interfaces.string("clan_shop.ads.name").component())
                .lore(splitLore(Interfaces.string("clan_shop.ads.lore")).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                    handleAdsClick()
                        }
        )

        // Огненный порошок - 16 слот (Клановое приветствие)
        val motdLore = if (clanModel.motdPurchased) {
            Interfaces.string("clan_shop.motd.lore") + "\n" + purchasedLabel
        } else {
            Interfaces.string("clan_shop.motd.lore")
        }
        
        gui.setItem(
                16,
                ItemBuilder.from(Material.BLAZE_POWDER)
                .name(Interfaces.string("clan_shop.motd.name", "cost" to Settings.double("pricing.clan_motd").toString()).component())
                .lore(splitLore(motdLore).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                    handleMotdClick()
                        }
        )

        // Яблоко - 18 слот (Клановый чат)
        val chatLore = if (clanModel.chatPurchased) {
            Interfaces.string("clan_shop.chat.lore") + "\n" + purchasedLabel
        } else {
            Interfaces.string("clan_shop.chat.lore")
        }
        
        gui.setItem(
                18,
                ItemBuilder.from(Material.APPLE)
                .name(Interfaces.string("clan_shop.chat.name", "cost" to Settings.double("pricing.clan_chat").toString()).component())
                .lore(splitLore(chatLore).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                    handleChatClick()
                        }
        )

        // Мембрана фантома - 20 слот (Пати-призыв)
        val partyLore = if (clanModel.partyPurchased) {
            Interfaces.string("clan_shop.party.lore") + "\n" + purchasedLabel
        } else {
            Interfaces.string("clan_shop.party.lore")
        }
        
        gui.setItem(
                20,
                ItemBuilder.from(Material.PHANTOM_MEMBRANE)
                .name(Interfaces.string("clan_shop.party.name", "cost" to Settings.double("pricing.party").toString()).component())
                .lore(splitLore(partyLore).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                    handlePartyClick()
                        }
        )

        // Сердце моря - 22 слот (Места для клана [1 уровень])
        val nextSlot = clanModel.slots.next()
        
        // Определяем уровень слотов
        val slotLevel = when (clanModel.slots) {
            ClanSlots.INITIAL -> 0
            ClanSlots.FIRST -> 1
            ClanSlots.SECOND -> 2
            ClanSlots.THIRD -> 3
            ClanSlots.FOURTH -> 4
            ClanSlots.FIFTH -> 5
            ClanSlots.SIXTH -> 6
        }
        
        // Рассчитываем текущее и максимальное количество слотов
        val currentSlots = clanModel.slots.calculateSlots()
        val maxSlots = ClanSlots.SIXTH.calculateSlots()
        
        // Формируем лор с учетом метки "КУПЛЕНО" для максимального уровня
        val slotsLore = Interfaces.string("clan_shop.slots.lore", mapOf(
            "current" to currentSlots.toString(),
            "max" to maxSlots.toString(),
            "upgrade" to nextSlot.plus.toString(),
            "cost" to nextSlot.cost.toString()
        ))
        
        // Добавляем метку "КУПЛЕНО" для максимального уровня
        val finalSlotsLore = if (clanModel.slots == ClanSlots.SIXTH) {
            slotsLore + "\n" + purchasedLabel
        } else {
            slotsLore
        }
        
        gui.setItem(
                22,
                ItemBuilder.from(Material.HEART_OF_THE_SEA)
                .name(Interfaces.string("clan_shop.slots.name", mapOf(
                    "level" to slotLevel.toString()
                )).component())
                .lore(splitLore(finalSlotsLore).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                    handleSlotsClick()
                        }
        )

        // Кварц - 24 слот (Повышение соклановцев)
        gui.setItem(
                24,
                ItemBuilder.from(Material.QUARTZ)
                .name(Interfaces.string("clan_shop.upgrade.name", "cost" to Settings.double("pricing.promote").toString()).component())
                .lore(splitLore(Interfaces.string("clan_shop.upgrade.lore")).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                        }
        )

        // Джунглевая табличка - 26 слот (Переименовать клан)
        gui.setItem(
                26,
                ItemBuilder.from(Material.JUNGLE_SIGN)
                .name(Interfaces.string("clan_shop.rename.name", "cost" to Settings.double("pricing.rename").toString()).component())
                .lore(splitLore(Interfaces.string("clan_shop.rename.lore")).map { it.component() })
                        .asGuiItem { event ->
                            event.isCancelled = true
                }
        )
    }

    /**
     * Разделяет многострочный лор на список строк
     * @param lore Многострочный лор
     * @return Список строк лора
     */
    private fun splitLore(lore: String): List<String> {
        return lore.split("\n")
    }

    /**
     * Обработчик клика по рекламе клана
     */
    private fun handleAdsClick() {
        
    }

    /**
     * Обработчик клика по MOTD клана
     * Покупает возможность установки MOTD для клана
     */
    private fun handleMotdClick() {
        val clanModel = ClanManager.get(clan!!) ?: return
        
        if (clanModel.motdPurchased) {
            player.message("general.function_already_purchased")
            return
        }
        
        val motdCost = Settings.double("pricing.clan_motd")
        
        if (clanModel.treasury < motdCost) {
            player.message("general.not_enough_money_clan", mapOf("amount" to motdCost.toString()))
            return
        }
        
        // Списываем средства из казны клана
        ClanManager.subtractTreasury(clan, motdCost.toLong())
        
        // Активируем MOTD для клана
        clanModel.motdPurchased = true
        ClanManager.update(clanModel)

        player.message("clan.motd.purchased", mapOf("cost" to motdCost.toString()))
        
        // Добавляем новость в клан
        clanModel.news.add("${player.name} купил MOTD для клана за $motdCost СтарКоинов")
        ClanManager.update(clanModel)
        
        // Обновляем интерфейс
        setupItems()
        gui.update()
    }

    /**
     * Обработчик клика по чату клана
     * Покупает возможность использования чата клана
     */
    private fun handleChatClick() {
        val clanModel = ClanManager.get(clan!!) ?: return
        
        if (clanModel.chatPurchased) {
            player.message("general.function_already_purchased")
            return
        }
        
        val chatCost = Settings.double("pricing.clan_chat")
        
        if (clanModel.treasury < chatCost) {
            player.message("general.not_enough_money_clan", mapOf("amount" to chatCost.toString()))
            return
        }
        
        // Списываем средства из казны клана
        ClanManager.subtractTreasury(clan, chatCost.toLong())
        
        // Активируем чат для клана
        clanModel.chatPurchased = true
        ClanManager.update(clanModel)

        player.message("clan.chat.purchased", mapOf("cost" to chatCost.toString()))
        
        // Добавляем новость в клан
        clanModel.news.add("${player.name} купил клановый чат за $chatCost СтарКоинов")
        ClanManager.update(clanModel)
        
        // Обновляем интерфейс
        setupItems()
        gui.update()
    }

    /**
     * Обработчик клика по пати-призыву
     * Покупает возможность использования пати-призыва
     */
    private fun handlePartyClick() {
        val clanModel = ClanManager.get(clan!!) ?: return
        
        if (clanModel.partyPurchased) {
            player.message("general.function_already_purchased")
            return
        }
        
        val partyCost = Settings.double("pricing.party")
        
        if (clanModel.treasury < partyCost) {
            player.message("general.not_enough_money_clan", mapOf("amount" to partyCost.toString()))
            return
        }
        
        // Списываем средства из казны клана
        ClanManager.subtractTreasury(clan, partyCost.toLong())
        
        // Активируем пати-призыв для клана
        clanModel.partyPurchased = true
        ClanManager.update(clanModel)

        player.message("clan.party.purchased", mapOf("cost" to partyCost.toString()))

        // Добавляем новость в клан
        clanModel.news.add("${player.name} купил пати-призыв для клана за $partyCost СтарКоинов")
        ClanManager.update(clanModel)
        
        // Обновляем интерфейс
        setupItems()
        gui.update()
    }

    /**
     * Обработчик клика по слотам клана
     * Покупает дополнительные слоты для клана
     */
    private fun handleSlotsClick() {
        val clanModel = ClanManager.get(clan!!) ?: return
        val nextSlot = clanModel.slots.next()
        
        if (clanModel.slots == nextSlot) {
            player.message("clan.slots.max")
            return
        }
        
        val slotsCost = nextSlot.cost
        val slotsPlus = nextSlot.plus
        
        if (clanModel.treasury < slotsCost) {
            player.message("general.not_enough_money_clan", mapOf("amount" to slotsCost.toString()))
            return
        }
        
        // Списываем средства из казны клана
        ClanManager.subtractTreasury(clan, slotsCost.toLong())
        
        // Обновляем слоты клана
        val updatedClanModel = ClanManager.get(clan)?.apply {
            slots = nextSlot
        }
        
        if (updatedClanModel != null) {
            ClanManager.update(updatedClanModel)
            
            player.message("clan.slots.purchased", mapOf("slots" to slotsPlus.toString(), "cost" to slotsCost.toString()))

            // Добавляем новость в клан
            updatedClanModel.news.add("${player.name} купил +$slotsPlus слотов для клана за $slotsCost СтарКоинов")
            ClanManager.update(updatedClanModel)
            
            // Обновляем интерфейс
            setupItems()
            gui.update()
        }
    }

    fun String.component() = LegacyComponentSerializer.legacySection().deserialize(this)
}
