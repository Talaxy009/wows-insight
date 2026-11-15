package com.talaxy.wowsinsight.data.model

/**
 * A battle type reported by `account/user/info2` under `battleTypeInfo`.
 *
 * [apiCode] is the key used by the API. It is also present in every ship's
 * `typeInfo`, so the picker on the player screen filters both the totals and the
 * ship list. The declaration order is the picker order and unknown codes are
 * ignored rather than shown raw.
 */
enum class BattleMode(val apiCode: String) {
    PVP("PVP"),
    PVP_SOLO("PVP_SOLO"),
    PVP_DIV2("PVP_DIV2"),
    PVP_DIV3("PVP_DIV3"),
    RANKED("RANK"),
    RANKED_SOLO("RANK_SOLO"),
    RANKED_DIV2("RANK_DIV2"),
    RANKED_DIV3("RANK_DIV3"),
    RANKED_OLD("RANK_OLD"),
    RANKED_OLD_SOLO("RANK_OLD_SOLO"),
    RANKED_OLD_DIV2("RANK_OLD_DIV2"),
    RANKED_OLD_DIV3("RANK_OLD_DIV3"),
    CLAN("CLAN"),
    BRAWL("BRAWL"),
    OPERATIONS("OPER_MM"),
    COOP("PVE"),
    COOP_SOLO("PVE_SOLO"),
    COOP_DIV2("PVE_DIV2"),
    COOP_DIV3("PVE_DIV3"),
    RATING("RATING"),
    RATING_SOLO("RATING_SOLO"),
    RATING_DIV("RATING_DIV"),
    ;

    companion object {
        fun fromApi(code: String?): BattleMode? =
            entries.firstOrNull { it.apiCode == code }
    }
}
