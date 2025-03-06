package org.mmarket.clans.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.mmarket.clans.Clans

class PlaceholderApiHook(val clans: Clans) : PlaceholderExpansion() {
    override fun getAuthor(): String {
        return "mMarket"
    }

    override fun getIdentifier(): String {
        return "clans"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun onRequest(player: OfflinePlayer?, placeholder: String): String? {
        val name = player?.name ?: return null

/*
        for (dungeon in dm.dungeons) {
            if (placeholder == "${dungeon.name}_done") {
                return if (dm.isDone(name, dungeon.name)) PAPI_YES else PAPI_NO
            }
        }
*/

        return null
    }
}