package com.talaxy.wowsinsight.ui.ship

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.BattleRecords
import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.data.model.ShipPerformance
import com.talaxy.wowsinsight.ui.components.EmptyState
import com.talaxy.wowsinsight.ui.components.ErrorState
import com.talaxy.wowsinsight.ui.components.LoadingState
import com.talaxy.wowsinsight.ui.components.RatingBadge
import com.talaxy.wowsinsight.ui.components.SectionCard
import com.talaxy.wowsinsight.ui.components.ShipMetaText
import com.talaxy.wowsinsight.ui.components.StatsSections
import com.talaxy.wowsinsight.ui.util.StatFormat
import com.talaxy.wowsinsight.ui.util.UiText
import com.talaxy.wowsinsight.ui.util.typeLabelRes

/**
 * One ship of the current player in the battle type selected on the player
 * page. The data is owned by the parent PlayerViewModel so switching back does
 * not reload the report.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipDetailScreen(
    ship: ShipPerformance?,
    mode: BattleMode,
    isLoading: Boolean,
    error: UiText?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ship?.ship?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            ship != null -> ShipDetailContent(ship = ship, mode = mode, modifier = modifier)
            error != null -> ErrorState(
                title = stringResource(R.string.player_error_title),
                message = error,
                onRetry = onRetry,
                modifier = modifier,
            )

            isLoading -> LoadingState(
                title = stringResource(R.string.player_loading),
                modifier = modifier,
            )

            else -> EmptyState(
                icon = Icons.Outlined.DirectionsBoat,
                title = stringResource(R.string.ship_not_found_title),
                message = stringResource(R.string.ship_not_found_message),
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ShipDetailContent(
    ship: ShipPerformance,
    mode: BattleMode,
    modifier: Modifier = Modifier,
) {
    val stats = ship.stats(mode)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            ShipHeaderCard(ship = ship, mode = mode)
        }
        if (stats != null) {
            item(key = "stats") {
                StatsSections(
                    stats = stats,
                    records = BattleRecords.list(stats.stats),
                )
            }
        } else {
            item(key = "empty") {
                SectionCard {
                    Text(
                        text = stringResource(R.string.player_ships_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShipHeaderCard(ship: ShipPerformance, mode: BattleMode) {
    val stats = ship.stats(mode)
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShipImage(
                url = ship.ship.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = ship.ship.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ShipMetaText(
                        tierLabel = ship.ship.tierLabel,
                        typeRes = ship.ship.typeLabelRes(),
                        nation = ship.ship.nation,
                    )
                    Text(
                        text = stringResource(
                            R.string.ship_detail_last_battle,
                            StatFormat.date(stats?.lastBattleTime),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RatingBadge(rating = stats?.rating ?: PlayerRating.Unknown)
            }
        }
    }
}

@Composable
private fun ShipImage(url: String?, modifier: Modifier = Modifier) {
    if (url == null) {
        ShipImagePlaceholder(modifier)
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = stringResource(R.string.player_ship_image),
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        },
        success = { SubcomposeAsyncImageContent() },
        error = { ShipImagePlaceholder(Modifier.fillMaxSize()) },
    )
}

@Composable
private fun ShipImagePlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.DirectionsBoat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
