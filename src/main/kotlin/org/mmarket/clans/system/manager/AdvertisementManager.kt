package org.mmarket.clans.system.manager

import java.time.LocalDateTime
import java.util.UUID
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.mmarket.clans.system.model.ClanAdvertisementJoinType
import org.mmarket.clans.system.model.ClanAdvertisementModel
import org.mmarket.clans.system.model.ClanAdvertisementTariff
import org.mmarket.clans.system.table.ClanAdvertisementsTable

/**
 * Менеджер для работы с рекламой кланов.
 * Обеспечивает создание, получение и удаление рекламы с истекшим сроком.
 */
object AdvertisementManager {
    private val advertisementsCache = mutableMapOf<UUID, MutableList<ClanAdvertisementModel>>()
    private lateinit var database: Database

    /**
     * Инициализирует менеджер рекламы
     * @param db База данных
     */
    fun init(db: Database) {
        database = db
        loadAllAdvertisements()
        
        // Запускаем задачу для удаления истекшей рекламы
        scheduleCleanupTask()
    }
    
    /**
     * Загружает все рекламные объявления из базы данных
     */
    private fun loadAllAdvertisements() {
        advertisementsCache.clear()
        
        database.from(ClanAdvertisementsTable).select().map { row ->
            val clanId = row[ClanAdvertisementsTable.clanId]!!
            val advertisement = convertRowToAdvertisementModel(row)
            
            if (!advertisementsCache.containsKey(clanId)) {
                advertisementsCache[clanId] = mutableListOf()
            }
            
            advertisementsCache[clanId]?.add(advertisement)
        }
    }
    
    /**
     * Преобразует строку из базы данных в модель рекламы
     * @param row Строка из базы данных
     * @return Модель рекламы
     */
    private fun convertRowToAdvertisementModel(row: QueryRowSet): ClanAdvertisementModel {
        return ClanAdvertisementModel(
            clanId = row[ClanAdvertisementsTable.clanId]!!,
            joinType = row[ClanAdvertisementsTable.joinType]!!,
            tariff = row[ClanAdvertisementsTable.tariff]!!,
            lines = row[ClanAdvertisementsTable.lines]!!,
            createdAt = row[ClanAdvertisementsTable.createdAt]!!,
            expiresAt = row[ClanAdvertisementsTable.expiresAt]!!
        )
    }
    
    /**
     * Планирует задачу для удаления истекшей рекламы
     */
    private fun scheduleCleanupTask() {
        // Запускаем задачу каждый час для проверки и удаления истекшей рекламы
        val cleanupTask = object : Runnable {
            override fun run() {
                removeExpiredAdvertisements()
            }
        }
        
        // Запускаем задачу через планировщик сервера
        // Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, cleanupTask, 20 * 60 * 60, 20 * 60 * 60)
        // TODO: Реализовать запуск задачи через планировщик сервера
    }
    
    /**
     * Создает новую рекламу для клана
     * @param clanId ID клана
     * @param joinType Тип присоединения (INVITE или OPEN)
     * @param tariff Тариф рекламы
     * @param lines Текст рекламы
     * @return true если реклама успешно создана, false в противном случае
     */
    fun createAdvertisement(clanId: UUID, joinType: ClanAdvertisementJoinType, 
                           tariff: ClanAdvertisementTariff, lines: String): Boolean {
        val now = LocalDateTime.now()
        val expiresAt = now.plusHours(tariff.hours.toLong())
        
        // Проверяем, есть ли уже активная реклама для этого клана
        if (hasActiveAdvertisement(clanId)) {
            return false
        }
        
        // Создаем запись в базе данных
        val insertResult = database.insert(ClanAdvertisementsTable) {
            set(it.clanId, clanId)
            set(it.joinType, joinType)
            set(it.tariff, tariff)
            set(it.lines, lines)
            set(it.createdAt, now)
            set(it.expiresAt, expiresAt)
        }
        
        if (insertResult > 0) {
            // Получаем созданную рекламу из базы данных
            val advertisement = database.from(ClanAdvertisementsTable)
                .select()
                .where { ClanAdvertisementsTable.clanId eq clanId }
                .orderBy(ClanAdvertisementsTable.createdAt.desc())
                .limit(1)
                .map { convertRowToAdvertisementModel(it) }
                .firstOrNull() ?: return false
            
            // Добавляем в кэш
            if (!advertisementsCache.containsKey(clanId)) {
                advertisementsCache[clanId] = mutableListOf()
            }
            advertisementsCache[clanId]?.add(advertisement)
            
            return true
        }
        
        return false
    }
    
    /**
     * Проверяет, есть ли активная реклама для клана
     * @param clanId ID клана
     * @return true если есть активная реклама, false в противном случае
     */
    fun hasActiveAdvertisement(clanId: UUID): Boolean {
        val now = LocalDateTime.now()
        return advertisementsCache[clanId]?.any { it.expiresAt.isAfter(now) } ?: false
    }
    
