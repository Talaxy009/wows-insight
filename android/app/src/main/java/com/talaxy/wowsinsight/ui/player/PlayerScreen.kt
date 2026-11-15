package com.talaxy.wowsinsight.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.model.AccountInfo
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.ChartBreakdown
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.data.model.PlayerReport
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.model.ShipPerformance
import com.talaxy.wowsinsight.data.model.ShipSort
import com.talaxy.wowsinsight.data.model.ranked
import com.talaxy.wowsinsight.ui.components.EndAlignedDropdownMenu
import com.talaxy.wowsinsight.ui.components.ErrorState
import com.talaxy.wowsinsight.ui.components.FavoriteStarButton
import com.talaxy.wowsinsight.ui.components.InfoRow
import com.talaxy.wowsinsight.ui.components.LoadingState
import com.talaxy.wowsinsight.ui.components.RatingBadge
import com.talaxy.wowsinsight.ui.components.SelectedMenuItem
import com.talaxy.wowsinsight.ui.components.SectionCard
import com.talaxy.wowsinsight.ui.components.StatsSections
import com.talaxy.wowsinsight.ui.components.serverAccountLabel
import com.talaxy.wowsinsight.ui.theme.ratingColor
import com.talaxy.wowsinsight.ui.util.StatFormat
import com.talaxy.wowsinsight.ui.util.labelRes
import com.talaxy.wowsinsight.ui.util.resolve
import com.talaxy.wowsinsight.ui.util.typeLabelRes

