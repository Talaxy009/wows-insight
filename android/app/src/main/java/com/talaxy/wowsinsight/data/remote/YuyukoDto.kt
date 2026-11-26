package com.talaxy.wowsinsight.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Every Yuyuko endpoint answers with this envelope. */
@Serializable
data class YuyukoEnvelope<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class YuyukoSearchUser(
    val accountId: Long,
    val userName: String,
)

@Serializable
data class YuyukoUserInfoData(
    val userInfo: YuyukoUserDetail? = null,
    val battleTypeInfo: Map<String, YuyukoModeSummary>? = null,
    /** Keyed by ship type code, then by battle type. */
    val shipTypeInfo: Map<String, Map<String, YuyukoModeSummary>>? = null,
    /** Keyed by tier (`"1"` … `"11"`), then by battle type. */
    val levelInfo: Map<String, Map<String, YuyukoModeSummary>>? = null,
    val lastBattleTime: Int? = null,
)

@Serializable
data class YuyukoUserDetail(
    val accountId: Long? = null,
    val userName: String? = null,
    val accountCreateTime: Int? = null,
    /** Absolute URL of the in-game dog tag image. */
    val dogTag: String? = null,
    /** Blank strings when the account is not in a clan. */
    val clanInfo: YuyukoClanInfo? = null,
)

@Serializable
data class YuyukoClanInfo(
    val tag: String? = null,
    val name: String? = null,
) {
    /** "[TAG] Name", or whichever part is present; `null` without a clan. */
    val displayName: String?
        get() {
            val tag = tag?.trim().orEmpty()
            val name = name?.trim().orEmpty()
            return when {
                tag.isNotEmpty() && name.isNotEmpty() -> "[$tag] $name"
                name.isNotEmpty() -> name
                tag.isNotEmpty() -> "[$tag]"
                else -> null
            }
        }
}

@Serializable
data class YuyukoRecentDaysData(
    val recentInfo: List<YuyukoRecentDay>? = null,
)

@Serializable
data class YuyukoRecentDay(
    /** Milliseconds since the epoch, when the record was taken. */
    val recordTime: Long? = null,
    val battleTypeInfoList: Map<String, YuyukoModeSummary>? = null,
)

@Serializable
data class YuyukoModeSummary(
    val prInfo: YuyukoRating? = null,
    /** `account/user/info2` nests the totals under `shipInfo`. */
    val shipInfo: YuyukoStatsBlock? = null,
    /** `account/ship/info/list` nests the totals under `battleInfo`. */
    val battleInfo: YuyukoStatsBlock? = null,
) {
    val stats: YuyukoStatsBlock? get() = battleInfo ?: shipInfo
}

@Serializable
data class YuyukoShipListData(
    val userInfo: YuyukoUserDetail? = null,
    val shipInfo: List<YuyukoShipEntry>? = null,
)

@Serializable
data class YuyukoShipEntry(
    val shipInfo: YuyukoShipMeta? = null,
    val typeInfo: Map<String, YuyukoModeSummary>? = null,
)

@Serializable
data class YuyukoShipMeta(
    val shipId: Long,
    val nameCn: String? = null,
    val nameCn360: String? = null,
    val nameEnglish: String? = null,
    val level: Int? = null,
    val shipType: String? = null,
    val country: String? = null,
    val imgSmall: String? = null,
)

@Serializable
data class YuyukoRating(
    /** `0` means "not rated yet"; higher values are rating tiers. */
    val code: Int? = null,
    val value: Int? = null,
    val name: String? = null,
)

@Serializable
data class YuyukoStatsBlock(
    val battleInfo: YuyukoTotals? = null,
    val fragsInfo: YuyukoFrags? = null,
    val maxInfo: YuyukoMax? = null,
    val hitRatioInfo: YuyukoHitRatio? = null,
    val lastBattleTime: Int? = null,
)

@Serializable
data class YuyukoTotals(
    val battle: Int? = null,
    val wins: Int? = null,
    val losses: Int? = null,
    val survived: Int? = null,
    val winAndSurvived: Int? = null,
    /** Lifetime totals can exceed Int.MAX_VALUE. */
    val xp: Long? = null,
    val damageDealt: Long? = null,
    val scoutingDamage: Long? = null,
    val shipsSpotted: Int? = null,
    val planesKilled: Int? = null,
    val artAgro: Long? = null,
)

@Serializable
data class YuyukoFrags(
    val frags: Int? = null,
)

@Serializable
data class YuyukoHitRatio(
    val ratioMain: Double? = null,
)

@Serializable
data class YuyukoMax(
    val maxFrags: YuyukoMaxValue? = null,
    val maxDamageDealt: YuyukoMaxValue? = null,
    val maxScoutingDamage: YuyukoMaxValue? = null,
    val maxPlanesKilled: YuyukoMaxValue? = null,
    val maxShipsSpotted: YuyukoMaxValue? = null,
    val maxTotalAgro: YuyukoMaxValue? = null,
    val maxXp: YuyukoMaxValue? = null,
)

@Serializable
data class YuyukoMaxValue(
    val shipId: Long? = null,
    val value: Long? = null,
)

/** Request body of the account search endpoint. */
@Serializable
data class YuyukoSearchRequest(
    val userName: String,
    val one: Boolean,
    val limit: Int,
)
