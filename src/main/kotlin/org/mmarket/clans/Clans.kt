package org.mmarket.clans

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.event.Listener
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.mmarket.clans.command.AclanCommand
import org.mmarket.clans.command.CcCommand
import org.mmarket.clans.command.ClanCommand
import org.mmarket.clans.hook.CitizensHook
import org.mmarket.clans.hook.PlaceholderApiHook
import org.mmarket.clans.hook.VaultHook
import org.mmarket.clans.system.table.ClanInvitesTable
import org.mmarket.clans.system.table.ClanMembersTable
import org.mmarket.clans.system.table.ClansTable
import java.time.LocalDateTime
import kotlin.math.log

class Clans(val name: String, val version: String, val plugin: ClansPlugin) {
    private var loaded = false
    val server = plugin.server
    val logger = plugin.logger
    val pluginManager = server.pluginManager
    val commandMap = server.commandMap

    fun load() {
        if (loaded) {
            logger.severe("Не удалось загрузить системы плагина, поскольку они уже загружены.")
            return
        }

        instance = this

        info()
        hook()
        database()
        commands(
            ClanCommand(),
            CcCommand(),
            AclanCommand()
        )
        listeners(

        )
        jobs()

        logger.info("=".repeat(50))

        loaded = true
    }

    fun reload() {

    }

    fun unload() {
        loaded = false
    }


    private fun info() {
        listOf(
            "=".repeat(50),
            "$name v${version}",
            "",
            "Plugin by mMarket | vk.com/mMarket",
            "Developer: Nikonbite",
            "=".repeat(50),
        ).forEach { logger.info(it) }
    }

    private fun hook() {
        val prefix = prefix("Hook")

        val papi = "PlaceholderAPI"
        if (server.pluginManager.isPluginEnabled(papi)) {
            PlaceholderApiHook(this).register()
            logger.info("$prefix Связка с $papi успешно установлена.")
        }

        val vault = "Vault"
        if (server.pluginManager.isPluginEnabled(vault)) {
            VaultHook()
            logger.info("$prefix Связка с $vault успешно установлена.")
        }

        val citizens = "Citizens"
        if (server.pluginManager.isPluginEnabled(citizens)) {
            CitizensHook()
            logger.info("$prefix Связка с $citizens успешно установлена.")
        }
    }

    private fun commands(vararg commands: Command) {
        val prefix = prefix("Commands")

        commands.forEach {
            commandMap.register(it.name, it)
            logger.info("$prefix Команда ${it.name} успешно зарегистрирована.")
        }
    }

    private fun listeners(vararg listeners: Listener) {
        val prefix = prefix("Listeners")

        listeners.forEach {
            pluginManager.registerEvents(it, plugin)
            logger.info("$prefix Слушатель ${it.javaClass.name} успешно зарегистрирован.")
        }
    }

    fun database() {
        val prefix = prefix("Database")

        logger.info("$prefix Устанавливаем соединение с базой данных...")
        Database.connect(
            url = "jdbc:mysql://localhost:3306/mydatabase",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "password"
        )

        logger.info("$prefix Подготавливаем таблицы...")
        transaction {
            SchemaUtils.create(ClansTable, ClanMembersTable, ClanInvitesTable)
        }
        logger.info("$prefix Таблицы подготовлены...")
    }

    fun jobs() {
        val prefix = prefix("Jobs")

        logger.info("$prefix Запускаем работу \"Очистка заявок\"...")
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(24 * 60 * 60 * 1000) // Каждые 24 часа
/*                transaction {
                    ClanInvitesTable.deleteWhere {
                        createdAt lessEq LocalDateTime.now().minusDays(7)
                    }
                }*/
            }
        }
        logger.info("$prefix Работа \"Очистка заявок\" запущена.")
    }

    private fun prefix(name: String) = "( $name ) ::"

    companion object {
        lateinit var instance: Clans
            private set
    }
}