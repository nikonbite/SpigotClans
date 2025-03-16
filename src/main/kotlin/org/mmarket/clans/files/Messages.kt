package org.mmarket.clans.files

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.ChatColor.translateAlternateColorCodes
import org.bukkit.entity.Player
import org.mmarket.clans.Clans
import org.tomlj.TomlParseResult
import java.io.File

object Messages {
    private val plugin = Clans.instance.plugin
    private lateinit var messages: TomlParseResult

    fun init() {
        val messagesFile = File(plugin.dataFolder, "messages.toml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.toml", false)
        }
        messages = org.tomlj.Toml.parse(messagesFile.toPath())
    }

    /**
     * Перезагружает сообщения из файла
     */
    fun reload() {
        val messagesFile = File(plugin.dataFolder, "messages.toml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.toml", false)
        }
        messages = org.tomlj.Toml.parse(messagesFile.toPath())
    }

    /**
     * Получает сообщение и заменяет в нем плейсхолдеры
     * @param key ключ сообщения
     * @param replacements пары ключ-значение для замены в сообщении
     * @return отформатированное сообщение
     */
    fun get(key: String, replacements: Map<String, String> = emptyMap()): String {
        val message = messages.getString(key) ?: run {
            plugin.logger.warning("Сообщение с ключом $key не найдено!")
            return "Message '$key' not found"
        }

        var formattedMessage = message

        // Обрабатываем все плейсхолдеры
        if (formattedMessage.contains("{") && formattedMessage.contains("}")) {
            val pattern = """\{([^}]+)}""".toRegex()
            formattedMessage = formattedMessage.replace(pattern) { matchResult ->
                val placeholder = matchResult.groupValues[1]
                // Сначала проверяем, есть ли значение в replacements
                replacements[placeholder] ?: run {
                    // Если нет в replacements, пробуем получить как вложенное сообщение
                    val nestedMessage = messages.getString(placeholder)
                    if (nestedMessage != null) {
                        get(placeholder)
                    } else {
                        // Если не нашли нигде, оставляем как есть
                        "{$placeholder}"
                    }
                }
            }
        }

        // Конвертация цветовых кодов
        return format(formattedMessage)
    }

    /**
     * Получает сообщение и заменяет в нем плейсхолдеры с поддержкой vararg
     * @param key ключ сообщения
     * @param args пары ключ-значение для замены в сообщении
     * @param usePrefix добавлять ли префикс клана
     * @return отформатированное сообщение
     */
    fun get(key: String, vararg args: Pair<String, String>): String {
        return get(key, args.toMap())
    }

    /**
     * Преобразует аргументы разных типов в строки и получает сообщение
     * @param key ключ сообщения
     * @param args пары ключ-значение разных типов для замены в сообщении
     * @param usePrefix добавлять ли префикс клана
     * @return отформатированное сообщение
     */
    fun getWithArgs(key: String, vararg args: Pair<String, Any?>): String {
        val stringArgs = args.associate { (k, v) -> k to (v?.toString() ?: "null") }
        return get(key, stringArgs)
    }

    /**
     * Форматирует произвольную строку с цветовыми кодами
     * @param message сообщение для форматирования
     * @return отформатированное сообщение
     */
    fun format(message: String) = translateAlternateColorCodes('&', message)

    fun Player.message(key: String, replacements: Map<String, String> = emptyMap()) {
        if (isOnline)
            sendMessage(PlaceholderAPI.setPlaceholders(this, get(key, replacements)))
    }
}