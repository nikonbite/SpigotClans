package org.mmarket.clans.system.manager

import java.time.LocalDateTime
import java.util.UUID
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.mmarket.clans.api.utility.FixedSizeList
import org.mmarket.clans.system.model.ClanMemberModel
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.system.model.ClanModel
import org.mmarket.clans.system.table.ClanInvitesTable
import org.mmarket.clans.system.table.ClanMembersTable
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
        val clans =
                database.from(ClansTable).select().map { row ->
                    val clanId = row[ClansTable.id]!!
                    val clan = convertRowToClanModel(row)
                    clansCache[clanId] = clan
                    clan
                }

        // Инициализируем кэш участников
        Members.init()
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
                        createdAt = now
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
        database.update(ClansTable) {
            set(it.treasury, it.treasury + amount)
            where { it.id eq clanId }
        }
    }

    /**
     * Вычитает сумму из бюджета клана
     *
     * @param clanId ID клана
     * @param amount сумма
     */
    fun subtractTreasury(clanId: UUID, amount: Long) {
        database.update(ClansTable) {
            set(it.treasury, it.treasury - amount)
            where { it.id eq clanId }
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
    fun createInvite(clanId: UUID, playerUuid: UUID): Boolean {
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

        // Проверяем, нет ли уже приглашения
        val alreadyInvited =
                database.from(ClanInvitesTable)
                        .select(count(ClanInvitesTable.id))
                        .where {
                            (ClanInvitesTable.clanId eq clanId) and
                                    (ClanInvitesTable.uuid eq playerUuid)
                        }
                        .map { it.getInt(1) }
                        .first() > 0

        if (alreadyInvited) return false

        // Создаем приглашение
        database.insert(ClanInvitesTable) {
            set(it.clanId, clanId)
            set(it.uuid, playerUuid)
            set(it.createdAt, LocalDateTime.now())
        }

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
        return database.delete(ClanInvitesTable) {
            (it.clanId eq clanId) and (it.uuid eq playerUuid)
        } > 0
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

        // Удаляем клан (каскадно удалятся участники и приглашения)
        val result = database.delete(ClansTable) { it.id eq clanId } > 0

        // Обновляем кэш
        if (result) {
            clansCache.remove(clanId)

            // Обновляем кэш участников
            members.forEach { Members.updateCacheOnRemove(it) }
        }

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
        return database.from(ClanInvitesTable)
                .select(count(ClanInvitesTable.id))
                .where {
                    (ClanInvitesTable.clanId eq clanId) and (ClanInvitesTable.uuid eq playerUuid)
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
        return database.from(ClanInvitesTable)
                .select(ClanInvitesTable.clanId)
                .where { ClanInvitesTable.uuid eq playerUuid }
                .map { it[ClanInvitesTable.clanId]!! }
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
                createdAt = row[ClansTable.createdAt] ?: LocalDateTime.now()
        )
    }
}
