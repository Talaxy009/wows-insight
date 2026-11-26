package com.talaxy.wowsinsight.ui.components

import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.ChartMetric
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure chart maths, separated from Compose so it can be unit tested. */
object ChartMath {
    private const val MAX_TICKS = 12

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

    /** Round y ticks inside [lower]..[upper], about five of them. */
    fun niceTicks(lower: Double, upper: Double): List<Double> {
        if (upper <= lower) return listOf(lower)
        val rawStep = (upper - lower) / 4
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val normalized = rawStep / magnitude
        val niceNormalized = when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 2.5 -> 2.5
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }
        return ticksBetween(lower, upper, niceNormalized * magnitude).ifEmpty { listOf(lower) }
    }

    /** Percentage ticks at a readable 5 / 10 / 25 point step. */
    fun percentageTicks(lower: Double, upper: Double): List<Double> {
        val span = upper - lower
        val step = when {
            span <= 20.0 -> 5.0
            span <= 60.0 -> 10.0
            else -> 25.0
        }
        return ticksBetween(lower, upper, step)
    }

    /** Multiples of [step] inside [lower]..[upper], never more than a dozen. */
    private fun ticksBetween(lower: Double, upper: Double, step: Double): List<Double> {
        val ticks = mutableListOf<Double>()
        var value = (ceil(lower / step) * step).withoutNegativeZero()
        while (value <= upper + step * 0.001 && ticks.size < MAX_TICKS) {
            ticks += value
            value += step
        }
        return ticks
    }

    /**
     * `ceil` answers -0.0 for a small negative input, and a formatter prints
     * that as "-0%"; a tick on zero is zero.
     */
    private fun Double.withoutNegativeZero(): Double = if (this == 0.0) 0.0 else this

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
