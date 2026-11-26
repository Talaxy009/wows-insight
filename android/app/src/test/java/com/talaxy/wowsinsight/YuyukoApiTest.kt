package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.remote.ApiException
import com.talaxy.wowsinsight.data.remote.DefaultYuyukoApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YuyukoApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private fun api(status: Int, body: String): DefaultYuyukoApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("test")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return DefaultYuyukoApi(client, json)
    }

    @Test
    fun searchReadsTheUsersOfASuccessfulEnvelope() = runBlocking {
        val body = """{"code":200,"data":[{"accountId":3002439652,"userName":"Yamato"}]}"""

        val players = api(200, body).searchPlayers("Yamato", GameServer.ASIA)

        assertEquals(3_002_439_652L, players.single().accountId)
        assertEquals("Yamato", players.single().nickname)
    }

    @Test
    fun searchTreatsNotFoundAsNoResults() = runBlocking {
        assertTrue(api(404, "").searchPlayers("Nobody", GameServer.ASIA).isEmpty())
        assertTrue(
            api(200, """{"code":404,"message":"missing","data":[]}""")
                .searchPlayers("Nobody", GameServer.ASIA)
                .isEmpty(),
        )
    }

    @Test
    fun chinaPartialNicknameEnvelopeIsNoResultsButOtherRealmsKeepTheError() = runBlocking {
        val body = """{"code":540,"message":"upstream 400","data":null}"""

        assertTrue(api(200, body).searchPlayers("ab", GameServer.CN).isEmpty())

        val failure = runCatching { api(200, body).searchPlayers("ab", GameServer.ASIA) }
            .exceptionOrNull()
        assertEquals(ApiException.Api(540, "upstream 400"), failure)
    }

    @Test
    fun unreadableBodiesAreReportedByTheirStatus() {
        assertThrows(ApiException.Http::class.java) {
            runBlocking { api(502, "<html>").accountOverview(1L, GameServer.ASIA) }
        }
        assertThrows(ApiException.Decoding::class.java) {
            runBlocking { api(200, "<html>").accountOverview(1L, GameServer.ASIA) }
        }
    }

    @Test
    fun anEnvelopeWithoutDataIsMissingData() {
        assertThrows(ApiException.MissingData::class.java) {
            runBlocking { api(200, """{"code":200,"data":null}""").accountOverview(1L, GameServer.ASIA) }
        }
    }

    @Test
    fun recentDaysNeverFails() = runBlocking {
        val days = api(200, """{"code":412,"message":"no recent record"}""")
            .recentDays(1L, GameServer.ASIA)

        assertTrue(days.isEmpty())
    }
}
