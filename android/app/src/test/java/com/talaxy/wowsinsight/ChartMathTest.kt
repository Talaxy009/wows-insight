package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.ui.components.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {
    @Test
    fun ratingDomainLeavesSixtyPointsAboveAndBelow() {
        val domain = ChartMath.trendDomain(listOf(1_000.0, 2_000.0), ChartMetric.RATING)
        assertEquals(750.0, domain.start, 0.0001)
        assertEquals(2_250.0, domain.endInclusive, 0.0001)
    }

    @Test
    fun winRateDomainLetsTheLineBreatheBeyondZeroAndHundred() {
        val flat = ChartMath.trendDomain(listOf(50.0, 50.0), ChartMetric.WIN_RATE)
        assertEquals(42.0, flat.start, 0.0001)
        assertEquals(58.0, flat.endInclusive, 0.0001)

        val extremes = ChartMath.trendDomain(listOf(0.0, 100.0), ChartMetric.WIN_RATE)
        assertEquals(-5.0, extremes.start, 0.0001)
        assertEquals(105.0, extremes.endInclusive, 0.0001)
    }

    @Test
    fun percentageTicksStartAtPositiveZeroBelowTheAxis() {
        // The win-rate domain can dip under zero, and `ceil` of that ratio is
        // -0.0, which the axis formatter printed as "-0%".
        val ticks = ChartMath.percentageTicks(-8.0, 105.0)
        assertEquals("0.0", ticks.first().toString())
        assertEquals(listOf(0.0, 25.0, 50.0, 75.0, 100.0), ticks)
    }

    @Test
    fun niceTicksStayInsideTheDomain() {
        val ticks = ChartMath.niceTicks(750.0, 2_250.0)
        assertTrue(ticks.isNotEmpty())
        assertTrue(ticks.first() >= 750.0)
        assertTrue(ticks.last() <= 2_250.0)
        assertTrue(ticks.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun breakdownScaleAndFractionAreSafeForUnknownRatings() {
        val categories = listOf(
            TestData.category(CategoryKey.ShipType("Cruiser"), BattleMode.PVP, TestData.mode(rating = 2_000)),
            TestData.category(CategoryKey.ShipType("Battleship"), BattleMode.PVP, TestData.mode(rating = 1_000)),
        )
        val scale = ChartMath.breakdownScale(categories, BattleMode.PVP)

        assertEquals(2_000.0, scale, 0.0001)
        assertEquals(1.0f, ChartMath.barFraction(2_000, scale), 0.0001f)
        assertEquals(0.5f, ChartMath.barFraction(1_000, scale), 0.0001f)
        assertEquals(0f, ChartMath.barFraction(-1, scale), 0.0001f)
    }
}
