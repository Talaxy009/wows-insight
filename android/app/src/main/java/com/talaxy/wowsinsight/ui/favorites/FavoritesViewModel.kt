package com.talaxy.wowsinsight.ui.favorites

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.talaxy.wowsinsight.R
import com.talaxy.wowsinsight.data.favorites.FavoritesFileService
import com.talaxy.wowsinsight.data.favorites.FavoritesRepository
import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.ui.util.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collects the persisted favorites and exposes the import / export actions used
 * by the search screen. Storage work runs on the IO dispatcher; the collection
 * itself is a cold DataStore flow shared by every observer.
 */
class FavoritesViewModel(
    private val repository: FavoritesRepository,
    private val fileService: FavoritesFileService,
) : ViewModel() {

    val favorites = repository.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    fun toggle(player: PlayerSummary, server: GameServer) {
        persist { repository.toggle(player, server) }
    }

    fun remove(favorite: FavoritePlayer) {
        persist { repository.remove(favorite) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        persist { repository.move(fromIndex, toIndex) }
    }

    fun moveBy(favorite: FavoritePlayer, delta: Int) {
        persist { repository.moveBy(favorite, delta) }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            try {
                val players = withContext(Dispatchers.IO) {
                    fileService.import(uri).getOrThrow()
                }
                val summary = repository.importArchive(players)
                _messages.emit(
                    UiText.Res(
                        R.string.search_import_summary,
                        listOf(summary.added, summary.skipped),
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _messages.emit(UiText.Res(R.string.search_import_failed_message))
            }
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            try {
                val players = repository.snapshot()
                val result = withContext(Dispatchers.IO) { fileService.export(uri, players) }
                _messages.emit(
                    UiText.Res(
                        if (result.isSuccess) {
                            R.string.search_export_done
                        } else {
                            R.string.search_export_failed
                        },
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _messages.emit(UiText.Res(R.string.search_export_failed))
            }
        }
    }

    /** Runs a storage write and reports a failure without crashing the app. */
    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _messages.emit(UiText.Res(R.string.favorites_save_failed))
            }
        }
    }

    companion object {
        fun factory(
            repository: FavoritesRepository,
            fileService: FavoritesFileService,
        ) = viewModelFactory {
            initializer { FavoritesViewModel(repository, fileService) }
        }
    }
}
