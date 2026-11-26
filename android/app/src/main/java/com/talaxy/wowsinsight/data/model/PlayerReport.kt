package com.talaxy.wowsinsight.data.model

import java.text.Collator
import java.time.LocalDate
import java.util.Locale

/** A player returned by the account search endpoint. */
data class PlayerSummary(
    val accountId: Long,
    val nickname: String,
)

/** Account details shown on the player header card. */
data class AccountInfo(
    val accountId: Long,
    val nickname: String,
    val createdAt: Int?,
    val lastBattleTime: Int?,
    /** Absolute URL of the in-game dog tag image. */
    val dogTagUrl: String?,
    /** "[TAG] Name" of the current clan, or `null` when the account is not in one. */
    val clanName: String?,
)

/** The account overview returned by `account/user/info2`, split per mode. */
data class AccountOverview(
    val account: AccountInfo,
    val modes: Map<BattleMode, ModeStats>,
    val shipTypes: List<CategoryStats>,
    val tiers: List<CategoryStats>,
)

/** One category of `shipTypeInfo` or `levelInfo`. */
sealed interface CategoryKey {
    data class ShipType(val code: String) : CategoryKey
    data class Tier(val tier: Int) : CategoryKey
}

data class CategoryStats(
    val key: CategoryKey,
    val modes: Map<BattleMode, ModeStats>,
) {
    val sortOrder: Int
        get() = when (key) {
            is CategoryKey.ShipType ->
                ShipInfo.TYPE_ORDER.indexOf(key.code).takeIf { it >= 0 } ?: ShipInfo.TYPE_ORDER.size
            is CategoryKey.Tier -> key.tier
        }
}

/** One day of the recently recorded battles of `account/day/list/info`. */
data class DailyStats(
    /** Exact record time in epoch milliseconds; unique per record. */
    val recordedAt: Long,
    /** Local calendar day the record was taken on. */
    val day: LocalDate,
    val modes: Map<BattleMode, ModeStats>,
)

/** Per-ship data assembled from `account/ship/info/list`. */
data class ShipStat(
    val ship: ShipInfo,
    val modes: Map<BattleMode, ModeStats>,
)

/**
 * Everything the detail screen needs, assembled once after loading.
 *
 * Totals, ratings and records are kept per battle type; the screen shows one
 * type at a time and defaults to random battles.
 */
