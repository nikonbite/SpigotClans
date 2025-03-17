package org.mmarket.clans.system.model

import org.mmarket.clans.files.Settings

enum class ClanAdvertisementTariff(val hours: Int, val cost: Double) {
    TARIFF_12(Settings.long("ad.tariff_12.hours").toInt(), Settings.double("ad.tariff_12.cost")),
    TARIFF_24(Settings.long("ad.tariff_24.hours").toInt(), Settings.double("ad.tariff_24.cost")),
}