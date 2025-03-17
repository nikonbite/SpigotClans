package org.mmarket.clans.api.interfaces

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.format.TextDecoration
/**
 * Утилиты для работы с интерфейсами
 */
object UiUtils {
    /**
     * Разделяет многострочный лор на список строк
     * @param lore Многострочный лор
     * @return Список строк лора
     */
    fun splitLore(lore: String): List<String> {
        return lore.split("\n")
    }
    
    /**
     * Разделяет многострочный лор на список компонентов
     * @param lore Многострочный лор
     * @return Список компонентов лора
     */
    fun splitLoreToComponents(lore: String): List<Component> {
        return splitLore(lore).map { LegacyComponentSerializer.legacySection().deserialize(it).decoration(TextDecoration.ITALIC, false) }
    }
    
    /**
     * Преобразует строку в компонент
     * @return Компонент из строки
     */
    fun String.toComponent(): Component {
        return LegacyComponentSerializer.legacySection().deserialize(this).decoration(TextDecoration.ITALIC, false)
    }
}