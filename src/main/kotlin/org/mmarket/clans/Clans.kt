package org.mmarket.clans

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.event.Listener
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.support.mysql.MySqlDialect
import org.mmarket.clans.api.pending.ActionManager
import org.mmarket.clans.command.AclanCommand
import org.mmarket.clans.command.CcCommand
import org.mmarket.clans.command.ClanCommand
import org.mmarket.clans.files.Interfaces
import org.mmarket.clans.files.Messages
import org.mmarket.clans.files.Settings
import org.mmarket.clans.hook.CitizensHook
import org.mmarket.clans.hook.PlaceholderApiHook
import org.mmarket.clans.hook.VaultHook
import org.mmarket.clans.system.manager.ClanManager
import org.mmarket.clans.system.table.ClanInvitesTable
import org.mmarket.clans.system.table.ClanMembersTable
import org.mmarket.clans.system.table.ClansTable
import java.time.LocalDateTime

class Clans(val name: String, val version: String, val plugin: ClansPlugin) {
    private var loaded = false
    val server = plugin.server
    val logger = plugin.logger
    val pluginManager = server.pluginManager
    val commandMap = server.commandMap
    val actionManager = ActionManager(plugin)
    
    // База данных
    lateinit var database: Database
        private set

    fun load() {
        if (loaded) {
            logger.severe("Не удалось загрузить системы плагина, поскольку они уже загружены.")
            return
        }

        instance = this

        info()
        files()
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
        clans()

        logger.info("=".repeat(50))

        loaded = true
    }

    fun reload() {
        Settings.reload()
        Messages.reload()
        Interfaces.reload()
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

    private fun files() {
        val prefix = prefix("Files")

        logger.info("$prefix Идёт загрузка сообщений...")
        Messages.init()
        logger.info("$prefix Сообщения успешно загружены.")

        logger.info("$prefix Идёт загрузка настроек...")
        Settings.init()
        logger.info("$prefix Настройки успешно загружены.")

        logger.info("$prefix Идёт загрузка интерфейсов...")
        Interfaces.init()
        logger.info("$prefix Интерфейсы успешно загружены.")
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
            VaultHook.init()
            logger.info("$prefix Связка с $vault успешно установлена.")
        }

        val citizens = "Citizens"
        if (server.pluginManager.isPluginEnabled(citizens)) {
            CitizensHook.init()
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

        val host = Settings.string("database.host")
        val port = Settings.long("database.port")
        val user = Settings.string("database.user")
        val pass = Settings.string("database.pass")
        val data = Settings.string("database.data")

        database = Database.connect(
            url = "jdbc:mysql://${host}:${port}/${data}",
            driver = "com.mysql.cj.jdbc.Driver",
            user = user,
            password = pass,
            dialect = MySqlDialect()
        )

        logger.info("$prefix Соединение с базой данных установлено.")

        logger.info("$prefix Подготавливаем таблицы...")
        // Создаем таблицы, если их нет
        database.useConnection { conn ->
            conn.createStatement().use { stmt ->
                // Создаем таблицу кланов
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS clans (
                        id BINARY(16) PRIMARY KEY,
                        name VARCHAR(256) UNIQUE,
                        colorless_name VARCHAR(12),
                        treasury BIGINT DEFAULT 0,
                        score BIGINT DEFAULT 0,
                        news JSON DEFAULT ('[]'),
                        motd TEXT DEFAULT '',
                        creator BINARY(16),
                        owner BINARY(16),
                        created_at DATETIME,
                        slots VARCHAR(50),
                        chat_purchased BOOLEAN DEFAULT FALSE,
                        motd_purchased BOOLEAN DEFAULT FALSE,
                        party_purchased BOOLEAN DEFAULT FALSE
                    )
                """)
                
                // Создаем таблицу участников клана
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS clan_members (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        clan_id BINARY(16),
                        uuid BINARY(16) UNIQUE,
                        name VARCHAR(36),
                        role VARCHAR(50),
                        joined_at DATETIME,
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                """)
                
                // Создаем таблицу приглашений в клан
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS clan_invites (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        clan_id BINARY(16),
                        uuid BINARY(16),
                        name TEXT,
                        created_at DATETIME,
                        FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                    )
                """)

                // Создаем таблицу с оценками кланов
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS clan_scores (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid BINARY(16),
                        player_name TEXT,
                        scores JSON
                    )
                """)
            }
        }
        
        logger.info("$prefix Таблицы подготовлены.")
    }

    fun jobs() {
        val prefix = prefix("Jobs")

        logger.info("$prefix Запускаем работу \"Очистка заявок\"...")
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(24 * 60 * 60 * 1000)
                database.delete(ClanInvitesTable) {
                    it.createdAt.lt(LocalDateTime.now().minusDays(7))
                }
            }
        }
        logger.info("$prefix Работа \"Очистка заявок\" запущена.")
    }

    fun clans() {
        val prefix = prefix("Clans")
        logger.info("$prefix Идёт загрузка кланов...")
        ClanManager.init(database)
        logger.info("$prefix Кланы успешно загружены.")
    }

    private fun prefix(name: String) = "( $name ) ::"

    companion object {
        lateinit var instance: Clans
            private set
    }
}