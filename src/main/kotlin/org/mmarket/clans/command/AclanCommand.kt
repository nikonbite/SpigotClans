package org.mmarket.clans.command

import org.bukkit.entity.Player
import org.mmarket.clans.api.command.SuperCommand
import org.mmarket.clans.api.command.SuperSubcommand

class AclanCommand : SuperCommand(
    listOf("aclan", "фсдфт"),
    listOf(
        KickSubcommand(),
        DisbandSubcommand(),
        ClearMOTDSubcommand(),
        PromoteSubcommand(),
        DemoteSubcommand(),
        ShopSubcommand(),
        NpcSubcommand(),
        RenameSubcommand(),
        UuidSubcommand(),
        AdSubcommand()
    )
) {

    override fun perform(player: Player, args: List<String>) { }

    override fun help(player: Player) { }
    
    /** Выкинуть игрока из клана */
    class KickSubcommand : SuperSubcommand(listOf("kick", "лшсл")) {
        override fun perform(player: Player, args: List<String>) {
            
        }
    }
    
    /** Распустить клан */
    class DisbandSubcommand : SuperSubcommand(listOf("disband", "вшыифтв")) {
        override fun perform(player: Player, args: List<String>) {
            
        }
    }
    
    /** Очистить MOTD клана */
    class ClearMOTDSubcommand : SuperSubcommand(listOf("clearmotd", "сдуфкьщев")) {
        override fun perform(player: Player, args: List<String>) {
            
        }
    }
    
    /** Повысить соклановца */
    class PromoteSubcommand : SuperSubcommand(listOf("promote", "зкщьщеу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Понизить соклановца */
    class DemoteSubcommand : SuperSubcommand(listOf("demote", "вуьщеу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
    
    /** Открыть магазин клана (без траты средств) */
    class ShopSubcommand : SuperSubcommand(listOf("shop", "ырщз")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
    
    /** Управление рекламными NPC */
    class NpcSubcommand : SuperSubcommand(listOf("npc", "тзс")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
    
    /** Переименовать клан */
    class RenameSubcommand : SuperSubcommand(listOf("rename", "кутфьу")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
    
    /** Посмотреть UUID клана в котором состоит игрок */
    class UuidSubcommand : SuperSubcommand(listOf("uuid", "ггшв")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
    
    /** Управление рекламой клана */
    class AdSubcommand : SuperSubcommand(listOf("ad", "фв")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }

    /** Перезагрузить плагин */
    class ReloadSubcommand : SuperSubcommand(listOf("reload", "кудщфв")) {
        override fun perform(player: Player, args: List<String>) {

        }
    }
}