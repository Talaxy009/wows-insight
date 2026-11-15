package com.talaxy.wowsinsight

import android.content.Context
import com.talaxy.wowsinsight.data.favorites.DataStoreFavoritesRepository
import com.talaxy.wowsinsight.data.favorites.FavoritesFileService
import com.talaxy.wowsinsight.data.favorites.FavoritesRepository
import com.talaxy.wowsinsight.data.favorites.favoritesDataStore
import com.talaxy.wowsinsight.data.remote.DefaultYuyukoApi
import com.talaxy.wowsinsight.data.remote.YuyukoApi
import com.talaxy.wowsinsight.data.repository.DefaultPlayerRepository
import com.talaxy.wowsinsight.data.repository.PlayerRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * A small manual dependency container.
 *
 * The app has one graph with no build-time injection. Keeping it explicit makes
 * the dependencies of each ViewModel visible and keeps the build fast; every
 * collaborator is an interface where a fake is useful in tests.
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        prettyPrint = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val api: YuyukoApi = DefaultYuyukoApi(okHttpClient, json)
    val playerRepository: PlayerRepository = DefaultPlayerRepository(api)

    val favoritesRepository: FavoritesRepository = DataStoreFavoritesRepository(
        dataStore = applicationContext.favoritesDataStore,
        json = json,
    )

    val favoritesFileService: FavoritesFileService = FavoritesFileService(
        contentResolver = applicationContext.contentResolver,
        json = json,
    )

    private companion object {
        // A cold player makes the service fetch from Wargaming, which takes
        // 6-8 s. The iOS app also waits 30 s before giving up.
        const val HTTP_TIMEOUT_SECONDS = 30L
    }
}
