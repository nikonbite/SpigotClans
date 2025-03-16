package org.mmarket.clans.files

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.ChatColor.translateAlternateColorCodes
import org.bukkit.entity.Player
import org.mmarket.clans.Clans
import org.tomlj.TomlArray
import org.tomlj.TomlParseResult
import java.io.File

object Interfaces {
    private val plugin = Clans.instance.plugin
    private lateinit var settings: TomlParseResult

    fun init() {
        val interfacesFile = File(plugin.dataFolder, "interfaces.toml")
        if (!interfacesFile.exists()) {
            plugin.saveResource("interfaces.toml", false)
        }
        settings = org.tomlj.Toml.parse(interfacesFile.toPath())
    }

    /**
     * Перезагружает настройки
     */
    fun reload() {
        val interfacesFile = File(plugin.dataFolder, "interfaces.toml")
        if (!interfacesFile.exists()) {
            plugin.saveResource("interfaces.toml", false)
        }
        settings = org.tomlj.Toml.parse(interfacesFile.toPath())
    }

    /**
     * Получает строку из настроек и заменяет в ней плейсхолдеры
     * @param key ключ настройки
     * @param replacements пары ключ-значение для замены в строке
     * @return отформатированная строка
     */
    fun string(key: String, replacements: Map<String, String> = emptyMap()): String {
        val value = settings.getString(key) ?: run {
            plugin.logger.warning("Настройка с ключом $key не найдена!")
            return key
        }

        var formattedValue = value

        // Обрабатываем все плейсхолдеры
        if (formattedValue.contains("{") && formattedValue.contains("}")) {
            val pattern = """\{([^}]+)}""".toRegex()
            formattedValue = formattedValue.replace(pattern) { matchResult ->
                val placeholder = matchResult.groupValues[1]
                // Сначала проверяем, есть ли значение в replacements
                replacements[placeholder] ?: run {
                    // Если нет в replacements, пробуем получить как вложенную настройку
                    val nestedValue = settings.getString(placeholder)
                    if (nestedValue != null) {
                        string(placeholder)
                    } else {
                        // Если не нашли нигде, оставляем как есть
                        "{$placeholder}"
                    }
                }
            }
        }

        // Конвертация цветовых кодов
        return format(formattedValue)
    }

    /**
     * Получает строку из настроек и заменяет в ней плейсхолдеры с поддержкой vararg
     * @param key ключ настройки
     * @param args пары ключ-значение для замены в строке
     * @return отформатированная строка
     */
    fun string(key: String, vararg args: Pair<String, String>): String {
        return string(key, args.toMap())
    }

    /**
     * Преобразует аргументы разных типов в строки и получает настройку
     * @param key ключ настройки
     * @param args пары ключ-значение разных типов для замены в строке
     * @return отформатированная строка
     */
    fun getWithArgs(key: String, vararg args: Pair<String, Any?>): String {
        val stringArgs = args.associate { (k, v) -> k to (v?.toString() ?: "null") }
        return string(key, stringArgs)
    }

    /**
     * Форматирует произвольную строку с цветовыми кодами
     * @param message строка для форматирования
     * @return отформатированная строка
     */
    fun format(message: String) = translateAlternateColorCodes('&', message)

    fun long(key: String): Long {
        return settings.getLong(key) ?: run {
            plugin.logger.warning("Настройка с ключом $key не найдена!")
            return -1
        }
    }

    fun double(key: String): Double {
        return settings.getDouble(key) ?: run {
            plugin.logger.warning("Настройка с ключом $key не найдена!")
            return -1.0
        }
    }

    fun boolean(key: String): Boolean {
        return settings.getBoolean(key) ?: run {
            plugin.logger.warning("Настройка с ключом $key не найдена!")
            return false
        }
    }

    fun array(key: String): TomlArray {
        return settings.getArrayOrEmpty(key)
    }

    fun Player.message(key: String, replacements: Map<String, String> = emptyMap()) {
        if (isOnline)
            sendMessage(PlaceholderAPI.setPlaceholders(this, string(key, replacements)))
    }
}