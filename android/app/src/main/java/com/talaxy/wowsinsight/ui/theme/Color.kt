package com.talaxy.wowsinsight.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.talaxy.wowsinsight.data.model.RatingTier

internal val LightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF0F0F4),
    inversePrimary = Color(0xFF9ECAFF),
    surfaceTint = Color(0xFF0061A4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F9),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE0E2E8),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF0061A4),
    surfaceTint = Color(0xFF9ECAFF),
    surfaceContainerLowest = Color(0xFF0B0F13),
    surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1C2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
)

/**
 * The fixed wows-numbers colours, brightened for dark surfaces so the text and
 * bars keep enough contrast. These deliberately do not use the dynamic palette:
 * a rating tier means the same thing regardless of wallpaper.
 */
fun ratingColor(tier: RatingTier, dark: Boolean): Color = when (tier) {
    RatingTier.UNKNOWN -> if (dark) Color(0xFF90A4AE) else Color(0xFF607D8B)
    RatingTier.BAD -> if (dark) Color(0xFFEF5350) else Color(0xFFD32F2F)
    RatingTier.BELOW_AVERAGE -> if (dark) Color(0xFFFFA726) else Color(0xFFFF9800)
    RatingTier.AVERAGE -> if (dark) Color(0xFFFFCA28) else Color(0xFFFFB300)
    RatingTier.GOOD -> if (dark) Color(0xFF9CCC65) else Color(0xFF7CB342)
    RatingTier.VERY_GOOD -> if (dark) Color(0xFF66BB6A) else Color(0xFF388E3C)
    RatingTier.GREAT -> if (dark) Color(0xFF4FC3F7) else Color(0xFF03A9F4)
    RatingTier.UNICUM -> if (dark) Color(0xFFBA68C8) else Color(0xFF9C27B0)
    RatingTier.SUPER_UNICUM -> if (dark) Color(0xFF9575CD) else Color(0xFF673AB7)
}
