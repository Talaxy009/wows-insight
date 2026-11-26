package com.talaxy.wowsinsight

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.talaxy.wowsinsight.data.favorites.DataStoreFavoritesRepository
import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FavoritesRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: DataStoreFavoritesRepository

    private val yamato = FavoritePlayer(3_002_439_652, "Yamato", GameServer.ASIA)
    private val flambass = FavoritePlayer(571_108_101, "Flambass", GameServer.EU)
    private val sameIdOtherRealm = FavoritePlayer(3_002_439_652, "Yamato", GameServer.NA)

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(folder.root, "favorites.preferences_pb")
        }
        repository = DataStoreFavoritesRepository(dataStore, Json)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun toggleAddsToTheFrontAndRemovesOnTheSecondCall() = runBlocking {
        repository.toggle(yamato.summary, yamato.server)
        repository.toggle(flambass.summary, flambass.server)
        assertEquals(listOf(flambass, yamato), repository.snapshot())

        repository.toggle(flambass.summary, flambass.server)
        assertEquals(listOf(yamato), repository.snapshot())
    }

    @Test
    fun importAppendsNewPlayersAndNeverDropsLocalOnes() = runBlocking {
        repository.importArchive(listOf(yamato, flambass))

        // The same account on another realm is a different favorite.
        val summary = repository.importArchive(listOf(flambass, sameIdOtherRealm, sameIdOtherRealm))

        assertEquals(1, summary.added)
        assertEquals(2, summary.skipped)
        assertEquals(listOf(yamato, flambass, sameIdOtherRealm), repository.snapshot())
    }

    @Test
    fun moveByStaysInsideTheList() = runBlocking {
        repository.importArchive(listOf(yamato, flambass))

        repository.moveBy(yamato, -1)
        assertEquals(listOf(yamato, flambass), repository.snapshot())

        repository.moveBy(yamato, 1)
        assertEquals(listOf(flambass, yamato), repository.snapshot())
    }

    @Test
    fun updateNicknameKeepsTheOrder() = runBlocking {
        repository.importArchive(listOf(yamato, flambass))

        repository.updateNickname(PlayerSummary(yamato.accountId, "Yamato2"), yamato.server)

        assertEquals(listOf(yamato.copy(nickname = "Yamato2"), flambass), repository.snapshot())
    }
}
