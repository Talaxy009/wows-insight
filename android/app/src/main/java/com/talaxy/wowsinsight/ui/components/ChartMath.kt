package com.talaxy.wowsinsight.ui.components

import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.ChartMetric
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure chart maths, separated from Compose so it can be unit tested. */
object ChartMath {

    /**
     * Room above and below the line so a flat series does not sit on the axis
     * and a point on 0 % or 100 % is not cut in half.
     */
    fun trendDomain(values: List<Double>, metric: ChartMetric): ClosedFloatingPointRange<Double> {
        if (values.isEmpty()) return 0.0..1.0
        val low = values.min()
        val high = values.max()
        val padding = max((high - low) * 0.25, if (metric == ChartMetric.RATING) 60.0 else 8.0)
        return when (metric) {
            ChartMetric.RATING -> {
                val lower = max(low - padding, 0.0)
                val upper = high + padding
                lower..max(upper, lower + 1.0)
            }

            ChartMetric.WIN_RATE -> {
                val lower = max(low - padding, -5.0)
                val upper = min(high + padding, 105.0)
                lower..max(upper, lower + 1.0)
            }
        }
    }

    /** One tick per whole day, at most about four labels on the x axis. */
    fun dayStride(first: LocalDate, last: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(first, last).toInt()
        return max(1, ceil(days / 4.0).toInt())
    }

    /** Round y ticks inside [lower]..[upper]. */
    fun niceTicks(lower: Double, upper: Double, desiredCount: Int = 5): List<Double> {
        if (upper <= lower) return listOf(lower)
        val rawStep = (upper - lower) / (desiredCount - 1).coerceAtLeast(1)
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val normalized = rawStep / magnitude
        val niceNormalized = when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 2.5 -> 2.5
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }
        val step = niceNormalized * magnitude
        var value = ceil(lower / step) * step
        val ticks = mutableListOf<Double>()
        while (value <= upper + step * 0.001 && ticks.size < 12) {
            ticks += value
            value += step
        }
        return if (ticks.isEmpty()) listOf(lower) else ticks
    }

    /** Percentage ticks at a readable 5 / 10 / 25 point step. */
    fun percentageTicks(lower: Double, upper: Double): List<Double> {
        val span = upper - lower
        val step = when {
            span <= 20.0 -> 5.0
            span <= 60.0 -> 10.0
            else -> 25.0
        }
        var value = ceil(lower / step) * step
        val ticks = mutableListOf<Double>()
        while (value <= upper + 0.001 && ticks.size < 12) {
            ticks += value
            value += step
        }
        return ticks
    }

    fun breakdownScale(categories: List<CategoryStats>, mode: BattleMode): Double {
        val maximum = categories.maxOfOrNull { category ->
            category.modes[mode]?.rating?.value?.coerceAtLeast(0) ?: 0
        } ?: 0
        return max(1.0, maximum.toDouble())
    }

    /** Value of one split segment as a 0..1 fraction. */
    fun barFraction(ratingValue: Int, scale: Double): Float {
        if (scale <= 0.0) return 0f
        return (ratingValue.coerceAtLeast(0) / scale).toFloat().coerceIn(0f, 1f)
    }
}
