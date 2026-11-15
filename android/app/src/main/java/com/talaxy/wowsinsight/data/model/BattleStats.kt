package com.talaxy.wowsinsight.data.model

/**
 * Battle totals of a player or a single ship in one battle type.
 *
 * Every value is normalised to zero while mapping the API response, so the
 * computed properties are always safe to display.
 */
data class BattleStats(
    val battleCount: Int,
    val winCount: Int,
    val lossCount: Int,
    val survivedCount: Int,
    val survivedWinCount: Int,
    val damage: Long,
    val scoutingDamage: Long,
    val fragCount: Int,
    val experience: Long,
    val planesDestroyed: Int,
    val spottedCount: Int,
    val potentialDamage: Long,
    val hitRate: Double,
    val records: Map<BattleRecordKind, BattleRecordValue>,
) {
    val drawCount: Int get() = (battleCount - winCount - lossCount).coerceAtLeast(0)
    val winRate: Double get() = ratio(winCount, battleCount) * 100
    val averageDamage: Double get() = ratio(damage.toDouble(), battleCount)
    val averageFrags: Double get() = ratio(fragCount.toDouble(), battleCount)
    val averageExperience: Double get() = ratio(experience.toDouble(), battleCount)
    val survivalRate: Double get() = ratio(survivedCount.toDouble(), battleCount) * 100
    val survivedWinRate: Double get() = ratio(survivedWinCount.toDouble(), survivedCount) * 100

    val killDeathRatio: Double
        get() {
            val deaths = battleCount - survivedCount
            if (deaths <= 0) return 0.0
            return fragCount.toDouble() / deaths
        }

    private fun ratio(value: Int, total: Int): Double = ratio(value.toDouble(), total)

    private fun ratio(value: Double, total: Int): Double =
        if (total > 0) value / total else 0.0

    companion object {
        val Empty = BattleStats(
            battleCount = 0,
            winCount = 0,
            lossCount = 0,
            survivedCount = 0,
            survivedWinCount = 0,
            damage = 0L,
            scoutingDamage = 0L,
            fragCount = 0,
            experience = 0L,
            planesDestroyed = 0,
            spottedCount = 0,
            potentialDamage = 0L,
            hitRate = 0.0,
            records = emptyMap(),
        )
    }
}

/** One best-battle record: its value and the ship it was achieved on. */
data class BattleRecordValue(
    val value: Long,
    val shipId: Long?,
)

/** The key of a best battle reported by the API in `maxInfo`. */
enum class BattleRecordKind(val apiKey: String) {
    MAX_DAMAGE("maxDamageDealt"),
    MAX_FRAGS("maxFrags"),
    MAX_XP("maxXp"),
    MAX_PLANES("maxPlanesKilled"),
    MAX_SPOTTED("maxShipsSpotted"),
    MAX_AGRO("maxTotalAgro"),
    MAX_SCOUTING("maxScoutingDamage"),
}

/** A record ready to display, with the ship name resolved when available. */
data class BattleRecord(
    val kind: BattleRecordKind,
    val value: Long,
    val shipName: String?,
) {
    val id: String get() = kind.apiKey
}

object BattleRecords {
    /**
     * The records that were actually achieved, in [BattleRecordKind] order.
     * [catalog] names the ships; it may be empty where the ship is already
     * known, in which case the ship name is omitted.
     */
    fun list(
        stats: BattleStats,
        catalog: Map<Long, ShipInfo> = emptyMap(),
    ): List<BattleRecord> = BattleRecordKind.entries.mapNotNull { kind ->
        val record = stats.records[kind] ?: return@mapNotNull null
        if (record.value <= 0L) return@mapNotNull null
        BattleRecord(
            kind = kind,
            value = record.value,
            shipName = record.shipId?.let { catalog[it]?.name },
        )
    }
}
