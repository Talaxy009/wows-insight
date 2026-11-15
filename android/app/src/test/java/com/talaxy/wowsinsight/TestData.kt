package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.BattleRecordKind
import com.talaxy.wowsinsight.data.model.BattleRecordValue
import com.talaxy.wowsinsight.data.model.BattleStats
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.ModeStats
import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.data.model.ShipInfo
import com.talaxy.wowsinsight.data.model.ShipNation

object TestData {
    fun stats(
        battles: Int = 100,
        wins: Int = 50,
        losses: Int = 40,
        survived: Int = 30,
        survivedWins: Int = 20,
        damage: Long = 1_000_000L,
        frags: Int = 140,
        xp: Long = 120_000L,
        records: Map<BattleRecordKind, BattleRecordValue> = emptyMap(),
    ) = BattleStats(
        battleCount = battles,
        winCount = wins,
        lossCount = losses,
        survivedCount = survived,
        survivedWinCount = survivedWins,
        damage = damage,
        scoutingDamage = 50_000L,
        fragCount = frags,
        experience = xp,
        planesDestroyed = 7,
        spottedCount = 20,
        potentialDamage = 2_000_000L,
        hitRate = 38.3,
        records = records,
    )

    fun mode(
        stats: BattleStats = stats(),
        rating: Int = 1_500,
        lastBattleTime: Int? = 1_700_000_000,
    ) = ModeStats(
        stats = stats,
        rating = if (rating < 0) PlayerRating.Unknown else PlayerRating(rating),
        lastBattleTime = lastBattleTime,
    )

    fun ship(
        id: Long = 1L,
        name: String = "Test Ship",
        tier: Int = 10,
        type: String = "Cruiser",
    ) = ShipInfo(
        shipId = id,
        name = name,
        tier = tier,
        type = type,
        nation = ShipNation.JAPAN,
        imageUrl = null,
    )

    fun category(
        key: CategoryKey,
        mode: BattleMode = BattleMode.PVP,
        stats: ModeStats = mode(),
    ) = CategoryStats(key = key, modes = mapOf(mode to stats))
}
