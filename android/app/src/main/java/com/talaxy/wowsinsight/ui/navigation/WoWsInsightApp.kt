package com.talaxy.wowsinsight.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.talaxy.wowsinsight.AppContainer
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.ui.about.AboutScreen
import com.talaxy.wowsinsight.ui.favorites.FavoritesViewModel
import com.talaxy.wowsinsight.ui.player.PlayerScreen
import com.talaxy.wowsinsight.ui.player.PlayerViewModel
import com.talaxy.wowsinsight.ui.search.SearchScreen
import com.talaxy.wowsinsight.ui.search.SearchViewModel
import com.talaxy.wowsinsight.ui.ship.ShipDetailScreen

private const val PLAYER_VIEW_MODEL_KEY = "player"

@Composable
fun WoWsInsightApp(container: AppContainer) {
    val navController = rememberNavController()
    val favoritesViewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModel.factory(
            repository = container.favoritesRepository,
            fileService = container.favoritesFileService,
        ),
    )
    val favorites by favoritesViewModel.favorites.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = SearchRoute,
    ) {
        composable<SearchRoute> {
            val searchViewModel: SearchViewModel = viewModel(
                factory = SearchViewModel.factory(container.playerRepository),
            )
            val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

            SearchScreen(
                uiState = uiState,
                favorites = favorites,
                messages = favoritesViewModel.messages,
                onQueryChange = searchViewModel::onQueryChange,
                onServerChange = searchViewModel::onServerChange,
                onSearchNow = searchViewModel::searchNow,
                onClearQuery = searchViewModel::clearQuery,
                onOpenPlayer = { player, server ->
                    navController.navigate(
                        PlayerRoute(
                            accountId = player.accountId,
                            nickname = player.nickname,
                            server = server.apiValue,
                        ),
                    )
                },
                onToggleFavorite = favoritesViewModel::toggle,
                onRemoveFavorite = favoritesViewModel::remove,
                onMoveFavorite = favoritesViewModel::move,
                onMoveFavoriteBy = favoritesViewModel::moveBy,
                onImport = favoritesViewModel::import,
                onExport = favoritesViewModel::export,
                onOpenAbout = { navController.navigate(AboutRoute) },
            )
        }

        composable<AboutRoute> {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable<PlayerRoute> { entry ->
            val route = entry.toRoute<PlayerRoute>()
            val server = GameServer.fromApi(route.server) ?: GameServer.ASIA
            val playerViewModel: PlayerViewModel = viewModel(
                key = PLAYER_VIEW_MODEL_KEY,
                factory = PlayerViewModel.factory(
                    account = PlayerSummary(route.accountId, route.nickname),
                    server = server,
                    repository = container.playerRepository,
                    favoritesRepository = container.favoritesRepository,
                ),
            )
            val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

            PlayerScreen(
                uiState = uiState,
                favorites = favorites,
                onBack = { navController.popBackStack() },
                onLoadIfNeeded = playerViewModel::loadIfNeeded,
                onRetry = playerViewModel::refresh,
                onRefresh = playerViewModel::refresh,
                onDismissError = playerViewModel::dismissError,
                onToggleFavorite = favoritesViewModel::toggle,
                onModeSelected = playerViewModel::selectMode,
                onSortSelected = playerViewModel::setSort,
                onChartMetricSelected = playerViewModel::setChartMetric,
                onChartBreakdownSelected = playerViewModel::setChartBreakdown,
                onShipClick = { ship, mode ->
                    navController.navigate(
                        ShipRoute(shipId = ship.ship.shipId, mode = mode.apiCode),
                    )
                },
            )
        }

        composable<ShipRoute> { entry ->
            val route = entry.toRoute<ShipRoute>()
            val parentEntry = remember(entry) {
                navController.getBackStackEntry<PlayerRoute>()
            }
            val parentRoute = parentEntry.toRoute<PlayerRoute>()
            val server = GameServer.fromApi(parentRoute.server) ?: GameServer.ASIA
            val playerViewModel: PlayerViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                key = PLAYER_VIEW_MODEL_KEY,
                factory = PlayerViewModel.factory(
                    account = PlayerSummary(parentRoute.accountId, parentRoute.nickname),
                    server = server,
                    repository = container.playerRepository,
                    favoritesRepository = container.favoritesRepository,
                ),
            )
            val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
            val mode = BattleMode.fromApi(route.mode) ?: BattleMode.PVP
            val ship = uiState.report?.ships?.firstOrNull {
                it.ship.shipId == route.shipId
            }

            LaunchedEffect(Unit) {
                playerViewModel.loadIfNeeded()
            }

            ShipDetailScreen(
                ship = ship,
                mode = mode,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onBack = { navController.popBackStack() },
                onRetry = playerViewModel::refresh,
            )
        }
    }
}
