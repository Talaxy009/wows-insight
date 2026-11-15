package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.BattleMode
import com.talaxy.wowsinsight.data.model.CategoryKey
import com.talaxy.wowsinsight.data.model.GameServer
import com.talaxy.wowsinsight.data.model.ShipNameLanguage
import com.talaxy.wowsinsight.data.remote.ApiException
import com.talaxy.wowsinsight.data.remote.YuyukoMax
import com.talaxy.wowsinsight.data.remote.YuyukoMaxValue
import com.talaxy.wowsinsight.data.remote.YuyukoModeSummary
import com.talaxy.wowsinsight.data.remote.YuyukoRating
import com.talaxy.wowsinsight.data.remote.YuyukoShipMeta
import com.talaxy.wowsinsight.data.remote.YuyukoStatsBlock
import com.talaxy.wowsinsight.data.remote.YuyukoTotals
import com.talaxy.wowsinsight.data.remote.YuyukoUserDetail
import com.talaxy.wowsinsight.data.remote.YuyukoUserInfoData
import com.talaxy.wowsinsight.data.remote.toAccountOverview
import com.talaxy.wowsinsight.data.remote.toBattleStats
import com.talaxy.wowsinsight.data.remote.toCategories
import com.talaxy.wowsinsight.data.remote.toModeStats
import com.talaxy.wowsinsight.data.remote.toShipInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YuyukoMappersTest {
    private val block = YuyukoStatsBlock(
        battleInfo = YuyukoTotals(
            battle = 30,
            wins = 14,
            losses = 16,
            survived = 9,
            winAndSurvived = 5,
            xp = 33_292L,
            damageDealt = 1_040_495L,
            scoutingDamage = 142_385L,
            shipsSpotted = 10,
            planesKilled = 27,
            artAgro = 13_144_750L,
        ),
        fragsInfo = com.talaxy.wowsinsight.data.remote.YuyukoFrags(frags = 42),
        maxInfo = YuyukoMax(
            maxDamageDealt = YuyukoMaxValue(shipId = 4_278_105_904, value = 117_445L),
            maxFrags = YuyukoMaxValue(shipId = 4_278_105_904, value = 5L),
        ),
        hitRatioInfo = com.talaxy.wowsinsight.data.remote.YuyukoHitRatio(ratioMain = 38.3),
        lastBattleTime = 1_767_211_200,
    )

    @Test
    fun statsBlockFillsMissingNumbersWithZeroAndKeeps64BitShipIds() {
        val stats = block.toBattleStats()

        assertEquals(30, stats.battleCount)
        assertEquals(42, stats.fragCount)
        assertEquals(1_040_495L, stats.damage)
        assertEquals(38.3, stats.hitRate, 0.0)
        assertEquals(
            117_445L,
            stats.records.getValue(com.talaxy.wowsinsight.data.model.BattleRecordKind.MAX_DAMAGE).value,
        )
        assertEquals(
            4_278_105_904L,
            stats.records.getValue(com.talaxy.wowsinsight.data.model.BattleRecordKind.MAX_DAMAGE).shipId,
        )

        val missing = YuyukoStatsBlock().toBattleStats()
        assertEquals(0, missing.battleCount)
        assertEquals(0.0, missing.winRate, 0.0)
    }

    @Test
    fun lifetimeTotalsAboveIntMaxAreKeptAsLong() {
        val stats = YuyukoStatsBlock(
            battleInfo = YuyukoTotals(
                battle = 8_885,
                wins = 4_655,
                losses = 4_226,
                xp = 12_382_603L,
                damageDealt = 409_052_943L,
                scoutingDamage = 222_410_046L,
                artAgro = 6_756_546_469L,
            ),
        ).toBattleStats()

        assertEquals(6_756_546_469L, stats.potentialDamage)
        assertEquals(409_052_943L, stats.damage)
        assertEquals(222_410_046L, stats.scoutingDamage)
        assertEquals(12_382_603L, stats.experience)
        assertEquals(46_038.6, stats.averageDamage, 0.2)
    }

    @Test
    fun modeStatsAcceptBothNestingShapesAndIgnoreUnknownCodes() {
        val asShipInfo = mapOf(
            "PVP" to YuyukoModeSummary(
                prInfo = YuyukoRating(code = 1, value = 1_820),
                shipInfo = block,
            ),
            "SOMETHING_NEW" to YuyukoModeSummary(shipInfo = block),
            "PVP_SOLO" to YuyukoModeSummary(), // no stats block: dropped
        ).toModeStats()

        assertEquals(setOf(BattleMode.PVP), asShipInfo.keys)
        assertEquals(1_820, asShipInfo.getValue(BattleMode.PVP).rating.value)
        assertEquals(1_767_211_200, asShipInfo.getValue(BattleMode.PVP).lastBattleTime)

        val asBattleInfo = mapOf(
            "PVP" to YuyukoModeSummary(battleInfo = block),
        ).toModeStats()
        assertEquals(30, asBattleInfo.getValue(BattleMode.PVP).stats.battleCount)
    }

    @Test
    fun ratingCodeZeroMeansUnknown() {
        val mode = mapOf(
            "PVP" to YuyukoModeSummary(
                prInfo = YuyukoRating(code = 0, value = 0),
                shipInfo = block,
            ),
        ).toModeStats().getValue(BattleMode.PVP)

        assertTrue(mode.rating.value < 0)
    }

    @Test
    fun shipNamesFollowTheInterfaceLanguageAndRealm() {
        val meta = YuyukoShipMeta(
            shipId = 4_278_105_904,
            nameCn = "大和",
            nameCn360 = "鲸",
            nameEnglish = "Yamato",
            level = 10,
            shipType = "Battleship",
            country = "Japan",
            imgSmall = "https://example.invalid/ship.png",
        )

        assertEquals(
            "大和",
            meta.toShipInfo(ShipNameLanguage.CHINESE, GameServer.ASIA).name,
        )
        assertEquals(
            "鲸",
            meta.toShipInfo(ShipNameLanguage.CHINESE, GameServer.CN).name,
        )
        assertEquals(
            "Yamato",
            meta.toShipInfo(ShipNameLanguage.ENGLISH, GameServer.ASIA).name,
        )
        assertEquals("X", meta.toShipInfo(ShipNameLanguage.ENGLISH, GameServer.ASIA).tierLabel)
        assertEquals("Battleship", meta.toShipInfo(ShipNameLanguage.ENGLISH, GameServer.ASIA).type)
        assertEquals(
            "https://example.invalid/ship.png",
            meta.toShipInfo(ShipNameLanguage.ENGLISH, GameServer.ASIA).imageUrl,
        )
    }

    @Test
    fun categoriesDropEmptyGroups() {
        val categories = mapOf(
            "Cruiser" to mapOf("PVP" to YuyukoModeSummary(shipInfo = block)),
            "Battleship" to mapOf(
                "PVP" to YuyukoModeSummary(
                    shipInfo = YuyukoStatsBlock(
                        battleInfo = YuyukoTotals(battle = 0),
                    ),
                ),
            ),
        ).toCategories { CategoryKey.ShipType(it) }

        assertEquals(listOf(CategoryKey.ShipType("Cruiser")), categories.map { it.key })
    }

    @Test
    fun accountOverviewRequiresAUserName() {
        assertThrows(ApiException.NotFound::class.java) {
            YuyukoUserInfoData(
                userInfo = YuyukoUserDetail(accountId = 1, userName = null),
            ).toAccountOverview(
                fallbackAccountId = 1,
                language = ShipNameLanguage.ENGLISH,
                server = GameServer.ASIA,
            )
        }
    }

    @Test
    fun accountOverviewFallsBackToTheRequestedAccountId() {
        val overview = YuyukoUserInfoData(
            userInfo = YuyukoUserDetail(accountId = null, userName = "Yamato"),
            battleTypeInfo = mapOf("PVP" to YuyukoModeSummary(shipInfo = block)),
            lastBattleTime = 1_767_211_200,
        ).toAccountOverview(
            fallbackAccountId = 3_002_439_652,
            language = ShipNameLanguage.ENGLISH,
            server = GameServer.ASIA,
        )

        assertEquals(3_002_439_652L, overview.account.accountId)
        assertEquals("Yamato", overview.account.nickname)
        assertNull(overview.account.createdAt)
        assertEquals(1, overview.modes.size)
    }
}
