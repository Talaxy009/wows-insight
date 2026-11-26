package com.talaxy.wowsinsight.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.talaxy.wowsinsight.data.favorites.FavoritesRepository
import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.ChartBreakdown
import com.talaxy.wowsinsight.data.model.ChartMetric
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerReport
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.model.ShipSort
import com.talaxy.wowsinsight.data.repository.PlayerRepository
import com.talaxy.wowsinsight.ui.util.UiText
import com.talaxy.wowsinsight.ui.util.toUiText
import com.talaxy.wowsinsight.util.runCatchingCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val accountId: Long,
    val nickname: String,
    val server: GameServer,
    val report: PlayerReport? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    val selectedMode: BattleMode? = null,
    val sort: ShipSort = ShipSort.Default,
    val chartMetric: ChartMetric = ChartMetric.RATING,
    val chartBreakdown: ChartBreakdown = ChartBreakdown.SHIP_TYPE,
) {
    /** The picked mode, or random battles while the report is loading. */
    val activeMode: BattleMode?
        get() = selectedMode
            ?.takeIf { report?.stats(it) != null }
            ?: report?.defaultMode

    val currentPlayer: PlayerSummary
        get() = report?.account?.let { PlayerSummary(it.accountId, it.nickname) }
            ?: PlayerSummary(accountId, nickname)
}

/**
 * Loads one player report and owns the view options of the player page.
 *
 * The ViewModel survives configuration changes, so the selected battle type,
 * ship sort and chart controls are not reset by a rotation. A refresh keeps the
 * already loaded report on screen if it fails.
 */
class PlayerViewModel(
    account: PlayerSummary,
    server: GameServer,
    private val repository: PlayerRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            accountId = account.accountId,
            nickname = account.nickname,
            server = server,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadIfNeeded() {
        if (_uiState.value.report == null) load()
    }

    /** Reloads even when a report is already on screen (pull to refresh, retry). */
    fun refresh() {
        load()
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun selectMode(mode: BattleMode) {
        if (_uiState.value.report?.stats(mode) == null) return
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun setSort(sort: ShipSort) {
        _uiState.update { it.copy(sort = sort) }
    }

    fun setChartMetric(metric: ChartMetric) {
        _uiState.update { it.copy(chartMetric = metric) }
    }

    fun setChartBreakdown(breakdown: ChartBreakdown) {
        _uiState.update { it.copy(chartBreakdown = breakdown) }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        val current = _uiState.value

        val hasReport = current.report != null
        _uiState.update {
            it.copy(
                isLoading = !hasReport,
                isRefreshing = hasReport,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            runCatchingCancellable { repository.loadReport(current.accountId, current.server) }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            report = report,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                    // The API nickname is authoritative; keep favorites in sync.
                    // A storage failure must not turn a successful report into
                    // an error screen.
                    runCatchingCancellable {
                        favoritesRepository.updateNickname(
                            PlayerSummary(report.account.accountId, report.account.nickname),
                            current.server,
                        )
                    }
                }
                .onFailure { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = failure.toUiText(),
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            account: PlayerSummary,
            server: GameServer,
            repository: PlayerRepository,
            favoritesRepository: FavoritesRepository,
        ) = viewModelFactory {
            initializer {
                PlayerViewModel(
                    account = account,
                    server = server,
                    repository = repository,
                    favoritesRepository = favoritesRepository,
                )
            }
        }
    }
}
