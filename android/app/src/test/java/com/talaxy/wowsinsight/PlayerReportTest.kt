package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.AccountInfo
import com.talaxy.wowsinsight.data.model.AccountOverview
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.BattleRecordKind
import com.talaxy.wowsinsight.data.model.BattleRecordValue
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.ChartBreakdown
import com.talaxy.wowsinsight.data.model.DailyStats
import com.talaxy.wowsinsight.data.model.PlayerReport
import com.talaxy.wowsinsight.data.model.ShipSort
import com.talaxy.wowsinsight.data.model.ShipStat
import com.talaxy.wowsinsight.data.model.ranked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlayerReportTest {
    private val account = AccountInfo(
        accountId = 3_002_439_652,
        nickname = "Yamato",
        createdAt = 1_723_289_938,
        lastBattleTime = 1_767_211_200,
        dogTagUrl = null,
    )

    private val pvp = TestData.mode()
    private val rankedMode = TestData.mode(rating = 1_850)

    private val ships = listOf(
        ShipStat(
            ship = TestData.ship(id = 1, name = "Alpha", tier = 10),
            modes = mapOf(BattleMode.PVP to pvp),
        ),
        ShipStat(
            ship = TestData.ship(id = 2, name = "Bravo", tier = 8),
            modes = mapOf(BattleMode.PVP to TestData.mode(lastBattleTime = 1_800_000_000)),
        ),
        ShipStat(
            ship = TestData.ship(id = 3, name = "Charlie", tier = 6),
            modes = emptyMap(),
        ),
    )

    private val shipTypes = listOf(
        TestData.category(CategoryKey.ShipType("Cruiser"), BattleMode.PVP, pvp),
        TestData.category(
            CategoryKey.ShipType("Battleship"),
            BattleMode.RANKED,
            TestData.mode(stats = TestData.stats(battles = 0)),
        ),
    )

    private val tiers = listOf(
        TestData.category(CategoryKey.Tier(10), BattleMode.PVP, pvp),
    )

    private val report = PlayerReport.create(
        overview = AccountOverview(
            account = account,
            modes = mapOf(
                BattleMode.PVP to pvp,
                BattleMode.RANKED to rankedMode,
                BattleMode.COOP to TestData.mode(stats = TestData.stats(battles = 0)),
            ),
            shipTypes = shipTypes,
            tiers = tiers,
        ),
        shipStats = ships,
        dailyStats = listOf(
            DailyStats(1, LocalDate.of(2026, 1, 1), mapOf(BattleMode.PVP to pvp)),
            DailyStats(2, LocalDate.of(2026, 1, 2), mapOf(BattleMode.RANKED to rankedMode)),
        ),
    )

    @Test
    fun onlyPlayedModesAreKeptInPickerOrder() {
        assertEquals(listOf(BattleMode.PVP, BattleMode.RANKED), report.modes)
        assertEquals(BattleMode.PVP, report.defaultMode)
        assertNull(report.stats(BattleMode.COOP))
    }

    @Test
    fun dailyStatsAreFilteredByMode() {
        assertEquals(1, report.dailyStats(BattleMode.PVP).size)
        assertEquals(1, report.dailyStats(BattleMode.RANKED).size)
        assertTrue(report.dailyStats(BattleMode.CLAN).isEmpty())
    }

    @Test
    fun trendOnlyCountsDaysInsideTheSevenDayAxis() {
        fun reportWith(vararg days: LocalDate) = PlayerReport.create(
            overview = AccountOverview(
                account = account,
                modes = mapOf(BattleMode.PVP to pvp),
                shipTypes = emptyList(),
                tiers = emptyList(),
            ),
            shipStats = emptyList(),
            dailyStats = days.mapIndexed { index, day ->
                DailyStats(index.toLong(), day, mapOf(BattleMode.PVP to pvp))
            },
        )

        // 11/11 is older than the axis that ends on 11/19, so only two days remain.
        val stale = reportWith(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 19))
        assertEquals(2, stale.trendDays(BattleMode.PVP).size)
        assertTrue(stale.hasTrend(BattleMode.PVP))

        val lone = reportWith(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 19))
        assertEquals(1, lone.trendDays(BattleMode.PVP).size)
        assertFalse(lone.hasTrend(BattleMode.PVP))
    }

    @Test
    fun categoriesAreFilteredAndSorted() {
        val categories = report.categories(ChartBreakdown.SHIP_TYPE, BattleMode.PVP)
        assertEquals(1, categories.size)
        assertEquals(CategoryKey.ShipType("Cruiser"), categories.first().key)
        assertEquals(1, report.categories(ChartBreakdown.TIER, BattleMode.PVP).size)
    }

    @Test
    fun recordsResolveTheirShipNameFromTheWholeCatalog() {
        val stats = TestData.stats(
            records = mapOf(
                BattleRecordKind.MAX_DAMAGE to BattleRecordValue(200_000L, shipId = 3),
            ),
        )
        val withRecord = PlayerReport.create(
            overview = AccountOverview(
                account = account,
                modes = mapOf(BattleMode.PVP to TestData.mode(stats = stats)),
                shipTypes = emptyList(),
                tiers = emptyList(),
            ),
            shipStats = ships,
            dailyStats = emptyList(),
        )

        val record = withRecord.records(BattleMode.PVP).single()
        assertEquals("Charlie", record.shipName)
        assertEquals(200_000L, record.value)
    }

    @Test
    fun shipsAreRankedByTheSelectedMetricWithANameTieBreak() {
        val byLastBattle = report.ships.ranked(BattleMode.PVP, ShipSort.LAST_BATTLE)
        assertEquals(listOf("Bravo", "Alpha"), byLastBattle.map { it.ship.name })

        val byRating = report.ships.ranked(BattleMode.PVP, ShipSort.RATING)
        assertEquals(2, byRating.size)
    }
}
