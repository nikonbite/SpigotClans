package org.mmarket.clans.hook

import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.npc.NPCRegistry
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.mmarket.clans.Clans

object CitizensHook {
    private lateinit var npcRegistry: NPCRegistry
    private var initialized = false

    /**
     * Инициализация CitizensAPI
     * @return true если инициализация успешна
     */
    fun init(): Boolean {
        val clans = Clans.instance

        npcRegistry = CitizensAPI.getNPCRegistry()
        initialized = true

        clans.pluginManager.registerEvents(object : Listener {
            @EventHandler
            fun on(event: NPCRightClickEvent) {
                val npc = event.npc
                if (npc.id == 123) {
                    event.clicker.sendMessage("Вы взаимодействовали с ${npc.name}")
                }
            }
        }, clans.plugin)

        clans.logger.info("( Citizens ) :: Слушатель взаимодействий инициализирован.")
        return true
    }

    /**
     * Создание NPC
     * @param location Локация спавна
     * @param name Имя NPC
     * @param entityType Тип сущности (по умолчанию PLAYER)
     * @return Созданный NPC или null при ошибке
     */
    fun create(
        location: Location,
        name: String,
        entityType: EntityType = EntityType.PLAYER
    ): NPC? {
        if (!initialized) return null

        val npc = npcRegistry.createNPC(entityType, name)
        npc.spawn(location)
        return npc
    }

    /**
     * Удаление NPC по ID
     * @param npcId ID NPC
     * @return true если удаление успешно
     */
    fun remove(npcId: Int): Boolean {
        val npc = npcRegistry.getById(npcId) ?: return false
        npc.destroy()
        return true
    }

    /**
     * Изменение имени NPC
     * @param npcId ID NPC
     * @param newName Новое имя
     */
    fun rename(npcId: Int, newName: String) {
        val npc = npcRegistry.getById(npcId) ?: return
        npc.name = newName
    }

    /**
     * Телепортация NPC
     * @param npcId ID NPC
     * @param location Новая локация
     */
    fun teleport(npcId: Int, location: Location) {
        val npc = npcRegistry.getById(npcId) ?: return
        npc.teleport(location, null)
    }

    /**
     * Получение NPC по ID
     */
    fun get(npcId: Int): NPC? = npcRegistry.getById(npcId)
}