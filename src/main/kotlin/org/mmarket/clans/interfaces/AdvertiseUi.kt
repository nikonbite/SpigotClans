package org.mmarket.clans.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.mmarket.clans.api.interfaces.Ui
import org.mmarket.clans.api.interfaces.UiUtils
import org.mmarket.clans.api.interfaces.UiUtils.splitLoreToComponents
import org.mmarket.clans.api.interfaces.UiUtils.toComponent
import org.mmarket.clans.files.Interfaces
import org.mmarket.clans.files.Messages.message
import org.mmarket.clans.system.manager.AdvertisementManager
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.system.model.ClanAdvertisementJoinType
import org.mmarket.clans.system.model.ClanAdvertisementModel
import org.mmarket.clans.system.model.ClanAdvertisementTariff
import org.mmarket.clans.system.model.ClanMemberRole

/** UI для рекламы кланов */
class AdvertiseUi(private val player: Player) : Ui {
    // Получаем все активные рекламы
    private val advertisements = AdvertisementManager.getAllActiveAdvertisements()

    // Создаем пагинированный GUI с заголовком "Большой сундук"
    val gui =
            Gui.paginated()
                    .title(Interfaces.string("advertise.title").toComponent())
                    .rows(6)
                    .pageSize(36)
                    .create()

    override fun open() {
        setupItems()
        gui.open(player)
    }

