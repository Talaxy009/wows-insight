package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.BattleStats
import org.junit.Assert.assertEquals
import org.junit.Test

class BattleStatsTest {
    @Test
    fun derivedValuesUseTheApiTotals() {
        val stats = TestData.stats()

        assertEquals(10, stats.drawCount)
        assertEquals(50.0, stats.winRate, 0.0001)
        assertEquals(10_000.0, stats.averageDamage, 0.0001)
        assertEquals(1.4, stats.averageFrags, 0.0001)
        assertEquals(1_200.0, stats.averageExperience, 0.0001)
        assertEquals(30.0, stats.survivalRate, 0.0001)
        assertEquals(20.0 / 30.0 * 100.0, stats.survivedWinRate, 0.0001)
        // 140 kills over 70 deaths.
        assertEquals(2.0, stats.killDeathRatio, 0.0001)
    }

    @Test
    fun zeroBattlesNeverDivideByZero() {
        val stats = BattleStats.Empty
        assertEquals(0.0, stats.winRate, 0.0)
        assertEquals(0.0, stats.averageDamage, 0.0)
        assertEquals(0.0, stats.killDeathRatio, 0.0)
        assertEquals(0, stats.drawCount)
    }

    @Test
    fun drawCountIsClampedWhenTheApiCountsAreInconsistent() {
        val stats = TestData.stats(battles = 10, wins = 8, losses = 5)
        assertEquals(0, stats.drawCount)
    }
}
