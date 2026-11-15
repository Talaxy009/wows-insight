package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.FavoritePlayer
import com.talaxy.wowsinsight.data.model.FavoritesArchive
import com.talaxy.wowsinsight.data.model.GameServer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FavoritesArchiveTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun decodesTheIosArchiveShapeWith64BitAccountIds() {
        val raw = """
            {
              "version": 1,
              "exportedAt": "2026-11-12T04:27:22Z",
              "players": [
                {
                  "accountId": 3002439652,
                  "nickname": "Yamato",
                  "server": "asia"
                }
              ]
            }
        """.trimIndent()

        val archive = json.decodeFromString<FavoritesArchive>(raw)

        assertEquals(1, archive.version)
        assertEquals(3_002_439_652L, archive.players.single().accountId)
        assertEquals("Yamato", archive.players.single().nickname)
        assertEquals(GameServer.ASIA, archive.players.single().server)
    }

    @Test
    fun encodedArchiveCanBeReadBack() {
        val players = listOf(
            FavoritePlayer(3_002_439_652, "Yamato", GameServer.ASIA),
            FavoritePlayer(571_108_101, "Flambass", GameServer.EU),
        )
        val encoded = json.encodeToString(FavoritesArchive.create(players))
        val decoded = json.decodeFromString<FavoritesArchive>(encoded)

        assertEquals(FavoritesArchive.CURRENT_VERSION, decoded.version)
        assertEquals(players, decoded.players)
        assertTrue(encoded.contains("\"server\": \"asia\""))
    }

    @Test
    fun exportedAtUsesWholeSecondsForTheIosDecoder() {
        val archive = FavoritesArchive.create(
            players = emptyList(),
            exportedAt = Instant.parse("2026-11-12T04:27:22.685040Z"),
        )

        assertEquals("2026-11-12T04:27:22Z", archive.exportedAt)
    }
}
