package org.mmarket.clans.api.utility

import org.mmarket.clans.files.Settings

object NamingUtility {

    fun validateClanName(name: String?): Boolean {
        return if (name.isNullOrBlank()) {
            false
        } else {
            val colorlessName = removeColors(name)

            val restrictions = listOf(
                colorlessName.length < Settings.long("restrictions.min_name_length"),
                colorlessName.length > Settings.long("restrictions.max_name_length"),
                !"^[a-zA-Z0-9_\\-\\s]+$".toRegex().matches(colorlessName),
            )

            !restrictions.contains(true)
        }
    }

    fun removeColors(str: String) = str.replace("&[a-fA-F0-9k-oK-OrR]".toRegex(), "")
}