    /**
     * Получает активную рекламу для клана
     * @param clanId ID клана
     * @return Модель рекламы или null, если активной рекламы нет
     */
    fun getActiveAdvertisement(clanId: UUID): ClanAdvertisementModel? {
        val now = LocalDateTime.now()
        return advertisementsCache[clanId]?.firstOrNull { it.expiresAt.isAfter(now) }
    }
    
    /**
     * Получает все активные рекламные объявления
     * @return Список активных рекламных объявлений
     */
    fun getAllActiveAdvertisements(): List<ClanAdvertisementModel> {
        val now = LocalDateTime.now()
        val result = mutableListOf<ClanAdvertisementModel>()
        
        advertisementsCache.values.forEach { advertisements ->
            advertisements.filter { it.expiresAt.isAfter(now) }.forEach { result.add(it) }
        }
        
        return result
    }
    
    /**
     * Удаляет рекламу для клана
     * @param clanId ID клана
     * @return true если реклама успешно удалена, false в противном случае
     */
    fun removeAdvertisement(clanId: UUID): Boolean {
        val deleteResult = database.delete(ClanAdvertisementsTable) {
            it.clanId eq clanId
        }
        
        if (deleteResult > 0) {
            advertisementsCache.remove(clanId)
            return true
        }
        
        return false
    }
    
    /**
     * Удаляет все истекшие рекламные объявления
     */
    fun removeExpiredAdvertisements() {
        val now = LocalDateTime.now()
        
        // Удаляем из базы данных
        database.delete(ClanAdvertisementsTable) {
            it.expiresAt less now
        }
        
        // Удаляем из кэша
        val clansToRemove = mutableListOf<UUID>()
        
        advertisementsCache.forEach { (clanId, advertisements) ->
            advertisements.removeIf { it.expiresAt.isBefore(now) }
            if (advertisements.isEmpty()) {
                clansToRemove.add(clanId)
            }
        }
        
        clansToRemove.forEach { advertisementsCache.remove(it) }
    }
    
    /**
     * Обновляет существующую рекламу клана
     * @param clanId ID клана
     * @param joinType Новый тип присоединения
     * @return true если реклама успешно обновлена, false в противном случае
     */
    fun updateAdvertisement(clanId: UUID, joinType: ClanAdvertisementJoinType): Boolean {
        // Получаем текущую рекламу
        val currentAd = getActiveAdvertisement(clanId) ?: return false
        
        // Обновляем рекламу в базе данных
        val updateResult = database.update(ClanAdvertisementsTable) {
            set(it.joinType, joinType)
            where { 
                (it.clanId eq clanId) and (it.expiresAt greater LocalDateTime.now())
            }
        }
        
        if (updateResult > 0) {
            // Обновляем кэш
            advertisementsCache[clanId]?.forEach { ad ->
                if (ad.expiresAt.isAfter(LocalDateTime.now())) {
                    // Создаем новую модель с обновленным типом присоединения
                    val updatedAd = ClanAdvertisementModel(
                        clanId = ad.clanId,
                        joinType = joinType,
                        tariff = ad.tariff,
                        lines = ad.lines,
                        createdAt = ad.createdAt,
                        expiresAt = ad.expiresAt
                    )
                    
                    // Заменяем старую модель на новую
                    advertisementsCache[clanId]?.remove(ad)
                    advertisementsCache[clanId]?.add(updatedAd)
                }
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * Обновляет текст рекламы клана
     * @param clanId ID клана
     * @param lines Новый текст рекламы
     * @return true, если обновление прошло успешно
     */
    fun updateAdvertisementText(clanId: UUID, lines: String): Boolean {
        // Получаем текущую рекламу
        val currentAd = getActiveAdvertisement(clanId) ?: return false
        
        // Обновляем рекламу в базе данных
        val updateResult = database.update(ClanAdvertisementsTable) {
            set(it.lines, lines)
            where { 
                (it.clanId eq clanId) and (it.expiresAt greater LocalDateTime.now())
            }
        }
        
        if (updateResult > 0) {
            // Обновляем кэш
            advertisementsCache[clanId]?.forEach { ad ->
                if (ad.expiresAt.isAfter(LocalDateTime.now())) {
                    // Создаем новую модель с обновленным текстом
                    val updatedAd = ClanAdvertisementModel(
                        clanId = ad.clanId,
                        joinType = ad.joinType,
                        tariff = ad.tariff,
                        lines = lines,
                        createdAt = ad.createdAt,
                        expiresAt = ad.expiresAt
                    )
                    
                    // Заменяем старую модель на новую
                    advertisementsCache[clanId]?.remove(ad)
                    advertisementsCache[clanId]?.add(updatedAd)
                }
            }
            
            return true
        }
        
        return false
    }
} 