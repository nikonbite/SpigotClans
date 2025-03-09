package org.mmarket.clans.system.manager

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mmarket.clans.api.utility.FixedSizeList
import org.mmarket.clans.system.model.ClanMemberModel
import org.mmarket.clans.system.model.ClanMemberRole
import org.mmarket.clans.system.model.ClanModel
import org.mmarket.clans.system.table.ClanInvitesTable
import org.mmarket.clans.system.table.ClanMembersTable
import org.mmarket.clans.system.table.ClansTable
import java.time.LocalDateTime
import java.util.*

/**
 * Менеджер для работы с кланами.
 * Обеспечивает взаимодействие между моделями кланов и таблицами базы данных.
 */
object ClanManager {
    private val clansCache = mutableMapOf<UUID, ClanModel>()

    /**
     * Загружает все кланы из базы данных в кэш
     */
    fun init() {
        transaction {
            ClansTable.selectAll().forEach { row: ResultRow ->
                val clanId = row[ClansTable.id]
                val clan = convertRowToClanModel(row)
                clansCache[clanId] = clan
            }
        }
        
        // Инициализируем кэш участников
        Members.init()
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
        return transaction {
            ClansTable.select(ClansTable.id eq id)
                .firstOrNull()
                ?.let { row: ResultRow ->
                    val clan = convertRowToClanModel(row)
                    clansCache[id] = clan
                    clan
                }
        }
    }

    /**
     * Получает клан по его имени
     *
     * @param name имя клана
     * @return модель клана или null, если клан не найден
     */
    fun get(name: String): ClanModel? {
        // Проверяем кэш
        val cachedClan = clansCache.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (cachedClan != null) {
            return cachedClan
        }

        // Если нет в кэше, пробуем загрузить из БД
        return transaction {
            ClansTable.select(ClansTable.colorlessName eq name)
                .firstOrNull()
                ?.let { row: ResultRow ->
                    val clan = convertRowToClanModel(row)
                    clansCache[clan.id] = clan
                    clan
                }
        }
    }

    /**
     * Создает новый клан
     *
     * @param name имя клана
     * @param colorlessName имя клана без цветовых кодов
     * @param creatorUuid UUID создателя клана
     * @return созданная модель клана
     */
    fun create(name: String, colorlessName: String, creatorUuid: UUID): ClanModel {
        val now = LocalDateTime.now()
        val newClanId = UUID.randomUUID()

        val clan = transaction {
            // Создаем запись клана
            ClansTable.insert {
                it[id] = newClanId
                it[ClansTable.name] = name
                it[ClansTable.colorlessName] = colorlessName
                it[treasury] = 0
                it[news] = emptyList()
                it[motd] = ""
                it[creator] = creatorUuid
                it[owner] = creatorUuid
                it[createdAt] = now
            }

            // Создаем запись владельца клана
            ClanMembersTable.insert {
                it[clanId] = newClanId
                it[uuid] = creatorUuid
                it[role] = ClanMemberRole.ADMIRAL
                it[joinedAt] = now
            }

            // Создаем модель клана
            val ownerModel = ClanMemberModel(
                uuid = creatorUuid,
                role = ClanMemberRole.ADMIRAL,
                joinedAt = now
            )

            ClanModel(
                id = newClanId,
                name = name,
                colorlessName = colorlessName,
                treasury = 0,
                news = FixedSizeList<String>(10),
                motd = "",
                creator = creatorUuid,
                owner = creatorUuid,
                members = mutableSetOf(ownerModel),
                createdAt = now
            )
        }

        // Добавляем в кэш
        clansCache[newClanId] = clan
        return clan
    }

    /**
     * Удаляет клан
     *
     * @param id ID клана
     * @return true, если клан успешно удален
     */
    fun delete(id: UUID): Boolean {
        return transaction {
            val result = ClansTable.deleteWhere { ClansTable.id eq id } > 0
            if (result) {
                clansCache.remove(id)
            }
            result
        }
    }

    /**
     * Обновляет данные клана в БД
     *
     * @param clan модель клана для обновления
     */
    fun update(clan: ClanModel) {
        transaction {
            ClansTable.update({ ClansTable.id eq clan.id }) {
                it[name] = clan.name
                it[colorlessName] = clan.colorlessName
                it[treasury] = clan.treasury
                it[news] = clan.news.toList()
                it[motd] = clan.motd
                it[owner] = clan.owner
            }

            // Обновляем кэш
            clansCache[clan.id] = clan
        }
    }

