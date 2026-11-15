package com.talaxy.wowsinsight.ui.util

import androidx.annotation.StringRes
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.BattleRecordKind
import com.talaxy.wowsinsight.data.model.ChartBreakdown
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.RatingTier
import com.talaxy.wowsinsight.data.model.ShipInfo
import com.talaxy.wowsinsight.data.model.ShipNation
import com.talaxy.wowsinsight.data.model.ShipSort

@StringRes
fun GameServer.labelRes(): Int = when (this) {
    GameServer.RU -> R.string.server_ru
    GameServer.EU -> R.string.server_eu
    GameServer.NA -> R.string.server_na
    GameServer.ASIA -> R.string.server_asia
    GameServer.CN -> R.string.server_cn
}

@StringRes
fun BattleMode.labelRes(): Int = when (this) {
    BattleMode.PVP -> R.string.battle_mode_pvp
    BattleMode.PVP_SOLO -> R.string.battle_mode_pvp_solo
    BattleMode.PVP_DIV2 -> R.string.battle_mode_pvp_div2
    BattleMode.PVP_DIV3 -> R.string.battle_mode_pvp_div3
    BattleMode.RANKED -> R.string.battle_mode_ranked
    BattleMode.RANKED_SOLO -> R.string.battle_mode_ranked_solo
    BattleMode.RANKED_DIV2 -> R.string.battle_mode_ranked_div2
    BattleMode.RANKED_DIV3 -> R.string.battle_mode_ranked_div3
    BattleMode.RANKED_OLD -> R.string.battle_mode_ranked_old
    BattleMode.RANKED_OLD_SOLO -> R.string.battle_mode_ranked_old_solo
    BattleMode.RANKED_OLD_DIV2 -> R.string.battle_mode_ranked_old_div2
    BattleMode.RANKED_OLD_DIV3 -> R.string.battle_mode_ranked_old_div3
    BattleMode.CLAN -> R.string.battle_mode_clan
    BattleMode.BRAWL -> R.string.battle_mode_brawl
    BattleMode.OPERATIONS -> R.string.battle_mode_operations
    BattleMode.COOP -> R.string.battle_mode_coop
    BattleMode.COOP_SOLO -> R.string.battle_mode_coop_solo
    BattleMode.COOP_DIV2 -> R.string.battle_mode_coop_div2
    BattleMode.COOP_DIV3 -> R.string.battle_mode_coop_div3
    BattleMode.RATING -> R.string.battle_mode_rating
    BattleMode.RATING_SOLO -> R.string.battle_mode_rating_solo
    BattleMode.RATING_DIV -> R.string.battle_mode_rating_div
}

@StringRes
fun RatingTier.labelRes(): Int = when (this) {
    RatingTier.UNKNOWN -> R.string.rating_tier_unknown
    RatingTier.BAD -> R.string.rating_tier_bad
    RatingTier.BELOW_AVERAGE -> R.string.rating_tier_below_average
    RatingTier.AVERAGE -> R.string.rating_tier_average
    RatingTier.GOOD -> R.string.rating_tier_good
    RatingTier.VERY_GOOD -> R.string.rating_tier_very_good
    RatingTier.GREAT -> R.string.rating_tier_great
    RatingTier.UNICUM -> R.string.rating_tier_unicum
    RatingTier.SUPER_UNICUM -> R.string.rating_tier_super_unicum
}

@StringRes
fun ShipSort.labelRes(): Int = when (this) {
    ShipSort.BATTLES -> R.string.sort_battles
    ShipSort.WIN_RATE -> R.string.sort_win_rate
    ShipSort.AVERAGE_DAMAGE -> R.string.sort_avg_damage
    ShipSort.RATING -> R.string.sort_rating
    ShipSort.LAST_BATTLE -> R.string.sort_last_battle
}

@StringRes
fun ChartMetric.labelRes(): Int = when (this) {
    ChartMetric.RATING -> R.string.sort_rating
    ChartMetric.WIN_RATE -> R.string.player_stats_win_rate
}

@StringRes
fun ChartBreakdown.labelRes(): Int = when (this) {
    ChartBreakdown.SHIP_TYPE -> R.string.player_charts_breakdown_ship_type
    ChartBreakdown.TIER -> R.string.player_charts_breakdown_tier
}

@StringRes
fun ShipInfo.typeLabelRes(): Int = shipTypeLabelRes(type)

@StringRes
fun shipTypeLabelRes(type: String): Int = when (type) {
    "Battleship" -> R.string.ship_type_battleship
    "Cruiser" -> R.string.ship_type_cruiser
    "Destroyer" -> R.string.ship_type_destroyer
    "AirCarrier" -> R.string.ship_type_aircarrier
    "Submarine" -> R.string.ship_type_submarine
    "Auxiliary" -> R.string.ship_type_auxiliary
    else -> R.string.ship_type_unknown
}

@StringRes
fun ShipNation.labelRes(): Int = when (this) {
    ShipNation.USA -> R.string.ship_nation_usa
    ShipNation.JAPAN -> R.string.ship_nation_japan
    ShipNation.GERMANY -> R.string.ship_nation_germany
    ShipNation.USSR -> R.string.ship_nation_ussr
    ShipNation.UK -> R.string.ship_nation_uk
    ShipNation.FRANCE -> R.string.ship_nation_france
    ShipNation.ITALY -> R.string.ship_nation_italy
    ShipNation.PAN_ASIA -> R.string.ship_nation_pan_asia
    ShipNation.EUROPE -> R.string.ship_nation_europe
    ShipNation.PAN_AMERICA -> R.string.ship_nation_pan_america
    ShipNation.NETHERLANDS -> R.string.ship_nation_netherlands
    ShipNation.SPAIN -> R.string.ship_nation_spain
    ShipNation.COMMONWEALTH -> R.string.ship_nation_commonwealth
    ShipNation.LATIN_AMERICA -> R.string.ship_nation_latin_america
    ShipNation.EVENTS -> R.string.ship_nation_events
    ShipNation.UNKNOWN -> R.string.ship_nation_unknown
}

@StringRes
fun BattleRecordKind.titleRes(): Int = when (this) {
    BattleRecordKind.MAX_DAMAGE -> R.string.record_max_damage
    BattleRecordKind.MAX_FRAGS -> R.string.record_max_frags
    BattleRecordKind.MAX_XP -> R.string.record_max_xp
    BattleRecordKind.MAX_PLANES -> R.string.record_max_planes
    BattleRecordKind.MAX_SPOTTED -> R.string.record_max_spotted
    BattleRecordKind.MAX_AGRO -> R.string.record_max_agro
    BattleRecordKind.MAX_SCOUTING -> R.string.record_max_scouting
}
