package com.talaxy.wowsinsight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.BattleRecord
import com.talaxy.wowsinsight.data.model.BattleStats
import com.talaxy.wowsinsight.data.model.ModeStats
import com.talaxy.wowsinsight.ui.util.StatFormat
import com.talaxy.wowsinsight.ui.util.titleRes

@Composable
fun StatsSections(
    stats: ModeStats,
    records: List<BattleRecord>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatsOverviewCard(stats = stats.stats)
        DetailedStatsCard(stats = stats.stats)
        if (records.isNotEmpty()) {
            RecordsCard(records = records)
        }
    }
}

@Composable
private fun StatsOverviewCard(stats: BattleStats) {
    val items = listOf(
        OverviewItem(
            symbol = Icons.Outlined.Flag,
            value = StatFormat.integer(stats.battleCount),
            label = stringResource(R.string.player_stats_battles),
        ),
        OverviewItem(
            symbol = Icons.Outlined.Percent,
            value = StatFormat.percent(stats.winRate),
            label = stringResource(R.string.player_stats_win_rate),
        ),
        OverviewItem(
            symbol = Icons.Outlined.LocalFireDepartment,
            value = StatFormat.decimal(stats.averageDamage),
            label = stringResource(R.string.player_stats_avg_damage),
        ),
        OverviewItem(
            symbol = Icons.Outlined.GpsFixed,
            value = StatFormat.decimal(stats.averageFrags, digits = 2),
            label = stringResource(R.string.player_stats_avg_frags),
        ),
        OverviewItem(
            symbol = Icons.Outlined.Bolt,
            value = StatFormat.decimal(stats.averageExperience),
            label = stringResource(R.string.player_stats_avg_xp),
        ),
        OverviewItem(
            symbol = Icons.Outlined.Favorite,
            value = StatFormat.percent(stats.survivalRate, digits = 1),
            label = stringResource(R.string.player_stats_survival_rate),
        ),
    )

    SectionCard(title = stringResource(R.string.player_stats_overview)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            items.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { item ->
                        StatCell(item = item, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class OverviewItem(
    val symbol: ImageVector,
    val value: String,
    val label: String,
)

@Composable
private fun StatCell(
    item: OverviewItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = item.symbol,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailedStatsCard(stats: BattleStats) {
    val items = listOf(
        DetailItem(
            title = stringResource(R.string.player_details_win_draw_loss),
            value = listOf(
                StatFormat.integer(stats.winCount),
                StatFormat.integer(stats.drawCount),
                StatFormat.integer(stats.lossCount),
            ).joinToString(" / "),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_total_damage),
            value = StatFormat.integer(stats.damage),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_total_xp),
            value = StatFormat.integer(stats.experience),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_total_frags),
            value = StatFormat.integer(stats.fragCount),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_hit_rate),
            value = StatFormat.percent(stats.hitRate, digits = 1),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_kd),
            value = StatFormat.decimal(stats.killDeathRatio, digits = 2),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_potential_damage),
            value = StatFormat.integer(stats.potentialDamage),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_spotting_damage),
            value = StatFormat.integer(stats.scoutingDamage),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_ships_spotted),
            value = StatFormat.integer(stats.spottedCount),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_planes_destroyed),
            value = StatFormat.integer(stats.planesDestroyed),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_survived_battles),
            value = StatFormat.integer(stats.survivedCount),
        ),
        DetailItem(
            title = stringResource(R.string.player_details_survived_win_rate),
            value = StatFormat.percent(stats.survivedWinRate, digits = 1),
        ),
    )

    SectionCard(title = stringResource(R.string.player_details_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { item ->
                        DetailStat(item = item, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class DetailItem(
    val title: String,
    val value: String,
)

@Composable
private fun DetailStat(
    item: DetailItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecordsCard(records: List<BattleRecord>) {
    SectionCard(title = stringResource(R.string.player_records_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            records.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { record ->
                        RecordCell(record = record, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecordCell(
    record: BattleRecord,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(record.kind.titleRes()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = StatFormat.integer(record.value),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (record.shipName != null) {
            Text(
                text = record.shipName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