    /**
     * Добавляет участника в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @param role роль участника
     * @return true, если участник успешно добавлен
     */
    fun addMember(clanId: UUID, playerUuid: UUID, role: ClanMemberRole = ClanMemberRole.RECRUIT): Boolean {
        val now = LocalDateTime.now()

        return transaction {
            // Проверяем, существует ли клан
            val clanExists = ClansTable.select(ClansTable.id eq clanId).count() > 0
            if (!clanExists) return@transaction false

            // Проверяем, не состоит ли игрок уже в клане
            val alreadyInClan = ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid).count() > 0

            if (alreadyInClan) return@transaction false

            // Добавляем участника
            ClanMembersTable.insert {
                it[ClanMembersTable.clanId] = clanId
                it[uuid] = playerUuid
                it[ClanMembersTable.role] = role
                it[joinedAt] = now
            }

            // Обновляем кэш
            val clan = clansCache[clanId]
            if (clan != null) {
                val memberModel = ClanMemberModel(
                    uuid = playerUuid,
                    role = role,
                    joinedAt = now
                )
                clan.members.add(memberModel)
                
                // Обновляем кэш участников
                Members.updateCacheOnAdd(clanId, memberModel)
            }

            true
        }
    }

    /**
     * Удаляет участника из клана
     *
     * @param playerUuid UUID игрока
     * @return true, если участник успешно удален
     */
    fun removeMember(playerUuid: UUID): Boolean {
        return transaction {
            // Находим запись участника клана
            val memberRow = ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid).firstOrNull()

            if (memberRow == null) return@transaction false

            val clanId = memberRow[ClanMembersTable.clanId]

            // Удаляем участника
            val result = ClanMembersTable.deleteWhere { uuid eq playerUuid } > 0

            // Обновляем кэш
            if (result) {
                val clan = clansCache[clanId]
                clan?.members?.removeIf { it.uuid == playerUuid }
                
                // Обновляем кэш участников
                Members.updateCacheOnRemove(playerUuid)
            }

            result
        }
    }

    /**
     * Обновляет роль участника клана
     *
     * @param playerUuid UUID игрока
     * @param newRole новая роль
     * @return true, если роль успешно обновлена
     */
    fun updateMemberRole(playerUuid: UUID, newRole: ClanMemberRole): Boolean {
        return transaction {
            // Находим запись участника клана
            val memberRow = ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid).firstOrNull()

            if (memberRow == null) return@transaction false

            val clanId = memberRow[ClanMembersTable.clanId]

            // Обновляем роль
            val result = ClanMembersTable.update({
                ClanMembersTable.uuid eq playerUuid
            }) {
                it[role] = newRole
            } > 0

            // Обновляем кэш
            if (result) {
                val clan = clansCache[clanId]
                clan?.members?.find { it.uuid == playerUuid }?.role = newRole
                
                // Обновляем кэш участников
                Members.updateCacheOnRoleChange(playerUuid, newRole)
            }

            result
        }
    }

    /**
     * Создает приглашение в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @return true, если приглашение успешно создано
     */
    fun createInvite(clanId: UUID, playerUuid: UUID): Boolean {
        val now = LocalDateTime.now()

        return transaction {
            // Проверяем, существует ли клан
            val clanExists = ClansTable.select(ClansTable.id eq clanId).count() > 0
            if (!clanExists) return@transaction false

            // Проверяем, не состоит ли игрок уже в клане
            val alreadyInClan = ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid).count() > 0

            if (alreadyInClan) return@transaction false

            // Проверяем, нет ли уже приглашения
            val inviteExists =
                ClanInvitesTable.select((ClanInvitesTable.clanId eq clanId) and (ClanInvitesTable.uuid eq playerUuid))
                    .count() > 0

            if (inviteExists) return@transaction false

            // Создаем приглашение
            ClanInvitesTable.insert {
                it[ClanInvitesTable.clanId] = clanId
                it[uuid] = playerUuid
                it[createdAt] = now
            }

            true
        }
    }

    /**
     * Удаляет приглашение в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @return true, если приглашение успешно удалено
     */
    fun removeInvite(clanId: UUID, playerUuid: UUID): Boolean {
        return transaction {
            ClanInvitesTable.deleteWhere {
                (ClanInvitesTable.clanId eq clanId) and (uuid eq playerUuid)
            } > 0
        }
    }

    /**
     * Проверяет, есть ли у игрока приглашение в клан
     *
     * @param clanId ID клана
     * @param playerUuid UUID игрока
     * @return true, если приглашение существует
     */
    fun hasInvite(clanId: UUID, playerUuid: UUID): Boolean {
        return transaction {
            ClanInvitesTable.select((ClanInvitesTable.clanId eq clanId) and (ClanInvitesTable.uuid eq playerUuid))
                .count() > 0
        }
    }

    /**
     * Получает список всех приглашений игрока
     *
     * @param playerUuid UUID игрока
     * @return список ID кланов, в которые приглашен игрок
     */
    fun getPlayerInvites(playerUuid: UUID): List<UUID> {
        return transaction {
            ClanInvitesTable.select(ClanInvitesTable.uuid eq playerUuid)
                .map { it[ClanInvitesTable.clanId] }
        }
    }

    object Members {
        /** Ключ - UUID игрока, значение - пара (ID клана, модель участника) */
        private val membersCache = mutableMapOf<UUID, Pair<UUID, ClanMemberModel>>()

        /**
         * Инициализирует кэш участников кланов
         */
        fun init() {
            transaction {
                ClanMembersTable.selectAll().forEach { row: ResultRow ->
                    val playerUuid = row[ClanMembersTable.uuid]
                    val clanId = row[ClanMembersTable.clanId]
                    val memberModel = ClanMemberModel(
                        uuid = playerUuid,
                        role = row[ClanMembersTable.role],
                        joinedAt = row[ClanMembersTable.joinedAt]
                    )
                    membersCache[playerUuid] = Pair(clanId, memberModel)
                }
            }
        }

        /**
         * Получает клан, в котором состоит игрок
         *
         * @param playerUuid UUID игрока
         * @return модель клана или null, если игрок не состоит в клане
         */
        fun playerClan(playerUuid: UUID): ClanModel? {
            // Проверяем кэш участников
            val memberData = membersCache[playerUuid]
            if (memberData != null) {
                val clanId = memberData.first
                return clansCache[clanId]
            }

            // Если нет в кэше, пробуем загрузить из БД
            return transaction {
                val memberRow = ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid).firstOrNull()
                    ?: return@transaction null

                val clanId = memberRow[ClanMembersTable.clanId]
                val memberModel = ClanMemberModel(
                    uuid = playerUuid,
                    role = memberRow[ClanMembersTable.role],
                    joinedAt = memberRow[ClanMembersTable.joinedAt]
                )

                // Добавляем в кэш
                membersCache[playerUuid] = Pair(clanId, memberModel)

                // Возвращаем клан
                clansCache[clanId] ?: get(clanId)
            }
        }

        /**
         * Проверяет, состоит ли игрок в клане
         *
         * @param playerUuid UUID игрока
         * @return true, если игрок состоит в клане
         */
        fun inClan(playerUuid: UUID): Boolean {
            return membersCache.containsKey(playerUuid) || transaction {
                ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid).count() > 0
            }
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
            return transaction {
                ClanMembersTable.select(
                    (ClanMembersTable.clanId eq clanId) and (ClanMembersTable.uuid eq playerUuid)
                ).count() > 0
            }
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
            return transaction {
                ClanMembersTable.select(ClanMembersTable.uuid eq playerUuid)
                    .firstOrNull()
                    ?.let { row ->
                        val clanId = row[ClanMembersTable.clanId]
                        val memberModel = ClanMemberModel(
                            uuid = playerUuid,
                            role = row[ClanMembersTable.role],
                            joinedAt = row[ClanMembersTable.joinedAt]
                        )
                        
                        // Добавляем в кэш
                        membersCache[playerUuid] = Pair(clanId, memberModel)
                        
                        memberModel.role
                    }
            }
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
            return transaction {
                ClanMembersTable.select(ClanMembersTable.clanId eq clanId)
                    .map { row ->
                        val playerUuid = row[ClanMembersTable.uuid]
                        val memberModel = ClanMemberModel(
                            uuid = playerUuid,
                            role = row[ClanMembersTable.role],
                            joinedAt = row[ClanMembersTable.joinedAt]
                        )
                        
                        // Добавляем в кэш участников
                        membersCache[playerUuid] = Pair(clanId, memberModel)
                        
                        memberModel
                    }
                    .toSet()
            }
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
    private fun convertRowToClanModel(row: ResultRow): ClanModel {
        val clanId = row[ClansTable.id]

        // Получаем участников клана
        val members = transaction {
            ClanMembersTable.select(ClanMembersTable.clanId eq clanId)
                .map { memberRow: ResultRow ->
                    ClanMemberModel(
                        uuid = memberRow[ClanMembersTable.uuid],
                        role = memberRow[ClanMembersTable.role],
                        joinedAt = memberRow[ClanMembersTable.joinedAt]
                    )
                }
                .toMutableSet()
        }

        // Создаем список новостей
        val newsList = FixedSizeList<String>(10)
        row[ClansTable.news].forEach { newsItem: String -> newsList.add(newsItem) }

        return ClanModel(
            id = clanId,
            name = row[ClansTable.name],
            colorlessName = row[ClansTable.colorlessName],
            treasury = row[ClansTable.treasury],
            news = newsList,
            motd = row[ClansTable.motd],
            creator = row[ClansTable.creator],
            owner = row[ClansTable.owner],
            members = members,
            createdAt = row[ClansTable.createdAt]
        )
    }
}