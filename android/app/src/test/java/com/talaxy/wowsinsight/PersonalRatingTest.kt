package com.talaxy.wowsinsight

import com.talaxy.wowsinsight.data.model.RatingTier
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalRatingTest {
    @Test
    fun tierBoundariesMatchWowsNumbers() {
        assertEquals(RatingTier.BAD, RatingTier.forRating(0))
        assertEquals(RatingTier.BAD, RatingTier.forRating(749))
        assertEquals(RatingTier.BELOW_AVERAGE, RatingTier.forRating(750))
        assertEquals(RatingTier.BELOW_AVERAGE, RatingTier.forRating(1_099))
        assertEquals(RatingTier.AVERAGE, RatingTier.forRating(1_100))
        assertEquals(RatingTier.GOOD, RatingTier.forRating(1_350))
        assertEquals(RatingTier.VERY_GOOD, RatingTier.forRating(1_550))
        assertEquals(RatingTier.GREAT, RatingTier.forRating(1_750))
        assertEquals(RatingTier.UNICUM, RatingTier.forRating(2_100))
        assertEquals(RatingTier.SUPER_UNICUM, RatingTier.forRating(2_450))
        assertEquals(RatingTier.SUPER_UNICUM, RatingTier.forRating(9_999))
    }

    @Test
    fun negativeRatingIsUnknown() {
        assertEquals(RatingTier.UNKNOWN, RatingTier.forRating(-1))
    }
}
