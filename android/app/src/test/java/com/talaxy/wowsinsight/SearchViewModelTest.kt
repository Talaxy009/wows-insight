package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerReport
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.remote.ApiException
import com.talaxy.wowsinsight.data.repository.PlayerRepository
import com.talaxy.wowsinsight.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun waitsForTheDebounceAndCancelsTheOlderQuery() = runTest(dispatcher) {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        viewModel.onQueryChange("Yam")
        runCurrent()
        assertTrue(viewModel.uiState.value.isSearching)

        advanceTimeBy(200)
        viewModel.onQueryChange("Yama")
        runCurrent()
        advanceTimeBy(349)
        assertTrue(repository.queries.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Yama"), repository.queries)
        assertFalse(viewModel.uiState.value.isSearching)
        assertEquals(2, viewModel.uiState.value.results.size)
    }

    @Test
    fun aShortQueryShowsTheLocalContentWithoutCallingTheApi() = runTest(dispatcher) {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        viewModel.onQueryChange("Ya")
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertFalse(viewModel.uiState.value.hasValidQuery)
        assertFalse(viewModel.uiState.value.isSearching)
        assertTrue(repository.queries.isEmpty())
    }

    @Test
    fun apiFailureBecomesAUserFacingError() = runTest(dispatcher) {
        val repository = FakeSearchRepository(fail = true)
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        viewModel.onQueryChange("Yamato")
        runCurrent()
        advanceTimeBy(351)
        runCurrent()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSearching)
    }

    private class FakeSearchRepository(
        private val fail: Boolean = false,
    ) : PlayerRepository {
        val queries = mutableListOf<String>()

        override suspend fun searchPlayers(
            query: String,
            server: GameServer,
        ): List<PlayerSummary> {
            queries += query
            if (fail) throw ApiException.Network("offline")
            return listOf(
                PlayerSummary(3_002_439_652, "Yamato"),
                PlayerSummary(2_006_871_100, "yamato000"),
            )
        }

        override suspend fun loadReport(accountId: Long, server: GameServer): PlayerReport =
            error("not used")
    }
}