    private fun setupItems() {
        advertisements.forEach { ad ->
            val clan = ClanManager.get(ad.clanId)
            if (clan != null) {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                val remainingHours = ChronoUnit.HOURS.between(LocalDateTime.now(), ad.expiresAt)
                val remainingMinutes =
                        ChronoUnit.MINUTES.between(LocalDateTime.now(), ad.expiresAt) % 60
                val remainTimeDisplay =
                        "$remainingHours:${remainingMinutes.toString().padStart(2, '0')}"

                val owner = ClanManager.Members.member(clan.id, clan.owner)?.name ?: "Unknown"
                val members = ClanManager.Members.members(clan.id)
                val membersSize = members.size
                val maxSlots = clan.slots.calculateSlots()
                val online = members.filter { getOfflinePlayer(it.uuid).isOnline }.size
                val createdAt = clan.createdAt.format(formatter)
                val clans = ClanManager.clans()
                val clansWithScores =
                        clans
                                .map { c -> Pair(c, ClanManager.Scores.calculateClanScore(c.id)) }
                                .sortedByDescending { it.second }

                val topPosition = clansWithScores.indexOfFirst { it.first.id == clan.id } + 1

                val adItem =
                        ItemBuilder.from(Material.PAPER)
                                .name(
                                        Interfaces.string(
                                                        "advertise.ad_name",
                                                        "clan_name" to clan.name
                                                )
                                                .toComponent()
                                )
                                .lore(
                                        splitLoreToComponents(
                                                Interfaces.string(
                                                        "advertise.ad_lore",
                                                        "owner" to owner,
                                                        "members" to membersSize.toString(),
                                                        "max_slots" to maxSlots.toString(),
                                                        "online" to online.toString(),
                                                        "join_type" to ad.joinType.displayName,
                                                        "remain" to remainTimeDisplay,
                                                        "top_position" to topPosition.toString(),
                                                        "created_at" to createdAt
                                                )
                                        )
                                )
                                .asGuiItem { event ->
                                    event.isCancelled = true

                                    // Обрабатываем клик по рекламе
                                    if (ad.joinType == ClanAdvertisementJoinType.OPEN) {
                                        // Если тип OPEN, сразу присоединяем игрока к клану
                                        val playerClan =
                                                ClanManager.Members.getClan(player.uniqueId)
                                        if (playerClan != null) {
                                            player.message("general.already_in_clan")
                                            return@asGuiItem
                                        }

                                        // Добавляем игрока в клан
                                        val memberModel =
                                                ClanManager.Members.member(
                                                        ad.clanId,
                                                        player.uniqueId
                                                )
                                        if (memberModel == null) {
                                            ClanManager.Invites.acceptInvite(
                                                    ad.clanId,
                                                    player.uniqueId
                                            )
                                            player.message(
                                                    "clan.joined",
                                                    mapOf("clan_name" to clan.name)
                                            )
                                            player.closeInventory()
                                        }
                                    } else {
                                        // Если тип INVITE, отправляем запрос на вступление
                                        val playerClan =
                                                ClanManager.Members.getClan(player.uniqueId)
                                        if (playerClan != null) {
                                            player.message("general.already_in_clan")
                                            return@asGuiItem
                                        }

                                        // Проверяем, есть ли уже приглашение
                                        if (ClanManager.Invites.hasInvite(
                                                        ad.clanId,
                                                        player.uniqueId
                                                )
                                        ) {
                                            player.message("invite.already_sent")
                                            return@asGuiItem
                                        }

                                        // Отправляем запрос на вступление
                                        ClanManager.Invites.createInvite(
                                                ad.clanId,
                                                player.uniqueId,
                                                player.name
                                        )
                                        player.message(
                                                "invite.sent",
                                                mapOf("clan_name" to clan.name)
                                        )
                                        player.closeInventory()
                                    }
                                }
                gui.addItem(adItem)
            }
        }

        // Проверяем, есть ли у игрока клан
        val playerClan = ClanManager.Members.getClan(player.uniqueId)
        val isAdmiral = if (playerClan != null) {
            val member = ClanManager.Members.member(playerClan.id, player.uniqueId)
            member != null && member.role == ClanMemberRole.ADMIRAL
        } else {
            false
        }
        val hasActiveAd = playerClan != null && AdvertisementManager.hasActiveAdvertisement(playerClan.id)

        // Добавляем алмаз в нижний ряд (создать рекламу) только если игрок адмирал и у клана нет рекламы
        if (isAdmiral && !hasActiveAd) {
            val diamond =
                    ItemBuilder.from(Material.DIAMOND)
                            .name(Interfaces.string("advertise.create_ad").toComponent())
                            .lore(
                                    splitLoreToComponents(
                                            Interfaces.string("advertise.create_ad_lore")
                                    ),
                            )
                            .asGuiItem { event ->
                                event.isCancelled = true
                                // Открываем интерфейс создания рекламы
                                player.closeInventory()

                                // Проверяем, есть ли у игрока клан
                                val clan = ClanManager.Members.getClan(player.uniqueId)
                                if (clan == null) {
                                    player.message("general.not_in_clan")
                                    return@asGuiItem
                                }

                                // Проверяем, является ли игрок адмиралом клана
                                val member = ClanManager.Members.member(clan.id, player.uniqueId)
                                if (member == null || member.role != ClanMemberRole.ADMIRAL) {
                                    player.message("general.not_admiral")
                                    return@asGuiItem
                                }

                                // Проверяем, есть ли уже активная реклама
                                if (AdvertisementManager.hasActiveAdvertisement(clan.id)) {
                                    player.message("advertise.already_has_ad")
                                    return@asGuiItem
                                }

                                // Открываем интерфейс создания рекламы
                                CreateAdvertiseUi(player, clan.id).open()
                            }
            gui.setItem(6, 1, diamond)
        }

        // Добавляем книгу с пером в нижний ряд (редактировать рекламу) только если игрок адмирал и у клана есть реклама
        if (isAdmiral && hasActiveAd) {
            val book =
                    ItemBuilder.from(Material.WRITABLE_BOOK)
                            .name(Interfaces.string("advertise.edit_ad").toComponent())
                            .lore(
                                    splitLoreToComponents(
                                            Interfaces.string("advertise.edit_ad_lore")
                                    )
                            )
                            .asGuiItem { event ->
                                event.isCancelled = true
                                // Проверяем, есть ли у игрока клан
                                val clan = ClanManager.Members.getClan(player.uniqueId)
                                if (clan == null) {
                                    player.message("general.not_in_clan")
                                    return@asGuiItem
                                }

                                // Проверяем, является ли игрок адмиралом клана
                                val member = ClanManager.Members.member(clan.id, player.uniqueId)
                                if (member == null || member.role != ClanMemberRole.ADMIRAL) {
                                    player.message("general.not_admiral")
                                    return@asGuiItem
                                }

                                // Проверяем, есть ли активная реклама
                                val ad = AdvertisementManager.getActiveAdvertisement(clan.id)
                                if (ad == null) {
                                    player.message("advertise.no_active_ad")
                                    return@asGuiItem
                                }

                                // Открываем интерфейс редактирования рекламы
                                EditAdvertiseUi(player, clan.id, ad).open()
                            }
            gui.setItem(6, 5, book)
        }

        // Добавляем кнопки навигации
        val prevButton =
                ItemBuilder.from(Material.ARROW)
                        .name(Interfaces.string("common.previous_page").toComponent())
                        .asGuiItem { event ->
                            event.isCancelled = true
                            gui.previous()
                        }
        gui.setItem(6, 4, prevButton)

        val nextButton =
                ItemBuilder.from(Material.ARROW)
                        .name(Interfaces.string("common.next_page").toComponent())
                        .asGuiItem { event ->
                            event.isCancelled = true
                            gui.next()
                        }
        gui.setItem(6, 6, nextButton)

        // Добавляем какао-бобы в нижний ряд (закрыть)
        val cocoaBeans =
                ItemBuilder.from(Material.COCOA_BEANS)
                        .name(Interfaces.string("common.close").toComponent())
                        .asGuiItem { event ->
                            event.isCancelled = true
                            // Закрываем инвентарь
                            player.closeInventory()
                        }
        gui.setItem(6, 9, cocoaBeans)
    }
}

