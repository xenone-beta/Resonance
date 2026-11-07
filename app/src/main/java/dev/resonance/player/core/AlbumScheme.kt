package dev.resonance.player.core

/** All the Material 3 color roles the app uses, as plain ARGB ints (no Compose dependency, so it is testable). */
data class SchemeColors(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val inversePrimary: Int,
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val surfaceDim: Int,
    val surfaceBright: Int,
    val surfaceContainerLowest: Int,
    val surfaceContainerLow: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val inverseSurface: Int,
    val inverseOnSurface: Int,
    val outline: Int,
    val outlineVariant: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,
)

/**
 * Builds a Material 3 style color scheme from one "seed" color, typically the dominant color
 * of the album cover. Tones follow the Material 3 baseline mapping for light and dark themes.
 */
object AlbumScheme {

    fun generate(seedArgb: Int, dark: Boolean): SchemeColors {
        val seed = ColorScience.argbToLch(seedArgb)
        val hue = seed.hue

        // Gray covers give a calm scheme; saturated covers a colorful one, but never garish.
        val primaryChroma = seed.chroma.coerceIn(10.0, 48.0)
        val secondaryChroma = (primaryChroma * 0.4).coerceIn(4.0, 18.0)
        val tertiaryChroma = (primaryChroma * 0.65).coerceIn(8.0, 32.0)
        val neutralChroma = (primaryChroma * 0.1).coerceIn(1.0, 5.0)
        val neutralVariantChroma = (primaryChroma * 0.18).coerceIn(2.0, 8.0)

        fun primary(tone: Double) = ColorScience.lchToArgb(tone, primaryChroma, hue)
        fun secondary(tone: Double) = ColorScience.lchToArgb(tone, secondaryChroma, hue)
        fun tertiary(tone: Double) = ColorScience.lchToArgb(tone, tertiaryChroma, hue + 60.0)
        fun neutral(tone: Double) = ColorScience.lchToArgb(tone, neutralChroma, hue)
        fun variant(tone: Double) = ColorScience.lchToArgb(tone, neutralVariantChroma, hue)

        return if (dark) {
            SchemeColors(
                primary = primary(80.0), onPrimary = primary(20.0),
                primaryContainer = primary(30.0), onPrimaryContainer = primary(90.0),
                inversePrimary = primary(40.0),
                secondary = secondary(80.0), onSecondary = secondary(20.0),
                secondaryContainer = secondary(30.0), onSecondaryContainer = secondary(90.0),
                tertiary = tertiary(80.0), onTertiary = tertiary(20.0),
                tertiaryContainer = tertiary(30.0), onTertiaryContainer = tertiary(90.0),
                background = neutral(6.0), onBackground = neutral(90.0),
                surface = neutral(6.0), onSurface = neutral(90.0),
                surfaceVariant = variant(30.0), onSurfaceVariant = variant(80.0),
                surfaceDim = neutral(6.0), surfaceBright = neutral(24.0),
                surfaceContainerLowest = neutral(4.0), surfaceContainerLow = neutral(10.0),
                surfaceContainer = neutral(12.0), surfaceContainerHigh = neutral(17.0),
                surfaceContainerHighest = neutral(22.0),
                inverseSurface = neutral(90.0), inverseOnSurface = neutral(20.0),
                outline = variant(60.0), outlineVariant = variant(30.0),
                error = 0xFFF2B8B5.toInt(), onError = 0xFF601410.toInt(),
                errorContainer = 0xFF8C1D18.toInt(), onErrorContainer = 0xFFF9DEDC.toInt(),
            )
        } else {
            SchemeColors(
                primary = primary(40.0), onPrimary = primary(100.0),
                primaryContainer = primary(90.0), onPrimaryContainer = primary(10.0),
                inversePrimary = primary(80.0),
                secondary = secondary(40.0), onSecondary = secondary(100.0),
                secondaryContainer = secondary(90.0), onSecondaryContainer = secondary(10.0),
                tertiary = tertiary(40.0), onTertiary = tertiary(100.0),
                tertiaryContainer = tertiary(90.0), onTertiaryContainer = tertiary(10.0),
                background = neutral(98.0), onBackground = neutral(10.0),
                surface = neutral(98.0), onSurface = neutral(10.0),
                surfaceVariant = variant(90.0), onSurfaceVariant = variant(30.0),
                surfaceDim = neutral(87.0), surfaceBright = neutral(98.0),
                surfaceContainerLowest = neutral(100.0), surfaceContainerLow = neutral(96.0),
                surfaceContainer = neutral(94.0), surfaceContainerHigh = neutral(92.0),
                surfaceContainerHighest = neutral(90.0),
                inverseSurface = neutral(20.0), inverseOnSurface = neutral(95.0),
                outline = variant(50.0), outlineVariant = variant(80.0),
                error = 0xFFB3261E.toInt(), onError = 0xFFFFFFFF.toInt(),
                errorContainer = 0xFFF9DEDC.toInt(), onErrorContainer = 0xFF410E0B.toInt(),
            )
        }
    }
}