/**
 * The player detail page: header, battle-type switcher, totals, records,
 * charts, and the filtered / sorted ship list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    favorites: List<FavoritePlayer>,
    onBack: () -> Unit,
    onLoadIfNeeded: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onToggleFavorite: (PlayerSummary, GameServer) -> Unit,
    onModeSelected: (BattleMode) -> Unit,
    onSortSelected: (ShipSort) -> Unit,
    onChartMetricSelected: (ChartMetric) -> Unit,
    onChartBreakdownSelected: (ChartBreakdown) -> Unit,
    onShipClick: (ShipPerformance, BattleMode) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.common_retry)
    val error = uiState.error
    val errorText = if (error != null) error.resolve() else null
    val player = uiState.currentPlayer
    val isFavorite = favorites.any {
        it.accountId == player.accountId && it.server == uiState.server
    }
    var showRatingInfo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { onLoadIfNeeded() }

    LaunchedEffect(errorText, uiState.report) {
        if (errorText != null && uiState.report != null) {
            val result = snackbarHostState.showSnackbar(
                message = errorText,
                actionLabel = retryLabel,
            )
            onDismissError()
            if (result == SnackbarResult.ActionPerformed) onRetry()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.report?.account?.nickname ?: uiState.nickname,
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
                actions = {
                    FavoriteStarButton(
                        selected = isFavorite,
                        addLabel = stringResource(R.string.player_favorite_add),
                        removeLabel = stringResource(R.string.player_favorite_remove),
                        onClick = { onToggleFavorite(player, uiState.server) },
                    )
                    IconButton(
                        onClick = { showRatingInfo = true },
                        enabled = uiState.report != null,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.player_rating_info),
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
            uiState.report != null -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier,
            ) {
                PlayerContent(
                    report = uiState.report,
                    state = uiState,
                    onModeSelected = onModeSelected,
                    onSortSelected = onSortSelected,
                    onChartMetricSelected = onChartMetricSelected,
                    onChartBreakdownSelected = onChartBreakdownSelected,
                    onShipClick = onShipClick,
                )
            }

            error != null -> ErrorState(
                title = stringResource(R.string.player_error_title),
                message = error,
                onRetry = onRetry,
                modifier = modifier,
            )

            else -> LoadingState(
                title = stringResource(R.string.player_loading),
                modifier = modifier,
            )
        }
    }

    if (showRatingInfo) {
        RatingScaleSheet(onDismiss = { showRatingInfo = false })
    }
}

@Composable
private fun PlayerContent(
    report: PlayerReport,
    state: PlayerUiState,
    onModeSelected: (BattleMode) -> Unit,
    onSortSelected: (ShipSort) -> Unit,
    onChartMetricSelected: (ChartMetric) -> Unit,
    onChartBreakdownSelected: (ChartBreakdown) -> Unit,
    onShipClick: (ShipPerformance, BattleMode) -> Unit,
) {
    val listState = rememberLazyListState()
    val mode = state.activeMode
    val stats = mode?.let(report::stats)
    val hasCharts = mode != null && (
        report.dailyStats(mode).size > 1 ||
            report.categories(ChartBreakdown.SHIP_TYPE, mode).isNotEmpty() ||
            report.categories(ChartBreakdown.TIER, mode).isNotEmpty()
        )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            PlayerHeaderCard(
                account = report.account,
                server = state.server,
                rating = stats?.rating ?: PlayerRating.Unknown,
            )
        }

        if (mode != null && stats != null) {
            if (report.modes.size > 1) {
                item(key = "mode") {
                    BattleModePicker(
                        modes = report.modes,
                        selection = mode,
                        onSelect = onModeSelected,
                    )
                }
            }
            item(key = "stats") {
                StatsSections(stats = stats, records = report.records(mode))
            }
            if (hasCharts) {
                item(key = "charts") {
                    PlayerChartsSection(
                        report = report,
                        mode = mode,
                        metric = state.chartMetric,
                        breakdown = state.chartBreakdown,
                        onMetricChange = onChartMetricSelected,
                        onBreakdownChange = onChartBreakdownSelected,
                    )
                }
            }
            item(key = "ships") {
                ShipsSection(
                    ships = report.ships,
                    mode = mode,
                    sort = state.sort,
                    onSortSelected = onSortSelected,
                    onShipClick = { ship -> onShipClick(ship, mode) },
                )
            }
        } else {
            item(key = "hidden") {
                HiddenProfileCard()
            }
        }
    }
}

@Composable
private fun PlayerHeaderCard(
    account: AccountInfo,
    server: GameServer,
    rating: PlayerRating,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DogTag(url = account.dogTagUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = account.nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = serverAccountLabel(server, account.accountId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RatingBadge(rating = rating)
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(
                    label = stringResource(R.string.player_registered),
                    value = StatFormat.date(account.createdAt),
                )
                InfoRow(
                    label = stringResource(R.string.player_last_battle),
                    value = StatFormat.date(account.lastBattleTime),
                )
            }
        }
    }
}

@Composable
private fun DogTag(url: String?) {
    if (url == null) {
        DogTagPlaceholder()
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Fit,
        loading = { DogTagPlaceholder() },
        success = { SubcomposeAsyncImageContent() },
        error = { DogTagPlaceholder() },
    )
}

@Composable
private fun DogTagPlaceholder() {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BattleModePicker(
    modes: List<BattleMode>,
    selection: BattleMode,
    onSelect: (BattleMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.player_battle_type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(selection.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        EndAlignedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            modes.forEach { mode ->
                SelectedMenuItem(
                    text = stringResource(mode.labelRes()),
                    selected = mode == selection,
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun ShipsSection(
    ships: List<ShipPerformance>,
    mode: BattleMode,
    sort: ShipSort,
    onSortSelected: (ShipSort) -> Unit,
    onShipClick: (ShipPerformance) -> Unit,
) {
    val sorted = remember(ships, mode, sort) { ships.ranked(mode, sort) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.player_ships_title,
                    StatFormat.integer(sorted.size),
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (sorted.isNotEmpty()) {
                SortMenu(sort = sort, onSelect = onSortSelected)
            }
        }

        if (sorted.isEmpty()) {
            SectionCard {
                Text(
                    text = stringResource(R.string.player_ships_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    sorted.forEachIndexed { index, ship ->
                        ShipRow(
                            performance = ship,
                            mode = mode,
                            sort = sort,
                            onClick = { onShipClick(ship) },
                        )
                        if (index != sorted.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    sort: ShipSort,
    onSelect: (ShipSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.SwapVert,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(sort.labelRes()),
                maxLines = 1,
            )
        }
        EndAlignedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ShipSort.entries.forEach { option ->
                SelectedMenuItem(
                    text = stringResource(option.labelRes()),
                    selected = option == sort,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun ShipRow(
    performance: ShipPerformance,
    mode: BattleMode,
    sort: ShipSort,
    onClick: () -> Unit,
) {
    val stats = performance.stats(mode)
    val dark = isSystemInDarkTheme()
    val rating = stats?.rating ?: PlayerRating.Unknown
    val type = stringResource(performance.ship.typeLabelRes())
    val avgDamage = StatFormat.decimal(stats?.stats?.averageDamage ?: 0.0)
    val avgDamageA11y = stringResource(R.string.player_ships_avg_damage, avgDamage)
    val subtitle = if (sort == ShipSort.LAST_BATTLE) {
        "$type · ${StatFormat.date(stats?.lastBattleTime)} · " +
            StatFormat.percent(stats?.stats?.winRate ?: 0.0, digits = 1)
    } else {
        stringResource(
            R.string.player_ships_subtitle,
            type,
            StatFormat.integer(stats?.stats?.battleCount ?: 0),
            StatFormat.percent(stats?.stats?.winRate ?: 0.0, digits = 1),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = performance.ship.tierLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = performance.ship.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = StatFormat.ratingText(rating),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ratingColor(rating.tier, dark),
            )
            Row(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = avgDamageA11y
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = avgDamage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HiddenProfileCard() {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(R.string.player_hidden_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.player_hidden_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