/** UI для создания рекламы клана */
class CreateAdvertiseUi(private val player: Player, private val clanId: UUID) : Ui {
    val gui =
            Gui.gui()
                    .title(Interfaces.string("advertise.create_title").toComponent())
                    .rows(3)
                    .create()

    val clan = ClanManager.get(clanId)

    override fun open() {
        setupItems()
        gui.open(player)
    }

    private fun setupItems() {
        // Тариф на 12 часов
        gui.setItem(
                2, 
                3,
                ItemBuilder.from(Material.DIAMOND)
                        .name(Interfaces.string("advertise.tariff_12.name").toComponent())
                        .lore(
                                splitLoreToComponents(
                                        Interfaces.string(
                                                "advertise.tariff_12.lore_1",
                                                "cost" to
                                                        ClanAdvertisementTariff.TARIFF_12.cost
                                                                .toString()
                                        )
                                )
                        )
                        .asGuiItem { event ->
                            event.isCancelled = true
                            handleTariffSelection(ClanAdvertisementTariff.TARIFF_12)
                        }
        )

        // Тариф на 24 часа
        gui.setItem(
                2,
                5,
                ItemBuilder.from(Material.DIAMOND)
                        .name(Interfaces.string("advertise.tariff_24.name").toComponent())
                        .lore(
                                splitLoreToComponents(
                                        Interfaces.string(
                                                "advertise.tariff_24.lore",
                                                "cost" to
                                                        ClanAdvertisementTariff.TARIFF_24.cost
                                                                .toString()
                                        )
                                )
                        )
                        .asGuiItem { event ->
                            event.isCancelled = true
                            handleTariffSelection(ClanAdvertisementTariff.TARIFF_24)
                        }
        )

        // Закрыть
        gui.setItem(
                2,
                7,
                ItemBuilder.from(Material.GOLD_INGOT)
                        .name(
                                Interfaces.string(
                                                "clan_shop.treasury.name",
                                                "balance" to clan?.treasury.toString()
                                        )
                                        .toComponent()
                        )
                        .lore(splitLoreToComponents(Interfaces.string("clan_shop.treasury.lore")))
                        .asGuiItem { event -> event.isCancelled = true }
        )
    }

    private fun handleTariffSelection(tariff: ClanAdvertisementTariff) {
        player.closeInventory()

        // Проверяем баланс клана
        val clan = ClanManager.get(clanId) ?: return
        if (clan.treasury < tariff.cost) {
            player.message(
                    "advertise.not_enough_money",
                    mapOf("cost" to tariff.cost.toString(), "balance" to clan.treasury.toString())
            )
            return
        }

        // Открываем окно подтверждения покупки
        ConfirmPurchaseUi(player, clanId, tariff).open()
    }
}

