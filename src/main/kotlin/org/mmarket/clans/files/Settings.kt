package org.mmarket.clans.files

import org.mmarket.clans.Clans
import org.tomlj.TomlArray
import org.tomlj.TomlParseResult
import java.io.File

object Settings {
    private val plugin = Clans.instance.plugin
    private lateinit var settings: TomlParseResult

    fun init() {
        val messagesFile = File(plugin.dataFolder, "settings.toml")
        if (!messagesFile.exists()) {
            plugin.saveResource("settings.toml", false)
        }
        settings = org.tomlj.Toml.parse(messagesFile.toPath())
    }

    /**
     * Перезагружает настройки
     */
    fun reload() {
        val messagesFile = File(plugin.dataFolder, "settings.toml")
        if (!messagesFile.exists()) {
            plugin.saveResource("settings.toml", false)
        }
        settings = org.tomlj.Toml.parse(messagesFile.toPath())
    }

    fun string(key: String): String {
        return settings.getString(key) ?: run {
            plugin.logger.warning("Настройка с ключом $key не найдена!")
            return key
        }
    }

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

    /**
     * Получает список строк из массива в настройках
     * @param key ключ настройки
     * @return список строк или пустой список, если настройка не найдена
     */
    fun getStringList(key: String): List<String> {
        val array = array(key)
        val result = mutableListOf<String>()
        
        for (i in 0 until array.size()) {
            val value = array.getString(i)
            if (value != null) {
                result.add(value)
            }
        }
        
        return result
    }
}