data class PlayerReport(
    val account: AccountInfo,
    /** Battle types with at least one battle, in picker order. */
    val modes: List<BattleMode>,
    val modeStats: Map<BattleMode, ModeStats>,
    val modeRecords: Map<BattleMode, List<BattleRecord>>,
    val ships: List<ShipPerformance>,
    /** Days recorded by the service, oldest first. */
    val dailyStats: List<DailyStats>,
    val shipTypes: List<CategoryStats>,
    val tiers: List<CategoryStats>,
) {
    /** Random battles when available, otherwise the first mode with battles. */
    val defaultMode: BattleMode?
        get() = if (modes.contains(BattleMode.PVP)) BattleMode.PVP else modes.firstOrNull()

    fun stats(forMode: BattleMode): ModeStats? = modeStats[forMode]

    fun records(forMode: BattleMode): List<BattleRecord> = modeRecords[forMode].orEmpty()

    /** The recorded days that fought [mode], which is what the trend plots. */
    fun dailyStats(forMode: BattleMode): List<DailyStats> = dailyByMode[forMode].orEmpty()

    /** The categories that fought [mode], in chart order. */
    fun categories(breakdown: ChartBreakdown, forMode: BattleMode): List<CategoryStats> =
        categoriesByMode[breakdown].orEmpty()[forMode].orEmpty()

    /**
     * The days of [forMode] inside the trend axis, which spans [TREND_AXIS_DAYS]
     * days ending on the newest recorded day. The service can keep older
     * snapshots, and those are not drawn.
     */
    fun trendDays(forMode: BattleMode): List<DailyStats> {
        val axisStart = dailyStats.lastOrNull()?.day?.minusDays(TREND_AXIS_DAYS - 1)
            ?: return emptyList()
        return dailyStats(forMode).filter { !it.day.isBefore(axisStart) }
    }

    /** One recorded day is a number, not a trend, so the trend card needs two. */
    fun hasTrend(forMode: BattleMode): Boolean = trendDays(forMode).size > 1

    fun hasBreakdown(forMode: BattleMode): Boolean =
        ChartBreakdown.entries.any { categories(it, forMode).isNotEmpty() }

    /**
     * The player page asks for these lists on every recomposition (scrolling the
     * ship list rebuilds it), so filtering and locale-aware sorting happen once
     * per mode instead of per frame.
     */
    private val dailyByMode: Map<BattleMode, List<DailyStats>> by lazy {
        dailyStats
            .flatMap { it.modes.keys }
            .distinct()
            .associateWith { mode ->
                dailyStats.filter { (it.modes[mode]?.stats?.battleCount ?: 0) > 0 }
            }
    }

    private val categoriesByMode: Map<ChartBreakdown, Map<BattleMode, List<CategoryStats>>> by lazy {
        val collator = Collator.getInstance(Locale.getDefault())
        ChartBreakdown.entries.associateWith { breakdown ->
            val source = if (breakdown == ChartBreakdown.SHIP_TYPE) shipTypes else tiers
            val modes = source.flatMap { it.modes.keys }.distinct()
            modes.associateWith { mode ->
                source
                    .filter { (it.modes[mode]?.stats?.battleCount ?: 0) > 0 }
                    .sortedWith(
                        compareBy<CategoryStats> { it.sortOrder }
                            .thenComparator { left, right ->
                                collator.compare(left.key.toString(), right.key.toString())
                            },
                    )
            }
        }
    }

    companion object {
        /** The service keeps the last week, so the trend axis always spans seven days. */
        const val TREND_AXIS_DAYS = 7L

        fun create(
            overview: AccountOverview,
            shipStats: List<ShipStat>,
            dailyStats: List<DailyStats>,
        ): PlayerReport {
            // Every ship goes into the catalog, not only those with visible
            // stats, so a record can always name the ship it was achieved on.
            val catalog = shipStats.associate { it.ship.shipId to it.ship }
            val played = overview.modes.filterValues { it.stats.battleCount > 0 }
            val modes = BattleMode.entries.filter { played.containsKey(it) }

            return PlayerReport(
                account = overview.account,
                modes = modes,
                modeStats = played,
                modeRecords = played.mapValues { (_, stats) ->
                    BattleRecords.list(stats.stats, catalog)
                },
                ships = shipStats.map { ShipPerformance(it.ship, it.modes) },
                dailyStats = dailyStats,
                shipTypes = overview.shipTypes,
                tiers = overview.tiers,
            )
        }
    }
}

/**
 * Ships that actually fought in [mode], sorted by [sort] with the ship name as
 * a stable tie-break.
 */
fun List<ShipPerformance>.ranked(mode: BattleMode, sort: ShipSort): List<ShipPerformance> {
    val collator = Collator.getInstance(Locale.getDefault())
    fun score(stats: ModeStats): Double = when (sort) {
        ShipSort.BATTLES -> stats.stats.battleCount.toDouble()
        ShipSort.WIN_RATE -> stats.stats.winRate
        ShipSort.AVERAGE_DAMAGE -> stats.stats.averageDamage
        ShipSort.RATING -> stats.rating.value.toDouble()
        ShipSort.LAST_BATTLE -> (stats.lastBattleTime ?: 0).toDouble()
    }

    return mapNotNull { ship ->
        val stats = ship.modes[mode] ?: return@mapNotNull null
        if (stats.stats.battleCount <= 0) return@mapNotNull null
        ship to stats
    }.sortedWith { left, right ->
        val leftScore = score(left.second)
        val rightScore = score(right.second)
        if (leftScore == rightScore) {
            collator.compare(left.first.ship.name, right.first.ship.name)
        } else {
            rightScore.compareTo(leftScore)
        }
    }.map { it.first }
}
