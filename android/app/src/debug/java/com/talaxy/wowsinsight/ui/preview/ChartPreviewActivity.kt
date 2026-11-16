package com.talaxy.wowsinsight.ui.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.BattleStats
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.data.model.DailyStats
import com.talaxy.wowsinsight.data.model.ModeStats
import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.ui.components.BreakdownRows
import com.talaxy.wowsinsight.ui.components.SectionCard
import com.talaxy.wowsinsight.ui.components.TrendChart
import com.talaxy.wowsinsight.ui.theme.WoWsInsightTheme
import java.time.LocalDate
import java.time.ZoneId

/**
 * Debug-only visual harness for the two charts.
 *
 * The public API does not currently return daily records for most accounts, so
 * this activity keeps a representative series available for layout checks on a
 * device. It is excluded from release builds because it lives in `src/debug`.
 */
class ChartPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WoWsInsightTheme {
                ChartPreview()
            }
        }
    }
}

@Composable
private fun ChartPreview() {
    val days = sampleDays()
    val categories = sampleCategories()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Chart preview (debug)",
                style = MaterialTheme.typography.titleLarge,
            )
            SectionCard(title = "Rating trend") {
                TrendChart(
                    days = days,
                    window = days,
                    mode = BattleMode.PVP,
                    metric = ChartMetric.RATING,
                )
            }
            SectionCard(title = "Win-rate trend") {
                TrendChart(
                    days = days,
                    window = days,
                    mode = BattleMode.PVP,
                    metric = ChartMetric.WIN_RATE,
                )
            }
            SectionCard(title = "Rating breakdown") {
                BreakdownRows(categories = categories, mode = BattleMode.PVP)
            }
        }
    }
}

private fun sampleDays(): List<DailyStats> {
    val zone = ZoneId.systemDefault()
    return (0 until 14).map { index ->
        val day = LocalDate.now(zone).minusDays((13 - index).toLong())
        val battles = 8 + index % 5
        val wins = (battles * (0.38 + index * 0.012)).toInt()
        val stats = BattleStats(
            battleCount = battles,
            winCount = wins,
            lossCount = battles - wins,
            survivedCount = battles / 3,
            survivedWinCount = wins / 3,
            damage = (battles * (42_000 + index * 900)).toLong(),
            scoutingDamage = 0L,
            fragCount = battles,
            experience = (battles * 1_100).toLong(),
            planesDestroyed = 0,
            spottedCount = 0,
            potentialDamage = 0L,
            hitRate = 38.0,
            records = emptyMap(),
        )
        DailyStats(
            recordedAt = day.atStartOfDay(zone).toInstant().toEpochMilli(),
            day = day,
            modes = mapOf(
                BattleMode.PVP to ModeStats(
                    stats = stats,
                    rating = PlayerRating(1_330 + index * 43),
                    lastBattleTime = null,
                ),
            ),
        )
    }
}

private fun sampleCategories(): List<CategoryStats> {
    fun category(code: String, rating: Int): CategoryStats = CategoryStats(
        key = CategoryKey.ShipType(code),
        modes = mapOf(
            BattleMode.PVP to ModeStats(
                stats = BattleStats.Empty.copy(battleCount = 40, winCount = 22),
                rating = PlayerRating(rating),
                lastBattleTime = null,
            ),
        ),
    )

    return listOf(
        category("Destroyer", 1_340),
        category("Cruiser", 2_560),
        category("Battleship", 1_860),
        category("AirCarrier", 927),
        category("Submarine", 1_505),
    )
}
