package com.talaxy.wowsinsight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.data.model.DailyStats
import com.talaxy.wowsinsight.data.model.ModeStats
import com.talaxy.wowsinsight.ui.theme.ratingColor
import com.talaxy.wowsinsight.ui.util.StatFormat
import com.talaxy.wowsinsight.ui.util.labelRes
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private data class TrendPoint(
    val day: LocalDate,
    val stats: ModeStats,
)

/**
 * The rating or win rate of every recorded day, oldest on the left.
 *
 * Drawn with Compose Canvas rather than a chart library: the data is a single
 * series and the card only needs axes, an area, a line and points. That keeps
 * the visual language consistent with the rest of the app and avoids pulling a
 * dependency for one screen.
 */
@Composable
fun TrendChart(
    days: List<DailyStats>,
    mode: BattleMode,
    metric: ChartMetric,
    modifier: Modifier = Modifier,
) {
    val points = days.mapNotNull { day ->
        day.modes[mode]?.let { TrendPoint(day.day, it) }
    }
    if (points.isEmpty()) return

    val dark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val pointColors = points.map { point ->
        if (metric == ChartMetric.RATING) {
            ratingColor(point.stats.rating.tier, dark)
        } else {
            primary
        }
    }
    val first = points.first()
    val last = points.last()
    val latestText = if (metric == ChartMetric.RATING) {
        StatFormat.ratingText(last.stats.rating)
    } else {
        StatFormat.percent(last.stats.stats.winRate, digits = 1)
    }
    val accessibility = stringResource(
        R.string.player_charts_trend_a11y,
        stringResource(metric.labelRes()),
        StatFormat.date(first.day),
        StatFormat.date(last.day),
        latestText,
        StatFormat.date(last.day),
    )

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = axisColor)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .semantics { contentDescription = accessibility },
    ) {
        val leftGutter = 44.dp.toPx()
        val rightGutter = 10.dp.toPx()
        val topGutter = 10.dp.toPx()
        val bottomGutter = 26.dp.toPx()
        val plotLeft = leftGutter
        val plotRight = size.width - rightGutter
        val plotTop = topGutter
        val plotBottom = size.height - bottomGutter
        if (plotRight <= plotLeft || plotBottom <= plotTop) return@Canvas

        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop
        val values = points.map { metric.valueOf(it.stats) }
        val domain = ChartMath.trendDomain(values, metric)
        val lower = domain.start
        val upper = domain.endInclusive
        val span = upper - lower

        val firstDay = points.first().day
        val lastDay = points.last().day
        val totalDays = max(1L, ChronoUnit.DAYS.between(firstDay, lastDay))
        val horizontalInset = 0.04f

        fun xFor(day: LocalDate): Float {
            val ratio = ChronoUnit.DAYS.between(firstDay, day).toFloat() / totalDays
            return plotLeft + plotWidth * (horizontalInset + ratio * (1 - 2 * horizontalInset))
        }

        fun yFor(value: Double): Float {
            val ratio = ((value - lower) / span).toFloat().coerceIn(0f, 1f)
            return plotTop + (1f - ratio) * plotHeight
        }

        // Horizontal grid and y-axis labels.
        val ticks = if (metric == ChartMetric.WIN_RATE) {
            ChartMath.percentageTicks(lower, upper)
        } else {
            ChartMath.niceTicks(lower, upper)
        }
        ticks.forEach { tick ->
            if (tick < lower || tick > upper) return@forEach
            val y = yFor(tick)
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            val text = if (metric == ChartMetric.RATING) {
                StatFormat.integer(tick.roundToInt())
            } else {
                StatFormat.percent(tick, digits = 0)
            }
            val layout = textMeasurer.measure(AnnotatedString(text), style = labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = max(0f, plotLeft - 8.dp.toPx() - layout.size.width),
                    y = (y - layout.size.height / 2f)
                        .coerceIn(0f, plotBottom - layout.size.height),
                ),
            )
        }

        // X-axis labels, at most four, one per whole day.
        val candidateDays = buildList {
            add(firstDay)
            val daySpan = ChronoUnit.DAYS.between(firstDay, lastDay)
            if (daySpan > 0) {
                val step = max(1, ceil(daySpan / 3.0).toInt())
                var day = firstDay.plusDays(step.toLong())
                while (day.isBefore(lastDay)) {
                    add(day)
                    day = day.plusDays(step.toLong())
                }
                add(lastDay)
            }
        }
        var lastRight = -Float.MAX_VALUE
        candidateDays.forEach { day ->
            val text = StatFormat.chartDay(day)
            val layout = textMeasurer.measure(AnnotatedString(text), style = labelStyle)
            val centerX = xFor(day)
            val leftX = (centerX - layout.size.width / 2f)
                .coerceIn(0f, size.width - layout.size.width)
            if (leftX >= lastRight + 6.dp.toPx() || day == lastDay) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(leftX, plotBottom + 6.dp.toPx()),
                )
                lastRight = leftX + layout.size.width
            }
        }

        val offsets = points.map { point ->
            Offset(xFor(point.day), yFor(metric.valueOf(point.stats)))
        }

        val linePath = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            for (index in 1 until offsets.size) {
                val previous = offsets[index - 1]
                val current = offsets[index]
                val middleX = (previous.x + current.x) / 2f
                cubicTo(middleX, previous.y, middleX, current.y, current.x, current.y)
            }
        }
        // The area is the same curve closed along the bottom of the plot.
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(offsets.last().x, plotBottom)
            lineTo(offsets.first().x, plotBottom)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0.02f)),
                startY = plotTop,
                endY = plotBottom,
            ),
        )
        drawPath(
            path = linePath,
            color = primary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        offsets.forEachIndexed { index, point ->
            drawCircle(
                color = surfaceColor,
                radius = 5.dp.toPx(),
                center = point,
            )
            drawCircle(
                color = pointColors[index],
                radius = 3.2.dp.toPx(),
                center = point,
            )
        }
    }
}
