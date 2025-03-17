package org.mmarket.clans.system.manager

import java.time.LocalDateTime
import java.util.UUID
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.mmarket.clans.api.utility.FixedSizeList
import org.mmarket.clans.system.model.ClanInviteModel
import org.mmarket.clans.system.model.ClanMemberModel
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.system.model.ClanModel
import org.mmarket.clans.system.model.ClanScoreModel
import org.mmarket.clans.system.model.ClanSlots
import org.mmarket.clans.system.table.ClanInvitesTable
import org.mmarket.clans.system.table.ClanMembersTable
import org.mmarket.clans.system.table.ClanScoresTable
import org.mmarket.clans.system.table.ClansTable

/**
 * Менеджер для работы с кланами. Обеспечивает взаимодействие между моделями кланов и таблицами базы
 * данных.
 */
object ClanManager {
    private val clansCache = mutableMapOf<UUID, ClanModel>()
    private lateinit var database: Database

    /** Загружает все кланы из базы данных в кэш */
    fun init(db: Database) {
        database = db

        // Загружаем все кланы
        database.from(ClansTable).select().map { row ->
            val clanId = row[ClansTable.id]!!
            val clan = convertRowToClanModel(row)
            clansCache[clanId] = clan
            clan
        }

        // Инициализируем кэш участников
        Members.init()

        // Инициализируем кэш приглашений
        Invites.init()
        
        // Инициализируем кэш очков
        Scores.init()
    }

    /**
     * Получает все кланы
     *
     * @return список всех кланов
     */
    fun clans(): List<ClanModel> {
        val clans =
                database.from(ClansTable)
                        .select()
                        .map { row -> convertRowToClanModel(row) }
                        .toList()

        return clans
    }

    /**
     * Получает клан по его ID
     *
     * @param id ID клана
     * @return модель клана или null, если клан не найден
     */
    fun get(id: UUID): ClanModel? {
        // Проверяем кэш
        if (clansCache.containsKey(id)) {
            return clansCache[id]
        }

        // Если нет в кэше, пробуем загрузить из БД
        return database.from(ClansTable)
                .select()
                .where { ClansTable.id eq id }
                .map { row ->
                    val clan = convertRowToClanModel(row)
                    clansCache[id] = clan
                    clan
                }
                .firstOrNull()
    }

    /**
     * Получает клан по его имени
     *
     * @param name имя клана
     * @return модель клана или null, если клан не найден
     */
    fun get(name: String): ClanModel? {
        // Проверяем кэш
        val clan = clansCache.values.find { it.name.equals(name, ignoreCase = true) }
        if (clan != null) {
            return clan
        }

        // Если нет в кэше, пробуем загрузить из БД
        return database.from(ClansTable)
                .select()
                .where { ClansTable.name eq name }
                .map { row ->
                    val clanModel = convertRowToClanModel(row)
                    clansCache[clanModel.id] = clanModel
                    clanModel
                }
                .firstOrNull()
    }

    /**
     * Создает новый клан
     *
     * @param name имя клана
     * @param colorlessName имя клана без цветовых кодов
     * @param creator UUID создателя клана
     * @return модель созданного клана
     */
    fun create(name: String, colorlessName: String, creator: UUID, creatorName: String): ClanModel {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Создаем клан в БД
        database.insert(ClansTable) {
            set(it.id, id)
            set(it.name, name)
            set(it.colorlessName, colorlessName)
            set(it.treasury, 0L)
            set(it.news, "[]")
            set(it.motd, "")
            set(it.creator, creator)
            set(it.owner, creator)
            set(it.createdAt, now)
            set(it.slots, ClanSlots.INITIAL)
            set(it.chatPurchased, false)
            set(it.motdPurchased, false)
            set(it.partyPurchased, false)
        }

        // Создаем модель клана
        val newsList = FixedSizeList<String>(10)

        val clan =
                ClanModel(
                        id = id,
                        name = name,
                        colorlessName = colorlessName,
                        treasury = 0,
                        score = 0,
                        news = newsList,
                        motd = "",
                        creator = creator,
                        owner = creator,
                        members = mutableSetOf(),
                        createdAt = now,
                        slots = ClanSlots.INITIAL,
                        chatPurchased = false,
                        motdPurchased = false,
                        partyPurchased = false
                )

        // Добавляем владельца как участника с ролью ADMIRAL
        addMember(id, creator, creatorName, ClanMemberRole.ADMIRAL)

        // Добавляем в кэш
        clansCache[id] = clan

        return clan
    }

