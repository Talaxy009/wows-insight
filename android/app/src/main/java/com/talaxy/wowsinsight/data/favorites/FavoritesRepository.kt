package com.talaxy.wowsinsight.data.favorites

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.ImportSummary
import com.talaxy.wowsinsight.data.model.PlayerSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "favorites",
)

/**
 * Stores the user's favourite players as JSON in Preferences DataStore.
 *
 * The list is tiny, so one JSON value keeps the implementation simple and avoids
 * adding a database for one screen. The format is a plain array of
 * [FavoritePlayer], matching what the iOS app keeps in UserDefaults.
 */
interface FavoritesRepository {
    val favorites: Flow<List<FavoritePlayer>>

    suspend fun toggle(player: PlayerSummary, server: GameServer)
    suspend fun remove(favorite: FavoritePlayer)
    suspend fun updateNickname(player: PlayerSummary, server: GameServer)
    suspend fun move(fromIndex: Int, toIndex: Int)
    suspend fun moveBy(favorite: FavoritePlayer, delta: Int)
    suspend fun importArchive(players: List<FavoritePlayer>): ImportSummary
    suspend fun snapshot(): List<FavoritePlayer>
}

class DataStoreFavoritesRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : FavoritesRepository {

    private val mutex = Mutex()
    private val serializer = ListSerializer(FavoritePlayer.serializer())

    override val favorites: Flow<List<FavoritePlayer>> =
        dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { preferences -> decode(preferences[STORAGE_KEY]) }

    override suspend fun toggle(player: PlayerSummary, server: GameServer) {
        update { players ->
            val index = players.indexOfFirst {
                it.accountId == player.accountId && it.server == server
            }
            if (index >= 0) {
                players.removeAt(index)
            } else {
                players.add(
                    0,
                    FavoritePlayer(
                        accountId = player.accountId,
                        nickname = player.nickname,
                        server = server,
                    ),
                )
            }
        }
    }

    override suspend fun remove(favorite: FavoritePlayer) {
        update { players ->
            players.removeAll {
                it.accountId == favorite.accountId && it.server == favorite.server
            }
        }
    }

    override suspend fun updateNickname(player: PlayerSummary, server: GameServer) {
        update { players ->
            val index = players.indexOfFirst {
                it.accountId == player.accountId && it.server == server
            }
            if (index >= 0 && players[index].nickname != player.nickname) {
                players[index] = players[index].copy(nickname = player.nickname)
            }
        }
    }

    override suspend fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        update { players ->
            if (fromIndex !in players.indices) return@update
            val item = players.removeAt(fromIndex)
            players.add(toIndex.coerceIn(0, players.size), item)
        }
    }

    override suspend fun moveBy(favorite: FavoritePlayer, delta: Int) {
        update { players ->
            val index = players.indexOfFirst { it.id == favorite.id }
            val target = index + delta
            if (index < 0 || target !in players.indices) return@update
            val item = players.removeAt(index)
            players.add(target, item)
        }
    }

    /**
     * Appends favorites that are not saved yet and keeps the existing order, so
     * importing never drops a local favorite.
     */
    override suspend fun importArchive(players: List<FavoritePlayer>): ImportSummary {
        var added = 0
        var skipped = 0
        update { current ->
            for (player in players) {
                val exists = current.any {
                    it.accountId == player.accountId && it.server == player.server
                }
                if (exists) {
                    skipped += 1
                } else {
                    current.add(player)
                    added += 1
                }
            }
        }
        return ImportSummary(added = added, skipped = skipped)
    }

    override suspend fun snapshot(): List<FavoritePlayer> = favorites.first()

    private suspend fun update(block: (MutableList<FavoritePlayer>) -> Unit) {
        mutex.withLock {
            dataStore.edit { preferences ->
                val players = decode(preferences[STORAGE_KEY]).toMutableList()
                block(players)
                preferences[STORAGE_KEY] = json.encodeToString(serializer, players)
            }
        }
    }

    private fun decode(raw: String?): List<FavoritePlayer> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    private companion object {
        val STORAGE_KEY = stringPreferencesKey("favorites.players")
    }
}
