package com.talaxy.wowsinsight.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.repository.PlayerRepository
import com.talaxy.wowsinsight.ui.util.UiText
import com.talaxy.wowsinsight.ui.util.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SearchUiState(
    val query: String = "",
    val server: GameServer = GameServer.ASIA,
    val results: List<PlayerSummary> = emptyList(),
    val isSearching: Boolean = false,
    val error: UiText? = null,
) {
    val trimmedQuery: String get() = query.trim()

    val hasValidQuery: Boolean get() = trimmedQuery.length >= SearchViewModel.MINIMUM_QUERY_LENGTH
}

/**
 * Owns the debounced player search.
 *
 * Every query / server change cancels the previous request through
 * `flatMapLatest`; a 350 ms delay before the request is the debounce. Results
 * from an older key can never overwrite the current one, because the older flow
 * is cancelled before it reaches the state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: PlayerRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val server = MutableStateFlow(GameServer.ASIA)
    private val output = MutableStateFlow(SearchOutput())
    private val retryToken = MutableStateFlow(0)

    val uiState = combine(query, server, output) { currentQuery, currentServer, current ->
        SearchUiState(
            query = currentQuery,
            server = currentServer,
            results = current.results,
            isSearching = current.isSearching,
            error = current.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    init {
        combine(query, server, retryToken) { currentQuery, currentServer, token ->
            SearchKey(
                query = currentQuery.trim(),
                server = currentServer,
                token = token,
            )
        }
            .distinctUntilChanged()
            .flatMapLatest(::searchFlow)
            .onEach(::apply)
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onServerChange(value: GameServer) {
        server.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    /** Re-runs the search without waiting for another keystroke. */
    fun searchNow() {
        retryToken.update { it + 1 }
    }

    private fun searchFlow(key: SearchKey): Flow<SearchOutcome> = flow {
        if (key.query.length < MINIMUM_QUERY_LENGTH) {
            emit(SearchOutcome.Idle)
            return@flow
        }
        emit(SearchOutcome.Searching)
        delay(DEBOUNCE_MILLIS)
        try {
            emit(SearchOutcome.Success(repository.searchPlayers(key.query, key.server)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            emit(SearchOutcome.Failure(failure.toUiText()))
        }
    }

    /** One atomic write per outcome, so the UI never sees a half-updated list. */
    private fun apply(outcome: SearchOutcome) {
        output.value = when (outcome) {
            SearchOutcome.Idle -> SearchOutput()
            SearchOutcome.Searching -> SearchOutput(isSearching = true)
            is SearchOutcome.Success -> SearchOutput(results = outcome.players)
            is SearchOutcome.Failure -> SearchOutput(error = outcome.message)
        }
    }

    private data class SearchOutput(
        val results: List<PlayerSummary> = emptyList(),
        val isSearching: Boolean = false,
        val error: UiText? = null,
    )

    private data class SearchKey(
        val query: String,
        val server: GameServer,
        val token: Int,
    )

    private sealed interface SearchOutcome {
        data object Idle : SearchOutcome
        data object Searching : SearchOutcome
        data class Success(val players: List<PlayerSummary>) : SearchOutcome
        data class Failure(val message: UiText) : SearchOutcome
    }

    companion object {
        const val MINIMUM_QUERY_LENGTH = 3
        private const val DEBOUNCE_MILLIS = 350L

        fun factory(repository: PlayerRepository) = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}