    /**
     * Обновляет информацию о клане
     *
     * @param clan модель клана
     */
    fun update(clan: ClanModel) {
        database.update(ClansTable) {
            set(it.name, clan.name)
            set(it.colorlessName, clan.colorlessName)
            set(it.treasury, clan.treasury)
            set(it.news, "[${clan.news.toList().joinToString(",") { "\"$it\"" }}]")
            set(it.motd, clan.motd)
            set(it.owner, clan.owner)
            set(it.slots, clan.slots)
            set(it.chatPurchased, clan.chatPurchased)
            set(it.motdPurchased, clan.motdPurchased)
            set(it.partyPurchased, clan.partyPurchased)
            where { it.id eq clan.id }
        }

        // Обновляем кэш
        clansCache[clan.id] = clan
    }

    /**
     * Добавляет сумму в бюджет клана
     *
     * @param clanId ID клана
     * @param amount сумма
     */
    fun addTreasury(clanId: UUID, amount: Long) {
        // Получаем текущее значение treasury из базы данных
        val currentTreasury = database.from(ClansTable)
            .select(ClansTable.treasury)
            .where { ClansTable.id eq clanId }
            .map { it[ClansTable.treasury] ?: 0L }
            .firstOrNull() ?: 0L
            
        // Обновляем в базе данных
        database.update(ClansTable) {
            set(it.treasury, currentTreasury + amount)
            where { it.id eq clanId }
        }
        
        // Обновляем кэш
        val clan = clansCache[clanId]
        if (clan != null) {
            clan.treasury += amount
            clansCache[clanId] = clan
        }
    }

    /**
     * Вычитает сумму из бюджета клана
     *
     * @param clanId ID клана
     * @param amount сумма
     */
    fun subtractTreasury(clanId: UUID, amount: Long) {
        // Получаем текущее значение treasury из базы данных
        val currentTreasury = database.from(ClansTable)
            .select(ClansTable.treasury)
            .where { ClansTable.id eq clanId }
            .map { it[ClansTable.treasury] ?: 0L }
            .firstOrNull() ?: 0L
            
        // Обновляем в базе данных
        database.update(ClansTable) {
            set(it.treasury, currentTreasury - amount)
            where { it.id eq clanId }
        }
        
        // Обновляем кэш
        val clan = clansCache[clanId]
        if (clan != null) {
            clan.treasury -= amount
            clansCache[clanId] = clan
        }
    }

    /**
     * Добавляет участника в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @param playerName Ник игрока
     * @param role роль участника
     * @return true, если участник успешно добавлен
     */
    fun addMember(
            clanId: UUID,
            playerUuid: UUID,
            playerName: String,
            role: ClanMemberRole = ClanMemberRole.RECRUIT
    ): Boolean {
        val now = LocalDateTime.now()

        // Проверяем, существует ли клан
        val clanExists =
                database.from(ClansTable)
                        .select(count(ClansTable.id))
                        .where { ClansTable.id eq clanId }
                        .map { it.getInt(1) }
                        .first() > 0

        if (!clanExists) return false

        // Проверяем, не состоит ли игрок уже в клане
        val alreadyInClan =
                database.from(ClanMembersTable)
                        .select(count(ClanMembersTable.id))
                        .where { ClanMembersTable.uuid eq playerUuid }
                        .map { it.getInt(1) }
                        .first() > 0

        if (alreadyInClan) return false

        // Добавляем участника
        database.insert(ClanMembersTable) {
            set(it.clanId, clanId)
            set(it.uuid, playerUuid)
            set(it.name, playerName)
            set(it.role, role)
            set(it.joinedAt, now)
        }

        // Обновляем кэш
        val clan = clansCache[clanId]
        if (clan != null) {
            val memberModel =
                    ClanMemberModel(
                            uuid = playerUuid,
                            name = playerName,
                            role = role,
                            joinedAt = now
                    )
            clan.members.add(memberModel)

            // Обновляем кэш участников
            Members.updateCacheOnAdd(clanId, memberModel)

            // Удаляем приглашение, если оно было
            Invites.removeInvite(clanId, playerUuid)
        }

        return true
    }

    /**
     * Удаляет участника из клана
     *
     * @param playerUuid UUID игрока
     * @return true, если участник успешно удален
     */
    fun removeMember(playerUuid: UUID): Boolean {
        // Находим запись участника клана
        val memberRow =
                database.from(ClanMembersTable)
                        .select()
                        .where { ClanMembersTable.uuid eq playerUuid }
                        .map { row ->
                            Pair(row[ClanMembersTable.clanId]!!, row[ClanMembersTable.uuid]!!)
                        }
                        .firstOrNull()

        if (memberRow == null) return false

        val clanId = memberRow.first

        // Удаляем участника
        val result = database.delete(ClanMembersTable) { it.uuid eq playerUuid } > 0

        // Обновляем кэш
        if (result) {
            val clan = clansCache[clanId]
            clan?.members?.removeIf { it.uuid == playerUuid }

            // Обновляем кэш участников
            Members.updateCacheOnRemove(playerUuid)
        }

        return result
    }

    /**
     * Обновляет роль участника клана
     *
     * @param playerUuid UUID игрока
     * @param newRole новая роль
     * @return true, если роль успешно обновлена
     */
    fun updateMemberRole(playerUuid: UUID, newRole: ClanMemberRole): Boolean {
        // Находим запись участника клана
        val memberRow =
                database.from(ClanMembersTable)
                        .select()
                        .where { ClanMembersTable.uuid eq playerUuid }
                        .map { row ->
                            Pair(row[ClanMembersTable.clanId]!!, row[ClanMembersTable.uuid]!!)
                        }
                        .firstOrNull()

        if (memberRow == null) return false

        val clanId = memberRow.first

        // Обновляем роль
        val result =
                database.update(ClanMembersTable) {
                    set(it.role, newRole)
                    where { it.uuid eq playerUuid }
                } > 0

        // Обновляем кэш
        if (result) {
            val clan = clansCache[clanId]
            clan?.members?.find { it.uuid == playerUuid }?.role = newRole

            // Обновляем кэш участников
            Members.updateCacheOnRoleChange(playerUuid, newRole)
        }

        return result
    }

    /**
     * Создает приглашение в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @return true, если приглашение успешно создано
     */
    fun createInvite(clanId: UUID, playerUuid: UUID, playerName: String): Boolean {
        return Invites.createInvite(clanId, playerUuid, playerName)
    }

    /**
     * Удаляет приглашение в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @return true, если приглашение успешно удалено
     */
    fun removeInvite(clanId: UUID, playerUuid: UUID): Boolean {
        return Invites.removeInvite(clanId, playerUuid)
    }

    /**
     * Проверяет, существует ли приглашение в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @return true, если приглашение существует
     */
    fun hasInvite(clanId: UUID, playerUuid: UUID): Boolean {
        return Invites.hasInvite(clanId, playerUuid)
    }

    /**
     * Получает список всех приглашений игрока
     *
     * @param playerUuid UUID игрока
     * @return список ID кланов, в которые приглашен игрок
     */
    fun getPlayerInvites(playerUuid: UUID): List<UUID> {
        return Invites.getPlayerInvites(playerUuid)
    }

    /**
     * Удаляет клан
     *
     * @param clanId ID клана
     * @return true, если клан успешно удален
     */
    fun delete(clanId: UUID): Boolean {
        // Получаем список участников клана для обновления кэша
        val members =
                database.from(ClanMembersTable)
                        .select()
                        .where { ClanMembersTable.clanId eq clanId }
                        .map { row -> row[ClanMembersTable.uuid]!! }
                        .toList()

        // Получаем список приглашений клана для обновления кэша
        val invites = Invites.getClanInvites(clanId)

        // Удаляем клан (каскадно удалятся участники и приглашения)
        val result = database.delete(ClansTable) { it.id eq clanId } > 0

        // Обновляем кэш
        if (result) {
            clansCache.remove(clanId)

            // Обновляем кэш участников
            members.forEach { Members.updateCacheOnRemove(it) }

            // Обновляем кэш приглашений
            invites.forEach { Invites.updateCacheOnRemove(clanId, it) }
        }

        return result
    }

    /** Объект для работы с приглашениями в кланы */
    object Invites {
        /** Ключ - UUID игрока, значение - список моделей приглашений */
        private val playerInvitesCache = mutableMapOf<UUID, MutableList<ClanInviteModel>>()

        /** Ключ - ID клана, значение - список моделей приглашений */
        private val clanInvitesCache = mutableMapOf<UUID, MutableList<ClanInviteModel>>()

        /** Инициализирует кэш приглашений */
        fun init() {
            database.from(ClanInvitesTable).select().map { row ->
                val playerUuid = row[ClanInvitesTable.uuid]!!
                val clanId = row[ClanInvitesTable.clanId]!!
                val playerName = row[ClanInvitesTable.name]!!
                val createdAt = row[ClanInvitesTable.createdAt]!!

                val inviteModel =
                        ClanInviteModel(
                                clanId = clanId,
                                playerUuid = playerUuid,
                                playerName = playerName,
                                createdAt = createdAt
                        )

                // Добавляем в кэш приглашений игрока
                if (!playerInvitesCache.containsKey(playerUuid)) {
                    playerInvitesCache[playerUuid] = mutableListOf()
                }
                playerInvitesCache[playerUuid]?.add(inviteModel)

                // Добавляем в кэш приглашений клана
                if (!clanInvitesCache.containsKey(clanId)) {
                    clanInvitesCache[clanId] = mutableListOf()
                }
                clanInvitesCache[clanId]?.add(inviteModel)
            }
        }

        /**
         * Создает приглашение в клан
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @param playerName Ник игрока
         * @return true, если приглашение успешно создано
         */
        fun createInvite(clanId: UUID, playerUuid: UUID, playerName: String): Boolean {
            // Проверяем, существует ли клан
            val clanExists =
                    database.from(ClansTable)
                            .select(count(ClansTable.id))
                            .where { ClansTable.id eq clanId }
                            .map { it.getInt(1) }
                            .first() > 0

            if (!clanExists) return false

            // Проверяем, не состоит ли игрок уже в клане
            val alreadyInClan = Members.inClan(playerUuid)

            if (alreadyInClan) return false

            // Проверяем, нет ли уже приглашения
            if (hasInvite(clanId, playerUuid)) return false

            val now = LocalDateTime.now()

            // Создаем приглашение
            database.insert(ClanInvitesTable) {
                set(it.clanId, clanId)
                set(it.uuid, playerUuid)
                set(it.name, playerName)
                set(it.createdAt, now)
            }

            // Создаем модель приглашения
            val inviteModel =
                    ClanInviteModel(
                            clanId = clanId,
                            playerUuid = playerUuid,
                            playerName = playerName,
                            createdAt = now
                    )

            // Обновляем кэш
            updateCacheOnAdd(inviteModel)

            return true
        }

        /**
         * Удаляет приглашение в клан
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return true, если приглашение успешно удалено
         */
        fun removeInvite(clanId: UUID, playerUuid: UUID): Boolean {
            val result =
                    database.delete(ClanInvitesTable) {
                        (it.clanId eq clanId) and (it.uuid eq playerUuid)
                    } > 0

            // Обновляем кэш
            if (result) {
                updateCacheOnRemove(clanId, playerUuid)
            }

            return result
        }

        /**
         * Удаляет все приглашения игрока
         *
         * @param playerUuid UUID игрока
         * @return количество удаленных приглашений
         */
        fun removeAllPlayerInvites(playerUuid: UUID): Int {
            // Получаем список приглашений игрока
            val invites = getPlayerInviteModels(playerUuid)

            // Удаляем все приглашения
            val result = database.delete(ClanInvitesTable) { it.uuid eq playerUuid }

            // Обновляем кэш
            invites.forEach { invite -> updateCacheOnRemove(invite.clanId, invite.playerUuid) }

            return result
        }

        /**
         * Удаляет все приглашения в клан
         *
         * @param clanId ID клана
         * @return количество удаленных приглашений
         */
        fun removeAllClanInvites(clanId: UUID): Int {
            // Получаем список приглашений клана
            val invites = getClanInviteModels(clanId)

            // Удаляем все приглашения
            val result = database.delete(ClanInvitesTable) { it.clanId eq clanId }

            // Обновляем кэш
            invites.forEach { invite -> updateCacheOnRemove(invite.clanId, invite.playerUuid) }

            return result
        }

        /**
         * Проверяет, существует ли приглашение в клан
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return true, если приглашение существует
         */
        fun hasInvite(clanId: UUID, playerUuid: UUID): Boolean {
            // Проверяем кэш
            if (playerInvitesCache.containsKey(playerUuid)) {
                return playerInvitesCache[playerUuid]?.any { it.clanId == clanId } ?: false
            }

            // Если нет в кэше, проверяем в БД
            return database.from(ClanInvitesTable)
                    .select(count(ClanInvitesTable.id))
                    .where {
                        (ClanInvitesTable.clanId eq clanId) and
                                (ClanInvitesTable.uuid eq playerUuid)
                    }
                    .map { it.getInt(1) }
                    .first() > 0
        }

        /**
         * Получает список всех приглашений игрока
         *
         * @param playerUuid UUID игрока
         * @return список ID кланов, в которые приглашен игрок
         */
        fun getPlayerInvites(playerUuid: UUID): List<UUID> {
            return getPlayerInviteModels(playerUuid).map { it.clanId }
        }

        /**
         * Получает список всех моделей приглашений игрока
         *
         * @param playerUuid UUID игрока
         * @return список моделей приглашений
         */
        fun getPlayerInviteModels(playerUuid: UUID): List<ClanInviteModel> {
            // Проверяем кэш
            if (playerInvitesCache.containsKey(playerUuid)) {
                return playerInvitesCache[playerUuid]?.toList() ?: emptyList()
            }

            // Если нет в кэше, загружаем из БД
            val invites =
                    database.from(ClanInvitesTable)
                            .select()
                            .where { ClanInvitesTable.uuid eq playerUuid }
                            .map { row ->
                                ClanInviteModel(
                                        clanId = row[ClanInvitesTable.clanId]!!,
                                        playerUuid = row[ClanInvitesTable.uuid]!!,
                                        playerName = row[ClanInvitesTable.name]!!,
                                        createdAt = row[ClanInvitesTable.createdAt]!!
                                )
                            }
                            .toList()

            // Обновляем кэш
            playerInvitesCache[playerUuid] = invites.toMutableList()

            // Обновляем кэш кланов
            invites.forEach { invite ->
                if (!clanInvitesCache.containsKey(invite.clanId)) {
                    clanInvitesCache[invite.clanId] = mutableListOf()
                }
                clanInvitesCache[invite.clanId]?.add(invite)
            }

            return invites
        }

        /**
         * Получает список всех приглашений в клан
         *
         * @param clanId ID клана
         * @return список UUID игроков, приглашенных в клан
         */
        fun getClanInvites(clanId: UUID): List<UUID> {
            return getClanInviteModels(clanId).map { it.playerUuid }
        }

        /**
         * Получает список всех моделей приглашений в клан
         *
         * @param clanId ID клана
         * @return список моделей приглашений
         */
        fun getClanInviteModels(clanId: UUID): List<ClanInviteModel> {
            // Проверяем кэш
            if (clanInvitesCache.containsKey(clanId)) {
                return clanInvitesCache[clanId]?.toList() ?: emptyList()
            }

            // Если нет в кэше, загружаем из БД
            val invites =
                    database.from(ClanInvitesTable)
                            .select()
                            .where { ClanInvitesTable.clanId eq clanId }
                            .map { row ->
                                ClanInviteModel(
                                        clanId = row[ClanInvitesTable.clanId]!!,
                                        playerUuid = row[ClanInvitesTable.uuid]!!,
                                        playerName = row[ClanInvitesTable.name]!!,
                                        createdAt = row[ClanInvitesTable.createdAt]!!
                                )
                            }
                            .toList()

            // Обновляем кэш
            clanInvitesCache[clanId] = invites.toMutableList()

            // Обновляем кэш игроков
            invites.forEach { invite ->
                if (!playerInvitesCache.containsKey(invite.playerUuid)) {
                    playerInvitesCache[invite.playerUuid] = mutableListOf()
                }
                playerInvitesCache[invite.playerUuid]?.add(invite)
            }

            return invites
        }

        /**
         * Получает количество приглашений в клан
         *
         * @param clanId ID клана
         * @return количество приглашений
         */
        fun getClanInvitesCount(clanId: UUID): Int {
            // Проверяем кэш
            if (clanInvitesCache.containsKey(clanId)) {
                return clanInvitesCache[clanId]?.size ?: 0
            }

            // Если нет в кэше, считаем в БД
            return database.from(ClanInvitesTable)
                    .select(count(ClanInvitesTable.id))
                    .where { ClanInvitesTable.clanId eq clanId }
                    .map { it.getInt(1) }
                    .first()
        }

        /**
         * Получает количество приглашений игрока
         *
         * @param playerUuid UUID игрока
         * @return количество приглашений
         */
        fun getPlayerInvitesCount(playerUuid: UUID): Int {
            // Проверяем кэш
            if (playerInvitesCache.containsKey(playerUuid)) {
                return playerInvitesCache[playerUuid]?.size ?: 0
            }

            // Если нет в кэше, считаем в БД
            return database.from(ClanInvitesTable)
                    .select(count(ClanInvitesTable.id))
                    .where { ClanInvitesTable.uuid eq playerUuid }
                    .map { it.getInt(1) }
                    .first()
        }

        /**
         * Получает модель приглашения
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return модель приглашения или null, если не найдено
         */
        fun getInvite(clanId: UUID, playerUuid: UUID): ClanInviteModel? {
            // Проверяем кэш
            playerInvitesCache[playerUuid]?.find { it.clanId == clanId }?.let {
                return it
            }

            // Если нет в кэше, ищем в БД
            return database.from(ClanInvitesTable)
                    .select()
                    .where {
                        (ClanInvitesTable.clanId eq clanId) and
                                (ClanInvitesTable.uuid eq playerUuid)
                    }
                    .map { row ->
                        ClanInviteModel(
                                clanId = row[ClanInvitesTable.clanId]!!,
                                playerUuid = row[ClanInvitesTable.uuid]!!,
                                playerName = row[ClanInvitesTable.name]!!,
                                createdAt = row[ClanInvitesTable.createdAt]!!
                        )
                    }
                    .firstOrNull()
                    ?.also { invite ->
                        // Обновляем кэш
                        updateCacheOnAdd(invite)
                    }
        }

        /**
         * Получает модель приглашения
         *
         * @param clanId ID клана
         * @param playerName Ник игрока
         * @return модель приглашения или null, если не найдено
         */
        fun getInvite(clanId: UUID, playerName: String): ClanInviteModel? {
            return database.from(ClanInvitesTable)
                    .select()
                    .where {
                        (ClanInvitesTable.clanId eq clanId) and
                                (ClanInvitesTable.name eq playerName)
                    }
                    .map { row ->
                        ClanInviteModel(
                                clanId = row[ClanInvitesTable.clanId]!!,
                                playerUuid = row[ClanInvitesTable.uuid]!!,
                                playerName = row[ClanInvitesTable.name]!!,
                                createdAt = row[ClanInvitesTable.createdAt]!!
                        )
                    }
                    .firstOrNull()
                    ?.also { invite ->
                        // Обновляем кэш
                        updateCacheOnAdd(invite)
                    }
        }

        /**
         * Обновляет кэш при добавлении приглашения
         *
         * @param invite модель приглашения
         */
        internal fun updateCacheOnAdd(invite: ClanInviteModel) {
            val clanId = invite.clanId
            val playerUuid = invite.playerUuid

            // Обновляем кэш приглашений игрока
            if (!playerInvitesCache.containsKey(playerUuid)) {
                playerInvitesCache[playerUuid] = mutableListOf()
            }
            if (!playerInvitesCache[playerUuid]?.any { it.clanId == clanId }!!) {
                playerInvitesCache[playerUuid]?.add(invite)
            }

            // Обновляем кэш приглашений клана
            if (!clanInvitesCache.containsKey(clanId)) {
                clanInvitesCache[clanId] = mutableListOf()
            }
            if (!clanInvitesCache[clanId]?.any { it.playerUuid == playerUuid }!!) {
                clanInvitesCache[clanId]?.add(invite)
            }
        }

        /**
         * Обновляет кэш при удалении приглашения
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         */
        internal fun updateCacheOnRemove(clanId: UUID, playerUuid: UUID) {
            // Обновляем кэш приглашений игрока
            playerInvitesCache[playerUuid]?.removeIf { it.clanId == clanId }
            if (playerInvitesCache[playerUuid]?.isEmpty() == true) {
                playerInvitesCache.remove(playerUuid)
            }

            // Обновляем кэш приглашений клана
            clanInvitesCache[clanId]?.removeIf { it.playerUuid == playerUuid }
            if (clanInvitesCache[clanId]?.isEmpty() == true) {
                clanInvitesCache.remove(clanId)
            }
        }

        /**
         * Принимает приглашение в клан
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return true, если приглашение успешно принято
         */
        fun acceptInvite(clanId: UUID, playerUuid: UUID): Boolean {
            // Проверяем, существует ли приглашение
            if (!hasInvite(clanId, playerUuid)) return false

            // Получаем имя игрока из базы данных
            val playerName =
                    database.from(ClanMembersTable)
                            .select(ClanMembersTable.name)
                            .where { ClanMembersTable.uuid eq playerUuid }
                            .map { it[ClanMembersTable.name] }
                            .firstOrNull()
                            ?: return false

            // Удаляем приглашение и добавляем игрока в клан
            removeInvite(clanId, playerUuid)
            return addMember(clanId, playerUuid, playerName)
        }

        /**
         * Отклоняет приглашение в клан
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return true, если приглашение успешно отклонено
         */
        fun declineInvite(clanId: UUID, playerUuid: UUID): Boolean {
            return removeInvite(clanId, playerUuid)
        }
    }

    object Members {
        /** Ключ - UUID игрока, значение - пара (ID клана, модель участника) */
        private val membersCache = mutableMapOf<UUID, Pair<UUID, ClanMemberModel>>()

        /** Инициализирует кэш участников кланов */
        fun init() {
            database.from(ClanMembersTable).select().map { row ->
                val playerUuid = row[ClanMembersTable.uuid]!!
                val clanId = row[ClanMembersTable.clanId]!!
                val memberModel =
                        ClanMemberModel(
                                uuid = playerUuid,
                                name = row[ClanMembersTable.name]!!,
                                role = row[ClanMembersTable.role]!!,
                                joinedAt = row[ClanMembersTable.joinedAt]!!
                        )
                membersCache[playerUuid] = Pair(clanId, memberModel)
            }
        }

        /**
         * Получает клан, в котором состоит игрок
         *
         * @param playerUuid UUID игрока
         * @return модель клана или null, если игрок не состоит в клане
         */
        fun getClan(playerUuid: UUID): ClanModel? {
            // Проверяем кэш участников
            val memberData = membersCache[playerUuid]
            if (memberData != null) {
                val clanId = memberData.first
                return clansCache[clanId]
            }

            // Если нет в кэше, пробуем загрузить из БД
            val memberRow =
                    database.from(ClanMembersTable)
                            .select()
                            .where { ClanMembersTable.uuid eq playerUuid }
                            .map { row ->
                                val clanId = row[ClanMembersTable.clanId] ?: return@map null
                                val uuid = row[ClanMembersTable.uuid] ?: return@map null
                                val name = row[ClanMembersTable.name] ?: return@map null
                                val role = row[ClanMembersTable.role] ?: return@map null
                                val joinedAt = row[ClanMembersTable.joinedAt] ?: return@map null

                                Triple(
                                        clanId,
                                        uuid,
                                        ClanMemberModel(
                                                uuid = uuid,
                                                name = name,
                                                role = role,
                                                joinedAt = joinedAt
                                        )
                                )
                            }
                            .firstOrNull()

            if (memberRow == null) return null

            val clanId = memberRow.first
            val memberModel = memberRow.third

            // Добавляем в кэш
            membersCache[playerUuid] = Pair(clanId, memberModel)

            // Возвращаем клан
            return clansCache[clanId] ?: get(clanId)
        }

        /**
         * Проверяет, состоит ли игрок в клане
         *
         * @param playerUuid UUID игрока
         * @return true, если игрок состоит в клане
         */
        fun inClan(playerUuid: UUID): Boolean {
            return membersCache.containsKey(playerUuid) ||
                    database.from(ClanMembersTable)
                            .select(count(ClanMembersTable.id))
                            .where { ClanMembersTable.uuid eq playerUuid }
                            .map { it.getInt(1) }
                            .first() > 0
        }

        /**
         * Проверяет, состоит ли игрок в указанном клане
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return true, если игрок состоит в указанном клане
         */
        fun inClan(clanId: UUID, playerUuid: UUID): Boolean {
            // Проверяем кэш
            val memberData = membersCache[playerUuid]
            if (memberData != null) {
                return memberData.first == clanId
            }

            // Если нет в кэше, проверяем в БД
            return database.from(ClanMembersTable)
                    .select(count(ClanMembersTable.id))
                    .where {
                        (ClanMembersTable.clanId eq clanId) and
                                (ClanMembersTable.uuid eq playerUuid)
                    }
                    .map { it.getInt(1) }
                    .first() > 0
        }

        /**
         * Получает роль игрока в клане
         *
         * @param playerUuid UUID игрока
         * @return роль игрока или null, если игрок не состоит в клане
         */
        fun role(playerUuid: UUID): ClanMemberRole? {
            // Проверяем кэш
            val memberData = membersCache[playerUuid]
            if (memberData != null) {
                return memberData.second.role
            }

            // Если нет в кэше, проверяем в БД
            return database.from(ClanMembersTable)
                    .select()
                    .where { ClanMembersTable.uuid eq playerUuid }
                    .map { row ->
                        val clanId = row[ClanMembersTable.clanId] ?: return@map null
                        val name = row[ClanMembersTable.name] ?: return@map null
                        val role = row[ClanMembersTable.role] ?: return@map null
                        val joinedAt = row[ClanMembersTable.joinedAt] ?: return@map null

                        val memberModel =
                                ClanMemberModel(
                                        uuid = playerUuid,
                                        name = name,
                                        role = role,
                                        joinedAt = joinedAt
                                )

                        // Добавляем в кэш
                        membersCache[playerUuid] = Pair(clanId, memberModel)

                        role
                    }
                    .firstOrNull()
        }

        /**
         * Получает всех участников клана
         *
         * @param clanId ID клана
         * @return список моделей участников клана
         */
        fun members(clanId: UUID): Set<ClanMemberModel> {
            // Проверяем кэш кланов
            val clan = clansCache[clanId]
            if (clan != null) {
                return clan.members
            }

            // Если клан не в кэше, загружаем участников из БД
            return database.from(ClanMembersTable)
                    .select()
                    .where { ClanMembersTable.clanId eq clanId }
                    .map { row ->
                        val playerUuid = row[ClanMembersTable.uuid]!!
                        val memberModel =
                                ClanMemberModel(
                                        uuid = playerUuid,
                                        name = row[ClanMembersTable.name]!!,
                                        role = row[ClanMembersTable.role]!!,
                                        joinedAt = row[ClanMembersTable.joinedAt]!!
                                )

                        // Добавляем в кэш участников
                        membersCache[playerUuid] = Pair(clanId, memberModel)

                        memberModel
                    }
                    .toSet()
        }

        /**
         * Получает конкретного участника клана по его UUID
         *
         * @param clanId ID клана
         * @param playerUuid UUID игрока
         * @return модель участника клана или null, если не найден
         */
        fun member(clanId: UUID, playerUuid: UUID): ClanMemberModel? {
            // Сначала проверяем кэш участников
            membersCache[playerUuid]?.let { (cachedClanId, member) ->
                if (cachedClanId == clanId) {
                    return member
                }
            }

            // Если не найдено в кэше, ищем в БД
            return database.from(ClanMembersTable)
                    .select()
                    .where {
                        ClanMembersTable.clanId eq clanId and (ClanMembersTable.uuid eq playerUuid)
                    }
                    .map { row -> // Преобразуем каждую строку в модель [[2]][[4]]
                        ClanMemberModel(
                                uuid = row[ClanMembersTable.uuid]!!,
                                name = row[ClanMembersTable.name]!!,
                                role = row[ClanMembersTable.role]!!,
                                joinedAt = row[ClanMembersTable.joinedAt]!!
                        )
                    }
                    .firstOrNull() // Берем первую запись или null [[8]]
                    ?.also { memberModel -> // Кэшируем результат [[6]]
                        membersCache[playerUuid] = Pair(clanId, memberModel)
                    }
        }

        /**
         * Получает конкретного участника клана по его UUID
         *
         * @param clanId ID клана
         * @param playerName Ник игрока
         * @return модель участника клана или null, если не найден
         */
        fun member(clanId: UUID, playerName: String): ClanMemberModel? {
            return database.from(ClanMembersTable)
                    .select()
                    .where {
                        ClanMembersTable.clanId eq clanId and (ClanMembersTable.name eq playerName)
                    }
                    .map { row -> // Преобразуем каждую строку в модель [[2]][[4]]
                        ClanMemberModel(
                                uuid = row[ClanMembersTable.uuid]!!,
                                name = row[ClanMembersTable.name]!!,
                                role = row[ClanMembersTable.role]!!,
                                joinedAt = row[ClanMembersTable.joinedAt]!!
                        )
                    }
                    .firstOrNull()
        }

        /**
         * Обновляет кэш при добавлении участника в клан
         *
         * @param clanId ID клана
         * @param memberModel модель участника
         */
        internal fun updateCacheOnAdd(clanId: UUID, memberModel: ClanMemberModel) {
            membersCache[memberModel.uuid] = Pair(clanId, memberModel)
        }

        /**
         * Обновляет кэш при удалении участника из клана
         *
         * @param playerUuid UUID игрока
         */
        internal fun updateCacheOnRemove(playerUuid: UUID) {
            membersCache.remove(playerUuid)
        }

        /**
         * Обновляет кэш при изменении роли участника
         *
         * @param playerUuid UUID игрока
         * @param newRole новая роль
         */
        internal fun updateCacheOnRoleChange(playerUuid: UUID, newRole: ClanMemberRole) {
            val memberData = membersCache[playerUuid]
            if (memberData != null) {
                val (clanId, memberModel) = memberData
                memberModel.role = newRole
                membersCache[playerUuid] = Pair(clanId, memberModel)
            }
        }

        /**
         * Получает всех участников клана по его имени
         *
         * @param clanName имя клана
         * @return список моделей участников клана или пустой набор, если клан не найден
         */
        fun members(clanName: String): Set<ClanMemberModel> {
            // Сначала получаем клан по имени
            val clan = get(clanName)

            // Если клан найден, возвращаем его участников
            if (clan != null) {
                return clan.members
            }

            // Если клан не найден, возвращаем пустой набор
            return emptySet()
        }
    }

    /** Объект для работы с очками кланов */
    object Scores {
        /** Ключ - UUID игрока, значение - модель очков */
        private val scoresCache = mutableMapOf<UUID, ClanScoreModel>()
        
        /** Инициализирует кэш очков игроков */
        fun init() {
            database.from(ClanScoresTable).select().map { row ->
                val playerUuid = row[ClanScoresTable.playerUuid]!!
                val scoreModel = ClanScoreModel(
                    playerUuid = playerUuid,
                    playerName = row[ClanScoresTable.playerName]!!,
                    scores = row[ClanScoresTable.scores] ?: emptyMap()
                )
                scoresCache[playerUuid] = scoreModel
            }
        }
        
        /**
         * Получает очки игрока
         *
         * @param playerUuid UUID игрока
         * @return модель очков или null, если у игрока нет очков
         */
        fun getPlayerScores(playerUuid: UUID): ClanScoreModel? {
            // Проверяем кэш
            if (scoresCache.containsKey(playerUuid)) {
                return scoresCache[playerUuid]
            }
            
            // Если нет в кэше, пробуем загрузить из БД
            return database.from(ClanScoresTable)
                .select()
                .where { ClanScoresTable.playerUuid eq playerUuid }
                .map { row ->
                    val scoreModel = ClanScoreModel(
                        playerUuid = playerUuid,
                        playerName = row[ClanScoresTable.playerName]!!,
                        scores = row[ClanScoresTable.scores] ?: emptyMap()
                    )
                    scoresCache[playerUuid] = scoreModel
                    scoreModel
                }
                .firstOrNull()
        }
        
        /**
         * Получает очки игрока для определенного типа
         *
         * @param playerUuid UUID игрока
         * @param scoreType тип очков
         * @return количество очков или 0, если у игрока нет очков данного типа
         */
        fun getPlayerScore(playerUuid: UUID, scoreType: String): Int {
            val scoreModel = getPlayerScores(playerUuid)
            return scoreModel?.scores?.get(scoreType) ?: 0
        }
        
        /**
         * Получает все очки игроков клана
         *
         * @param clanId ID клана
         * @return список моделей очков игроков клана
         */
        fun getClanMembersScores(clanId: UUID): List<ClanScoreModel> {
            val clan = get(clanId) ?: return emptyList()
            return clan.members.mapNotNull { member -> 
                getPlayerScores(member.uuid)
            }
        }
        
        /**
         * Получает топ игроков клана по определенному типу очков
         *
         * @param clanId ID клана
         * @param scoreType тип очков
         * @param limit максимальное количество игроков в топе
         * @return список пар (имя игрока, количество очков)
         */
        fun getClanTopByScoreType(clanId: UUID, scoreType: String, limit: Int = 10): List<Pair<String, Int>> {
            val scores = getClanMembersScores(clanId)
            return scores
                .map { it.playerName to (it.scores[scoreType] ?: 0) }
                .sortedByDescending { it.second }
                .take(limit)
        }
        
        /**
         * Рассчитывает общее количество очков клана по определенному типу
         *
         * @param clanId ID клана
         * @param scoreType тип очков (если null, то суммируются все типы)
         * @return общее количество очков клана
         */
        fun calculateClanScore(clanId: UUID, scoreType: String? = null): Long {
            val clan = get(clanId) ?: return 0
            val members = clan.members
            
            var totalScore = 0L
            members.forEach { member ->
                val scoreModel = getPlayerScores(member.uuid)
                if (scoreModel != null) {
                    if (scoreType != null) {
                        totalScore += (scoreModel.scores[scoreType] ?: 0).toLong()
                    } else {
                        totalScore += scoreModel.scores.values.sum().toLong()
                    }
                }
            }
            
            return totalScore
        }
        
        /**
         * Добавляет очки игроку (создает запись, если её нет)
         *
         * @param playerUuid UUID игрока
         * @param playerName имя игрока
         * @param scoreType тип очков
         * @param amount количество очков
         */
        fun addScore(playerUuid: UUID, playerName: String, scoreType: String, amount: Int) {
            val currentScores = getPlayerScores(playerUuid)
            
            if (currentScores == null) {
                // Создаем новую запись
                val scores = mapOf(scoreType to amount)
                
                database.insert(ClanScoresTable) {
                    set(it.playerUuid, playerUuid)
                    set(it.playerName, playerName)
                    set(it.scores, scores)
                }
                
                // Обновляем кэш
                scoresCache[playerUuid] = ClanScoreModel(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    scores = scores
                )
            } else {
                // Обновляем существующую запись
                val newScores = currentScores.scores.toMutableMap()
                newScores[scoreType] = (newScores[scoreType] ?: 0) + amount
                
                database.update(ClanScoresTable) {
                    set(it.playerName, playerName) // Обновляем имя на случай, если оно изменилось
                    set(it.scores, newScores)
                    where { it.playerUuid eq playerUuid }
                }
                
                // Обновляем кэш
                scoresCache[playerUuid] = currentScores.copy(
                    playerName = playerName,
                    scores = newScores
                )
            }
        }
        
        /**
         * Вычитает очки у игрока (создает запись с нулевым значением, если её нет)
         *
         * @param playerUuid UUID игрока
         * @param playerName имя игрока
         * @param scoreType тип очков
         * @param amount количество очков для вычитания
         */
        fun removeScore(playerUuid: UUID, playerName: String, scoreType: String, amount: Int) {
            val currentScores = getPlayerScores(playerUuid)
            
            if (currentScores == null) {
                // Создаем новую запись с нулевым значением
                val scores = mapOf(scoreType to 0)
                
                database.insert(ClanScoresTable) {
                    set(it.playerUuid, playerUuid)
                    set(it.playerName, playerName)
                    set(it.scores, scores)
                }
                
                // Обновляем кэш
                scoresCache[playerUuid] = ClanScoreModel(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    scores = scores
                )
            } else {
                // Обновляем существующую запись
                val newScores = currentScores.scores.toMutableMap()
                val currentValue = newScores[scoreType] ?: 0
                newScores[scoreType] = maxOf(0, currentValue - amount) // Не допускаем отрицательных значений
                
                database.update(ClanScoresTable) {
                    set(it.playerName, playerName) // Обновляем имя на случай, если оно изменилось
                    set(it.scores, newScores)
                    where { it.playerUuid eq playerUuid }
                }
                
                // Обновляем кэш
                scoresCache[playerUuid] = currentScores.copy(
                    playerName = playerName,
                    scores = newScores
                )
            }
        }
        
        /**
         * Устанавливает очки игроку (создает запись, если её нет)
         *
         * @param playerUuid UUID игрока
         * @param playerName имя игрока
         * @param scoreType тип очков
         * @param amount количество очков
         */
        fun setScore(playerUuid: UUID, playerName: String, scoreType: String, amount: Int) {
            val currentScores = getPlayerScores(playerUuid)
            
            if (currentScores == null) {
                // Создаем новую запись
                val scores = mapOf(scoreType to amount)
                
                database.insert(ClanScoresTable) {
                    set(it.playerUuid, playerUuid)
                    set(it.playerName, playerName)
                    set(it.scores, scores)
                }
                
                // Обновляем кэш
                scoresCache[playerUuid] = ClanScoreModel(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    scores = scores
                )
            } else {
                // Обновляем существующую запись
                val newScores = currentScores.scores.toMutableMap()
                newScores[scoreType] = amount
                
                database.update(ClanScoresTable) {
                    set(it.playerName, playerName)
                    set(it.scores, newScores)
                    where { it.playerUuid eq playerUuid }
                }
                
                // Обновляем кэш
                scoresCache[playerUuid] = currentScores.copy(
                    playerName = playerName,
                    scores = newScores
                )
            }
        }
        
        /**
         * Сбрасывает очки игрока определенного типа
         *
         * @param playerUuid UUID игрока
         * @param scoreType тип очков
         * @return true, если очки успешно сброшены
         */
        fun resetScore(playerUuid: UUID, scoreType: String): Boolean {
            val currentScores = getPlayerScores(playerUuid) ?: return false
            
            // Обновляем существующую запись
            val newScores = currentScores.scores.toMutableMap()
            newScores.remove(scoreType)
            
            database.update(ClanScoresTable) {
                set(it.scores, newScores)
                where { it.playerUuid eq playerUuid }
            }
            
            // Обновляем кэш
            scoresCache[playerUuid] = currentScores.copy(scores = newScores)
            
            return true
        }
        
        /**
         * Сбрасывает все очки игрока
         *
         * @param playerUuid UUID игрока
         * @return true, если очки успешно сброшены
         */
        fun resetAllScores(playerUuid: UUID): Boolean {
            val result = database.delete(ClanScoresTable) { it.playerUuid eq playerUuid } > 0
            
            // Обновляем кэш
            if (result) {
                scoresCache.remove(playerUuid)
            }
            
            return result
        }
    }

    /**
     * Конвертирует строку из таблицы в модель клана
     *
     * @param row строка из таблицы кланов
     * @return модель клана
     */
    private fun convertRowToClanModel(row: QueryRowSet): ClanModel {
        val clanId = row[ClansTable.id] ?: throw IllegalStateException("Clan ID cannot be null")

        // Получаем участников клана
        val members =
                database.from(ClanMembersTable)
                        .select()
                        .where { ClanMembersTable.clanId eq clanId }
                        .map { memberRow ->
                            val uuid = memberRow[ClanMembersTable.uuid] ?: return@map null
                            val name = memberRow[ClanMembersTable.name] ?: return@map null
                            val role = memberRow[ClanMembersTable.role] ?: return@map null
                            val joinedAt = memberRow[ClanMembersTable.joinedAt] ?: return@map null

                            ClanMemberModel(
                                    uuid = uuid,
                                    name = name,
                                    role = role,
                                    joinedAt = joinedAt
                            )
                        }
                        .filterNotNull()
                        .toMutableSet()

        // Создаем список новостей
        val newsList = FixedSizeList<String>(10)
        val news = row[ClansTable.news] ?: "[]"

        // Парсим JSON строку с новостями
        val newsJson = news.trim()
        if (newsJson.startsWith("[") && newsJson.endsWith("]")) {
            val newsItems =
                    newsJson.substring(1, newsJson.length - 1)
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .map {
                                if (it.startsWith("\"") && it.endsWith("\"")) {
                                    it.substring(1, it.length - 1)
                                } else {
                                    it
                                }
                            }

            newsItems.forEach { newsItem -> newsList.add(newsItem) }
        }

        return ClanModel(
                id = clanId,
                name = row[ClansTable.name] ?: "",
                colorlessName = row[ClansTable.colorlessName] ?: "",
                treasury = row[ClansTable.treasury] ?: 0,
                score = row[ClansTable.score] ?: 0,
                news = newsList,
                motd = row[ClansTable.motd] ?: "",
                creator = row[ClansTable.creator] ?: clanId,
                owner = row[ClansTable.owner] ?: clanId,
                members = members,
                createdAt = row[ClansTable.createdAt] ?: LocalDateTime.now(),
                slots = row[ClansTable.slots] ?: ClanSlots.INITIAL,
                chatPurchased = row[ClansTable.chatPurchased] ?: false,
                motdPurchased = row[ClansTable.motdPurchased] ?: false,
                partyPurchased = row[ClansTable.partyPurchased] ?: false
        )
    }
}