/** UI для подтверждения покупки тарифа */
class ConfirmPurchaseUi(
        private val player: Player,
        private val clanId: UUID,
        private val tariff: ClanAdvertisementTariff
) : Ui {
    val gui =
            Gui.gui()
                    .title(Interfaces.string("advertise.confirm_title").toComponent())
                    .rows(3)
                    .create()

    override fun open() {
        setupItems()
        gui.open(player)
    }

    private fun setupItems() {
        // Информация о покупке
        gui.setItem(
                1,
                5,
                ItemBuilder.from(Material.DIAMOND)
                        .name(
                                Interfaces.string(
                                                "advertise.confirm_purchase_name",
                                                "tariff" to tariff.hours.toString(),
                                                "cost" to tariff.cost.toString()
                                        )
                                        .toComponent()
                        )
                        .lore(splitLoreToComponents(Interfaces.string("advertise.confirm_purchase_lore")))
                        .asGuiItem { event -> event.isCancelled = true }
        )

        // Кнопка отмены (красная шерсть)
        gui.setItem(
                2,
                7,
                ItemBuilder.from(Material.RED_WOOL)
                        .name(Interfaces.string("advertise.cancel_create_button").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.cancel_create_button_lore")))
                        .asGuiItem { event ->
                            event.isCancelled = true
                            player.closeInventory()
                            player.message("advertise.purchase_cancelled")
                        }
        )

        // Кнопка подтверждения (зеленая шерсть)
        gui.setItem(
                2,
                3,
                ItemBuilder.from(Material.LIME_WOOL)
                        .name(Interfaces.string("advertise.confirm_create_button").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.confirm_create_button_lore")))
                        .asGuiItem { event ->
                            event.isCancelled = true
                            player.closeInventory()

                            createAdvertisement(ClanAdvertisementJoinType.INVITE)
                        }
        )
    }


    private fun createAdvertisement(joinType: ClanAdvertisementJoinType, text: String = "Присоединяйтесь к нашему клану!") {
        // Создаем рекламу
        val clan = ClanManager.get(clanId) ?: return
        if (clan.treasury < tariff.cost) {
            player.message(
                    "advertise.not_enough_money",
                    mapOf("cost" to tariff.cost.toString(), "balance" to clan.treasury.toString())
            )
            return
        }


        // Снимаем деньги с баланса клана
        clan.treasury -= tariff.cost.toLong()
        ClanManager.update(clan)

        // Создаем рекламу
        val success = AdvertisementManager.createAdvertisement(clanId, joinType, tariff, text)

        if (success) {
            player.message(
                    "advertise.created_success",
                    mapOf("hours" to tariff.hours.toString(), "cost" to tariff.cost.toString())
            )

            val ad = AdvertisementManager.getActiveAdvertisement(clanId)
            if (ad == null) {
                player.message("advertise.no_active_ad")
                return
            }

            player.closeInventory()
            EditAdvertiseUi(player, clanId, ad).open()
        } else {
            player.message("advertise.created_error")
            // Возвращаем деньги
            clan.treasury += tariff.cost.toLong()
            ClanManager.update(clan)
        }
    }
}

/** UI для редактирования рекламы клана */
class EditAdvertiseUi(
        private val player: Player,
        private val clanId: UUID,
        private var advertisement: ClanAdvertisementModel
) : Ui {
    val gui =
            Gui.gui()
                    .title(Interfaces.string("advertise.edit_title").toComponent())
                    .rows(3)
                    .create()

    override fun open() {
        setupItems()
        gui.open(player)
    }

    private fun setupItems() {
        gui.setItem(
                2,
                2,
                ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Interfaces.string("advertise.edit_text").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.edit_text_lore")))
                        .asGuiItem { event -> event.isCancelled = true }
        )

        gui.setItem(
                2,
                5,
                ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Interfaces.string("advertise.ad_join_type").toComponent())
                        .lore(
                                splitLoreToComponents(
                                        getJoinTypeLore(advertisement.joinType)
                                )
                        )
                        .asGuiItem { event ->
                            event.isCancelled = true

                            // Определяем новый тип присоединения в зависимости от клика
                            val newJoinType =
                                    if (event.isLeftClick) {
                                        // ЛКМ - устанавливаем тип OPEN
                                        ClanAdvertisementJoinType.OPEN
                                    } else if (event.isRightClick) {
                                        // ПКМ - устанавливаем тип INVITE
                                        ClanAdvertisementJoinType.INVITE
                                    } else {
                                        return@asGuiItem
                                    }

                            // Если тип не изменился, ничего не делаем
                            if (newJoinType == advertisement.joinType) {
                                return@asGuiItem
                            }

                            // Обновляем тип присоединения в рекламе
                            val success =
                                    AdvertisementManager.updateAdvertisement(clanId, newJoinType)

                            if (success) {
                                // Уведомляем игрока
                                player.message(
                                        "advertise.join_type_changed",
                                        mapOf("type" to newJoinType.displayName)
                                )
                                
                                // Получаем обновленную рекламу и обновляем локальный объект
                                val updatedAd = AdvertisementManager.getActiveAdvertisement(clanId)
                                if (updatedAd != null) {
                                    // Обновляем локальный объект advertisement
                                    advertisement = updatedAd
                                    
                                    // Обновляем GUI
                                    setupItems()
                                    gui.update()
                                }
                            } else {
                                player.message("advertise.error_updating")
                            }
                        }
        )

        // Удалить рекламу
        gui.setItem(
                2,
                8,
                ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Interfaces.string("advertise.delete_ad").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.delete_ad_lore")))
                        .asGuiItem { event ->
                            event.isCancelled = true
                            player.closeInventory()

                            // Открываем окно подтверждения удаления
                            ConfirmDeleteUi(player, clanId).open()
                        }
        )
    }

    /**
     * Получает лор для кнопки типа присоединения с правильными цветами
     * @param joinType текущий тип присоединения
     * @return строка лора с цветами
     */
    private fun getJoinTypeLore(joinType: ClanAdvertisementJoinType): String {
        val requestColor = if (joinType == ClanAdvertisementJoinType.INVITE) "&a&l" else "&c"
        val openColor = if (joinType == ClanAdvertisementJoinType.OPEN) "&a&l" else "&c"

        return Interfaces.string(
                "advertise.ad_join_type_lore",
                "request_color" to requestColor,
                "open_color" to openColor
        )
    }
}

/** UI для подтверждения удаления рекламы */
class ConfirmDeleteUi(private val player: Player, private val clanId: UUID) : Ui {
    val gui =
            Gui.gui()
                    .title(Interfaces.string("advertise.confirm_delete_title").toComponent())
                    .rows(3)
                    .create()

    override fun open() {
        setupItems()
        gui.open(player)
    }

    private fun setupItems() {
        // Информация о удалении
        gui.setItem(
                1,
                5,
                ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Interfaces.string("advertise.confirm_delete_name").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.confirm_delete_lore")))
                        .asGuiItem { event -> event.isCancelled = true }
        )

        // Кнопка отмены (красная шерсть)
        gui.setItem(
                2,
                7,
                ItemBuilder.from(Material.RED_WOOL)
                        .name(Interfaces.string("advertise.cancel_delete_button").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.cancel_delete_button_lore")))
                        .asGuiItem { event ->
                            event.isCancelled = true
                            player.closeInventory()
                            player.message("advertise.delete_cancelled")
                        }
        )

        // Кнопка подтверждения (зеленая шерсть)
        gui.setItem(
                2,
                3,
                ItemBuilder.from(Material.LIME_WOOL)
                        .name(Interfaces.string("advertise.confirm_delete_button").toComponent())
                        .lore(splitLoreToComponents(Interfaces.string("advertise.confirm_delete_button_lore")))
                        .asGuiItem { event ->
                            event.isCancelled = true
                            player.closeInventory()

                            // Удаляем рекламу
                            val success = AdvertisementManager.removeAdvertisement(clanId)

                            if (success) {
                                player.message("advertise.deleted_success")
                            } else {
                                player.message("advertise.deleted_error")
                            }
                        }
        )
    }
}
