package com.talaxy.wowsinsight.ui.util

import com.talaxy.wowsinsight.data.model.PlayerRating
import com.talaxy.wowsinsight.data.model.RatingTier
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware display helpers shared by all screens. */
object StatFormat {
    /** Shown instead of a value the API did not provide. */
    const val PLACEHOLDER = "—"

    fun integer(value: Int): String = NumberFormat.getIntegerInstance().format(value)

    fun integer(value: Long): String = NumberFormat.getIntegerInstance().format(value)

    fun decimal(value: Double, digits: Int = 0): String =
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(value)

    fun percent(value: Double, digits: Int = 2): String =
        NumberFormat.getPercentInstance().apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(value / 100.0)

    fun date(timestampSeconds: Int?): String {
        val timestamp = timestampSeconds?.takeIf { it > 0 } ?: return PLACEHOLDER
        return dateFormatter
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(timestamp.toLong()))
    }

    fun date(day: LocalDate?): String = day?.format(dateFormatter) ?: PLACEHOLDER

    /** `MEDIUM` is the style iOS uses (`.abbreviated`): "2026年11月14日" / "Nov 14, 2026". */
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun ratingText(rating: PlayerRating): String =
        if (rating.isValid) integer(rating.value) else PLACEHOLDER

    fun tierRange(tier: RatingTier): String {
        val lower = tier.lowerBound ?: return PLACEHOLDER
        val next = RatingTier.entries.getOrNull(tier.ordinal + 1)?.lowerBound
            ?: return "${integer(lower)}+"
        return "${integer(lower)} – ${integer(next - 1)}"
    }

    /** `MMM d` in the user's language, e.g. "Nov 12" or "11月12日". */
    fun chartDay(day: LocalDate): String {
        val locale = Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
        return DateTimeFormatter.ofPattern(pattern, locale).format(day)
    }
}
