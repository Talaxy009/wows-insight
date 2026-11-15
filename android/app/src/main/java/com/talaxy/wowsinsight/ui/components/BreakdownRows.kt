package com.talaxy.wowsinsight.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.CategoryStats
import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.data.model.ShipInfo
import com.talaxy.wowsinsight.ui.theme.ratingColor
import com.talaxy.wowsinsight.ui.util.StatFormat
import com.talaxy.wowsinsight.ui.util.labelRes
import com.talaxy.wowsinsight.ui.util.shipTypeLabelRes

/**
 * The personal rating of every ship type or tier the player fought in.
 *
 * Rows are drawn directly instead of using a horizontal chart so the category
 * label, bar and value always line up, including on narrow screens and large
 * font scales.
 */
@Composable
fun BreakdownRows(
    categories: List<CategoryStats>,
    mode: BattleMode,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val scale = ChartMath.breakdownScale(categories, mode)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.forEach { category ->
            val rating = category.modes[mode]?.rating ?: PlayerRating.Unknown
            val fraction = ChartMath.barFraction(rating.value, scale)
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                label = "rating breakdown bar",
            )
            val color = ratingColor(rating.tier, dark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = categoryLabel(category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(88.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
                Text(
                    text = StatFormat.ratingText(rating),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.width(46.dp),
                )
            }
        }
    }
}

@Composable
private fun categoryLabel(category: CategoryStats): String = when (val key = category.key) {
    is CategoryKey.ShipType -> stringResource(shipTypeLabelRes(key.code))
    is CategoryKey.Tier -> ShipInfo.tierLabel(key.tier)
}
