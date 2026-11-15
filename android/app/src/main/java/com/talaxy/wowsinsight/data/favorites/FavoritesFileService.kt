package com.talaxy.wowsinsight.data.favorites

import android.content.ContentResolver
import android.net.Uri
import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.FavoritesArchive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Reads and writes the versioned favorites archive through the Storage Access
 * Framework, so no storage permission is needed.
 */
class FavoritesFileService(
    private val contentResolver: ContentResolver,
    private val json: Json,
) {
    fun import(uri: Uri): Result<List<FavoritePlayer>> = runCatching {
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                require(output.size() <= MAX_ARCHIVE_BYTES) {
                    "Favorites archive is too large"
                }
            }
            output.toByteArray()
        } ?: error("Could not open the selected file")
        val archive = json.decodeFromString<FavoritesArchive>(bytes.decodeToString())
        require(archive.version == FavoritesArchive.CURRENT_VERSION) {
            "Unsupported favorites archive version"
        }
        archive.players
    }

    fun export(uri: Uri, players: List<FavoritePlayer>): Result<Unit> = runCatching {
        val archive = FavoritesArchive.create(players)
        val payload = json.encodeToString(archive).encodeToByteArray()
        contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(payload)
            output.flush()
        } ?: error("Could not open the destination file")
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 2 * 1024 * 1024
    }
}
