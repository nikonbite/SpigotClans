package org.mmarket.clans

import org.bukkit.plugin.java.JavaPlugin

class ClansPlugin : JavaPlugin() {
    override fun onEnable() {
        instance = this
        Clans("Clans", "1.0", this).load()
    }

    override fun onDisable() = Clans.instance.unload()

    companion object {
        lateinit var instance: ClansPlugin
            private set
    }
}