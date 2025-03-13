package org.mmarket.clans.api.utility

import org.mmarket.clans.files.Settings
import org.bukkit.ChatColor

object NamingUtility {

    // TODO: FIX!!!
    fun validateClanName(name: String?): Boolean {
        if (name.isNullOrBlank()) {
            return false
        }

        val colorlessName = removeColors(name)
        val minLength = Settings.long("restrictions.min_name_length")
        val maxLength = Settings.long("restrictions.max_name_length")
        
        if (colorlessName.length < minLength) {
            return false
        }
        
        if (colorlessName.length > maxLength) {
            return false
        }
        
        if (!"^[a-zA-Z0-9_\\-\\s]+$".toRegex().matches(colorlessName)) {
            return false
        }

        return true
    }

    fun removeColors(str: String) = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', str)) ?: ""
}