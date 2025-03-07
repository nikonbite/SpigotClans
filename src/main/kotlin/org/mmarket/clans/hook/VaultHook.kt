package org.mmarket.clans.hook

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.mmarket.clans.Clans
import java.util.UUID

object VaultHook {
    private lateinit var economy: Economy

    /**
     * Инициализирует экономику через VaultAPI
     */
    fun init(): Boolean {
        val clans = Clans.instance
        val rsp = clans.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            clans.logger.severe("( Vault ) :: Не найден сервис экономики! Экономические функции будут недоступны.")
            return false
        }

        economy = rsp.provider
        return true
    }

    /**
     * Проверяет, достаточно ли у игрока средств
     */
    fun has(player: Player, amount: Double) = economy.has(player, amount)

    /**
     * Проверяет, достаточно ли у игрока средств
     */
    fun has(playerId: UUID, amount: Double): Boolean {
        val player = Bukkit.getPlayer(playerId) ?: return false
        return has(player, amount)
    }

    /**
     * Снимает деньги с игрока
     */
    fun withdraw(player: Player, amount: Double): Boolean {
        if (!has(player, amount)) return false
        return economy.withdrawPlayer(player, amount)?.transactionSuccess() == true
    }

    /**
     * Снимает деньги с игрока
     */
    fun withdraw(playerId: UUID, amount: Double): Boolean {
        val player = Bukkit.getPlayer(playerId) ?: return false
        return withdraw(player, amount)
    }

    /**
     * Добавляет деньги игроку
     */
    fun deposit(player: Player, amount: Double) = economy.depositPlayer(player, amount)?.transactionSuccess() == true

    /**
     * Добавляет деньги игроку
     */
    fun deposit(playerId: UUID, amount: Double): Boolean {
        val player = Bukkit.getPlayer(playerId) ?: return false
        return deposit(player, amount)
    }

    /**
     * Получает текущий баланс игрока
     */
    fun balance(player: Player) = economy.getBalance(player)

    /**
     * Получает текущий баланс игрока
     */
    fun balance(playerId: UUID): Double {
        val player = Bukkit.getPlayer(playerId) ?: return 0.0
        return balance(player)
    }

    /**
     * Форматирует сумму в строку
     */
    fun format(amount: Double) = economy.format(amount) ?: amount.toString()
}