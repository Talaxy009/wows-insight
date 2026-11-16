package com.talaxy.wowsinsight.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.ChartBreakdown
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.data.model.DailyStats
import com.talaxy.wowsinsight.data.model.PlayerReport
import com.talaxy.wowsinsight.ui.components.BreakdownRows
import com.talaxy.wowsinsight.ui.components.SectionCard
import com.talaxy.wowsinsight.ui.components.SegmentedChoice
import com.talaxy.wowsinsight.ui.components.TrendChart
import com.talaxy.wowsinsight.ui.util.labelRes

/**
 * The two chart cards of the player page. A card with nothing to draw stays
 * hidden, so a player the service has no record of only loses the card instead
 * of seeing an empty chart.
 */
@Composable
fun PlayerChartsSection(
    report: PlayerReport,
    mode: BattleMode,
    metric: ChartMetric,
    breakdown: ChartBreakdown,
    onMetricChange: (ChartMetric) -> Unit,
    onBreakdownChange: (ChartBreakdown) -> Unit,
) {
    val days = report.dailyStats(mode)
    val shipTypeCategories = report.categories(ChartBreakdown.SHIP_TYPE, mode)
    val tierCategories = report.categories(ChartBreakdown.TIER, mode)
    val hasTrend = days.size > 1
    val hasBreakdown = shipTypeCategories.isNotEmpty() || tierCategories.isNotEmpty()

    if (!hasTrend && !hasBreakdown) return

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (hasTrend) {
            TrendCard(
                days = days,
                window = report.dailyStats,
                mode = mode,
                metric = metric,
                onMetricChange = onMetricChange,
            )
        }
        if (hasBreakdown) {
            BreakdownCard(
                shipTypeCategories = shipTypeCategories,
                tierCategories = tierCategories,
                mode = mode,
                breakdown = breakdown,
                onBreakdownChange = onBreakdownChange,
            )
        }
    }
}

@Composable
private fun TrendCard(
    days: List<DailyStats>,
    window: List<DailyStats>,
    mode: BattleMode,
    metric: ChartMetric,
    onMetricChange: (ChartMetric) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.player_charts_trend_title),
        trailing = {
            SegmentedChoice(
                options = ChartMetric.entries,
                selected = metric,
                onSelect = onMetricChange,
                label = { stringResource(it.labelRes()) },
            )
        },
    ) {
        TrendChart(days = days, window = window, mode = mode, metric = metric)
    }
}

@Composable
private fun BreakdownCard(
    shipTypeCategories: List<CategoryStats>,
    tierCategories: List<CategoryStats>,
    mode: BattleMode,
    breakdown: ChartBreakdown,
    onBreakdownChange: (ChartBreakdown) -> Unit,
) {
    val categories = if (breakdown == ChartBreakdown.SHIP_TYPE) {
        shipTypeCategories
    } else {
        tierCategories
    }

    SectionCard(
        title = stringResource(R.string.player_charts_breakdown_title),
        trailing = {
            SegmentedChoice(
                options = ChartBreakdown.entries,
                selected = breakdown,
                onSelect = onBreakdownChange,
                label = { stringResource(it.labelRes()) },
            )
        },
    ) {
        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.player_charts_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            BreakdownRows(categories = categories, mode = mode)
        }
    }
}
