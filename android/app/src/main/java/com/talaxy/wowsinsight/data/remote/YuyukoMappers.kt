package com.talaxy.wowsinsight.data.remote

import com.talaxy.wowsinsight.data.model.AccountInfo
import com.talaxy.wowsinsight.data.model.AccountOverview
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.BattleRecordKind
import com.talaxy.wowsinsight.data.model.BattleRecordValue
import com.talaxy.wowsinsight.data.model.BattleStats
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.DailyStats
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.ModeStats
import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.model.ShipInfo
import com.talaxy.wowsinsight.data.model.ShipNation
import com.talaxy.wowsinsight.data.model.ShipStat
import com.talaxy.wowsinsight.data.model.ShipNameLanguage
import java.time.Instant
import java.time.ZoneId

internal fun YuyukoRating.toPlayerRating(): PlayerRating {
    val code = code ?: return PlayerRating.Unknown
    val value = value ?: return PlayerRating.Unknown
    return if (code != 0 && value > 0) PlayerRating(value) else PlayerRating.Unknown
}

internal fun YuyukoStatsBlock.toBattleStats(): BattleStats {
    val totals = battleInfo
    val best = maxInfo

    val records = buildMap {
        fun add(kind: BattleRecordKind, entry: YuyukoMaxValue?) {
            val value = entry?.value ?: return
            put(kind, BattleRecordValue(value = value, shipId = entry.shipId))
        }
        add(BattleRecordKind.MAX_DAMAGE, best?.maxDamageDealt)
        add(BattleRecordKind.MAX_FRAGS, best?.maxFrags)
        add(BattleRecordKind.MAX_XP, best?.maxXp)
        add(BattleRecordKind.MAX_PLANES, best?.maxPlanesKilled)
        add(BattleRecordKind.MAX_SPOTTED, best?.maxShipsSpotted)
        add(BattleRecordKind.MAX_AGRO, best?.maxTotalAgro)
        add(BattleRecordKind.MAX_SCOUTING, best?.maxScoutingDamage)
    }

    return BattleStats(
        battleCount = totals?.battle ?: 0,
        winCount = totals?.wins ?: 0,
        lossCount = totals?.losses ?: 0,
        survivedCount = totals?.survived ?: 0,
        survivedWinCount = totals?.winAndSurvived ?: 0,
        damage = totals?.damageDealt ?: 0L,
        scoutingDamage = totals?.scoutingDamage ?: 0L,
        fragCount = fragsInfo?.frags ?: 0,
        experience = totals?.xp ?: 0L,
        planesDestroyed = totals?.planesKilled ?: 0,
        spottedCount = totals?.shipsSpotted ?: 0,
        potentialDamage = totals?.artAgro ?: 0L,
        hitRate = hitRatioInfo?.ratioMain ?: 0.0,
        records = records,
    )
}

/** The battle types the app can name and that carry a stats block. */
internal fun Map<String, YuyukoModeSummary>.toModeStats(): Map<BattleMode, ModeStats> =
    buildMap {
        for ((code, summary) in this@toModeStats) {
            val mode = BattleMode.fromApi(code) ?: continue
            val block = summary.stats ?: continue
            put(
                mode,
                ModeStats(
                    stats = block.toBattleStats(),
                    rating = summary.prInfo?.toPlayerRating() ?: PlayerRating.Unknown,
                    lastBattleTime = block.lastBattleTime,
                ),
            )
        }
    }

/**
 * `shipTypeInfo` and `levelInfo` nest the same summary one level deeper, keyed
 * by the category the chart groups by. Only categories that were played are
 * returned, so the chart never draws an empty bar.
 */
internal fun Map<String, Map<String, YuyukoModeSummary>>.toCategories(
    keyOf: (String) -> CategoryKey?,
): List<CategoryStats> = mapNotNull { (name, modes) ->
    val key = keyOf(name) ?: return@mapNotNull null
    val stats = modes.toModeStats()
    if (stats.values.none { it.stats.battleCount > 0 }) return@mapNotNull null
    CategoryStats(key = key, modes = stats)
}

internal fun YuyukoShipMeta.toShipInfo(
    language: ShipNameLanguage,
    server: GameServer,
): ShipInfo {
    fun first(candidates: List<String?>): String? =
        candidates.firstOrNull { !it.isNullOrBlank() }

    val chinese = if (server == GameServer.CN) {
        first(listOf(nameCn360, nameCn, nameEnglish))
    } else {
        first(listOf(nameCn, nameCn360, nameEnglish))
    }
    val english = first(listOf(nameEnglish, nameCn, nameCn360))
    val resolved = if (language == ShipNameLanguage.CHINESE) chinese else english

    return ShipInfo(
        shipId = shipId,
        name = resolved ?: "#$shipId",
        tier = level ?: 0,
        type = shipType.orEmpty(),
        nation = ShipNation.fromApi(country),
        imageUrl = imgSmall?.takeIf { it.isNotBlank() },
    )
}

internal fun YuyukoUserInfoData.toAccountOverview(
    fallbackAccountId: Long,
    language: ShipNameLanguage,
    server: GameServer,
): AccountOverview {
    val user = userInfo
    val name = user?.userName
    if (user == null || name.isNullOrBlank()) throw ApiException.NotFound

    return AccountOverview(
        account = AccountInfo(
            accountId = user.accountId ?: fallbackAccountId,
            nickname = name,
            createdAt = user.accountCreateTime,
            lastBattleTime = lastBattleTime,
            dogTagUrl = user.dogTag?.takeIf { it.isNotBlank() },
        ),
        modes = battleTypeInfo?.toModeStats().orEmpty(),
        shipTypes = shipTypeInfo?.toCategories { CategoryKey.ShipType(it) }.orEmpty(),
        tiers = levelInfo
            ?.toCategories { rawTier -> rawTier.toIntOrNull()?.let(CategoryKey::Tier) }
            .orEmpty(),
    )
}

internal fun YuyukoRecentDay.toDailyStats(zoneId: ZoneId): DailyStats? {
    val timestamp = recordTime?.takeIf { it > 0 } ?: return null
    val instant = Instant.ofEpochMilli(timestamp)
    return DailyStats(
        recordedAt = timestamp,
        day = instant.atZone(zoneId).toLocalDate(),
        modes = battleTypeInfoList?.toModeStats().orEmpty(),
    )
}
