package com.talaxy.wowsinsight.data.model

/** A personal rating value. A negative value means the API has no rating. */
data class PlayerRating(val value: Int) {
    val isValid: Boolean get() = value >= 0
    val tier: RatingTier get() = RatingTier.forRating(value)

    companion object {
        val Unknown = PlayerRating(-1)
    }
}

/**
 * Rating boundaries and labels used by wows-numbers.
 *
 * The declaration order is ascending, which [forRating] relies on.
 */
enum class RatingTier(val lowerBound: Int?) {
    UNKNOWN(null),
    BAD(0),
    BELOW_AVERAGE(750),
    AVERAGE(1_100),
    GOOD(1_350),
    VERY_GOOD(1_550),
    GREAT(1_750),
    UNICUM(2_100),
    SUPER_UNICUM(2_450),
    ;

    companion object {
        fun forRating(rating: Int): RatingTier {
            if (rating < 0) return UNKNOWN
            return entries.lastOrNull { (it.lowerBound ?: Int.MAX_VALUE) <= rating } ?: BAD
        }
    }
}
