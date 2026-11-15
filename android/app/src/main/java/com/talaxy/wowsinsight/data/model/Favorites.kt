package com.talaxy.wowsinsight.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit

/** A locally saved player. The server is stored because account IDs repeat. */
@Serializable
data class FavoritePlayer(
    val accountId: Long,
    val nickname: String,
    val server: GameServer,
) {
    val id: String get() = "${server.apiValue}-$accountId"

    val summary: PlayerSummary get() = PlayerSummary(accountId, nickname)
}

/**
 * The JSON file used to move favorites between devices.
 *
 * This intentionally matches the iOS archive so a file exported by either app
 * can be imported by the other.
 */
@Serializable
data class FavoritesArchive(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String,
    val players: List<FavoritePlayer>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun create(
            players: List<FavoritePlayer>,
            exportedAt: Instant = Instant.now(),
        ): FavoritesArchive = FavoritesArchive(
            version = CURRENT_VERSION,
            // The iOS decoder uses Foundation's ISO8601 strategy, which expects
            // whole seconds. Truncating keeps archives interchangeable.
            exportedAt = exportedAt.truncatedTo(ChronoUnit.SECONDS).toString(),
            players = players,
        )
    }
}

data class ImportSummary(
    val added: Int,
    val skipped: Int,
)
