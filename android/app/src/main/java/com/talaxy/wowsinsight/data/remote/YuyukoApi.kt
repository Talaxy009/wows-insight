package com.talaxy.wowsinsight.data.remote

import com.talaxy.wowsinsight.data.model.AccountOverview
import com.talaxy.wowsinsight.data.model.DailyStats
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.model.ShipNameLanguage
import com.talaxy.wowsinsight.data.model.ShipStat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.ZoneId
import kotlin.coroutines.resume

/** A user-facing failure of the Yuyuko API. */
sealed class ApiException(message: String? = null) : Exception(message) {
    data object InvalidUrl : ApiException("Invalid request URL")
    data class Http(val code: Int) : ApiException("HTTP $code")
    data object NotFound : ApiException("Not found")
    data class Api(val code: Int, val serverMessage: String?) : ApiException(serverMessage)
    data object MissingData : ApiException("Missing data")
    data class Decoding(val detail: String) : ApiException(detail)
    data class Network(val detail: String?) : ApiException(detail)
}

/** The signed public API surface the screens depend on. */
interface YuyukoApi {
    suspend fun searchPlayers(query: String, server: GameServer): List<PlayerSummary>
    suspend fun accountOverview(accountId: Long, server: GameServer): AccountOverview
    suspend fun recentDays(accountId: Long, server: GameServer): List<DailyStats>
    suspend fun shipStats(accountId: Long, server: GameServer): List<ShipStat>
}

/**
 * A small OkHttp + kotlinx.serialization wrapper around the Yuyuko API.
 *
 * Only `/public/…` endpoints are used, so no credentials are involved. Calls are
 * cancellable: cancelling the coroutine cancels the underlying HTTP call.
 */
class DefaultYuyukoApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : YuyukoApi {

    override suspend fun searchPlayers(
        query: String,
        server: GameServer,
    ): List<PlayerSummary> {
        val body = json.encodeToString(
            YuyukoSearchRequest(
                userName = query,
                one = server.usesExactSearch,
                limit = SEARCH_LIMIT,
            ),
        )
        val users: List<YuyukoSearchUser> = try {
            post(path = "public/wows/account/search/${server.apiValue}/user", body = body)
        } catch (_: ApiException.NotFound) {
            return emptyList()
        } catch (error: ApiException.Api) {
            // The China realm reports a partial nickname as a 540 envelope.
            if (server.usesExactSearch && error.code == 540) return emptyList()
            throw error
        }
        return users.map { PlayerSummary(it.accountId, it.userName) }
    }

    override suspend fun accountOverview(
        accountId: Long,
        server: GameServer,
    ): AccountOverview {
        val data: YuyukoUserInfoData = get(
            path = "public/wows/account/user/info2",
            query = accountQuery(accountId, server),
        )
        return data.toAccountOverview(
            fallbackAccountId = accountId,
            language = ShipNameLanguage.current(),
            server = server,
        )
    }

    override suspend fun recentDays(
        accountId: Long,
        server: GameServer,
    ): List<DailyStats> {
        val data: YuyukoRecentDaysData = try {
            get(
                path = "public/wows/account/day/list/info",
                query = accountQuery(accountId, server),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The service answers 412 until it has a record for the account.
            // Every failure means "no trend to draw"; the player page still
            // loads without the chart.
            return emptyList()
        }

        return data.recentInfo.orEmpty()
            .mapNotNull { it.toDailyStats(zoneId) }
            .sortedBy { it.recordedAt }
    }

    override suspend fun shipStats(
        accountId: Long,
        server: GameServer,
    ): List<ShipStat> {
        val data: YuyukoShipListData = get(
            path = "public/wows/account/ship/info/list",
            query = accountQuery(accountId, server),
        )
        val language = ShipNameLanguage.current()
        return data.shipInfo.orEmpty().mapNotNull { entry ->
            val meta = entry.shipInfo ?: return@mapNotNull null
            ShipStat(
                ship = meta.toShipInfo(language = language, server = server),
                modes = entry.typeInfo?.toModeStats().orEmpty(),
            )
        }
    }

    // --- Transport ---

    private suspend inline fun <reified T> get(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
    ): T = request<T>(requestBuilder(path, query).get().build())

    private suspend inline fun <reified T> post(
        path: String,
        body: String,
    ): T = request<T>(
        requestBuilder(path).post(body.toRequestBody(JSON_MEDIA_TYPE)).build(),
    )

    /** Every call carries the same headers, so they are only written once. */
    private fun requestBuilder(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
    ): Request.Builder {
        val url = BASE_URL.toHttpUrl().newBuilder().addPathSegments(path).apply {
            query.forEach { (name, value) -> addQueryParameter(name, value) }
        }
        return Request.Builder()
            .url(url.build())
            .header("Accept", "application/json")
            .header("Yuyuko-Client-Type", CLIENT_TYPE)
    }

    private suspend inline fun <reified T> request(httpRequest: Request): T = withContext(Dispatchers.IO) {
        val response = execute(httpRequest)
        val envelope = try {
            json.decodeFromString<YuyukoEnvelope<T>>(response.body)
        } catch (failure: Exception) {
            if (response.status in 200..299) {
                throw ApiException.Decoding(
                    "${failure.message}: ${response.body.take(300)}",
                )
            }
            null
        }

        if (response.status == 404 || response.status == 405) throw ApiException.NotFound
        if (envelope == null) {
            if (response.status in 200..299) {
                throw ApiException.Decoding("Unexpected response body")
            }
            throw ApiException.Http(response.status)
        }
        if (envelope.code == 404 || envelope.code == 405) throw ApiException.NotFound
        if (envelope.code != 200) {
            throw ApiException.Api(envelope.code, envelope.message)
        }
        if (response.status !in 200..299) throw ApiException.Http(response.status)
        return@withContext envelope.data ?: throw ApiException.MissingData
    }

    private suspend fun execute(request: Request): HttpResponse =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWith(Result.failure(ApiException.Network(e.message)))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = try {
                        response.use {
                            HttpResponse(
                                status = it.code,
                                body = it.body?.string().orEmpty(),
                            )
                        }
                    } catch (error: Exception) {
                        if (!continuation.isCancelled) {
                            continuation.resumeWith(
                                Result.failure(ApiException.Network(error.message)),
                            )
                        }
                        return
                    }
                    if (continuation.isCancelled) return
                    continuation.resume(result)
                }
            })
        }

    private data class HttpResponse(
        val status: Int,
        val body: String,
    )

    private fun accountQuery(accountId: Long, server: GameServer): List<Pair<String, String>> =
        listOf(
            "server" to server.apiValue,
            "accountId" to accountId.toString(),
        )

    private companion object {
        const val BASE_URL = "https://v3-api.wows.shinoaki.com"
        const val CLIENT_TYPE = "APP;WoWsInsight;1.0"
        const val SEARCH_LIMIT = 25
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
