package com.talaxy.wowsinsight.data.model

/** One ship of the encyclopedia as shown by the app. */
data class ShipInfo(
    val shipId: Long,
    val name: String,
    val tier: Int,
    /** Raw API type code, e.g. `Cruiser`. */
    val type: String,
    val nation: ShipNation,
    val imageUrl: String?,
) {
    val tierLabel: String get() = tierLabel(tier)

    companion object {
        private val TIER_LABELS = listOf(
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "★",
        )

        fun tierLabel(tier: Int): String =
            TIER_LABELS.getOrNull(tier - 1) ?: "—"

        /** The order the game lists ship types in, used by the breakdown chart. */
        val TYPE_ORDER = listOf(
            "Destroyer",
            "Cruiser",
            "Battleship",
            "AirCarrier",
            "Submarine",
            "Auxiliary",
        )
    }
}

/** Totals, rating and last battle of a player or ship in one battle type. */
data class ModeStats(
    val stats: BattleStats,
    val rating: PlayerRating,
    val lastBattleTime: Int?,
)

/** One ship with its unsorted per-mode totals. */
data class ShipPerformance(
    val ship: ShipInfo,
    val modes: Map<BattleMode, ModeStats>,
) {
    val id: Long get() = ship.shipId

    fun stats(forMode: BattleMode): ModeStats? = modes[forMode]
}

/** The categories the breakdown chart can group battles by. */
enum class ChartBreakdown {
    SHIP_TYPE,
    TIER,
}

/** The two numbers the trend chart can plot for one battle type. */
enum class ChartMetric {
    RATING,
    WIN_RATE,
    ;

    fun valueOf(stats: ModeStats): Double = when (this) {
        RATING -> stats.rating.value.coerceAtLeast(0).toDouble()
        WIN_RATE -> stats.stats.winRate
    }
}

/** Sorting options for the player's ship list. */
enum class ShipSort {
    BATTLES,
    WIN_RATE,
    AVERAGE_DAMAGE,
    RATING,
    LAST_BATTLE,
    ;

    companion object {
        val Default = LAST_BATTLE
    }
}
