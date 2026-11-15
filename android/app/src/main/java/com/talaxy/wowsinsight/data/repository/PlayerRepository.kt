package com.talaxy.wowsinsight.data.repository

import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.PlayerReport
import com.talaxy.wowsinsight.data.model.PlayerSummary
import com.talaxy.wowsinsight.data.remote.YuyukoApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Everything the UI needs from the Yuyuko service. */
interface PlayerRepository {
    suspend fun searchPlayers(query: String, server: GameServer): List<PlayerSummary>
    suspend fun loadReport(accountId: Long, server: GameServer): PlayerReport
}

/**
 * Loads the overview, the ship list and the daily records concurrently.
 *
 * The daily request never throws (no records simply means no chart), so a
 * player page is built once and only once all three calls have settled.
 */
class DefaultPlayerRepository(
    private val api: YuyukoApi,
) : PlayerRepository {

    override suspend fun searchPlayers(query: String, server: GameServer): List<PlayerSummary> =
        api.searchPlayers(query, server)

    override suspend fun loadReport(accountId: Long, server: GameServer): PlayerReport =
        coroutineScope {
            val overview = async { api.accountOverview(accountId, server) }
            val ships = async { api.shipStats(accountId, server) }
            val daily = async { api.recentDays(accountId, server) }

            PlayerReport.create(
                overview = overview.await(),
                shipStats = ships.await(),
                dailyStats = daily.await(),
            )
        }
}
