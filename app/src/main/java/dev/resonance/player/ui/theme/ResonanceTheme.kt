package dev.resonance.player.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.resonance.player.core.AlbumScheme
import dev.resonance.player.core.SchemeColors

/** Used when the system has no wallpaper colors (Android 11 and older) and nothing is playing. */
const val DEFAULT_SEED = 0xFF4F6BD8.toInt()

fun SchemeColors.toColorScheme(dark: Boolean): ColorScheme {
    fun c(argb: Int) = Color(argb)
    return if (dark) {
        darkColorScheme(
            primary = c(primary), onPrimary = c(onPrimary),
            primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
            inversePrimary = c(inversePrimary),
            secondary = c(secondary), onSecondary = c(onSecondary),
            secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
            tertiary = c(tertiary), onTertiary = c(onTertiary),
            tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
            background = c(background), onBackground = c(onBackground),
            surface = c(surface), onSurface = c(onSurface),
            surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
            surfaceDim = c(surfaceDim), surfaceBright = c(surfaceBright),
            surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow),
            surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
            surfaceContainerHighest = c(surfaceContainerHighest),
            inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
            outline = c(outline), outlineVariant = c(outlineVariant),
            error = c(error), onError = c(onError),
            errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        )
    } else {
        lightColorScheme(
            primary = c(primary), onPrimary = c(onPrimary),
            primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
            inversePrimary = c(inversePrimary),
            secondary = c(secondary), onSecondary = c(onSecondary),
            secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
            tertiary = c(tertiary), onTertiary = c(onTertiary),
            tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
            background = c(background), onBackground = c(onBackground),
            surface = c(surface), onSurface = c(onSurface),
            surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
            surfaceDim = c(surfaceDim), surfaceBright = c(surfaceBright),
            surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow),
            surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
            surfaceContainerHighest = c(surfaceContainerHighest),
            inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
            outline = c(outline), outlineVariant = c(outlineVariant),
            error = c(error), onError = c(onError),
            errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        )
    }
}

/** App-wide theme: Material You wallpaper colors on Android 12+, a fixed palette before that. */
@Composable
fun ResonanceTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        AlbumScheme.generate(DEFAULT_SEED, dark).toColorScheme(dark)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Re-themes [content] with colors taken from the album cover, fading between songs. */
@Composable
fun AlbumTheme(seed: Int?, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val target = AlbumScheme.generate(seed ?: DEFAULT_SEED, dark).toColorScheme(dark)

    @Composable
    fun fade(color: Color): Color =
        animateColorAsState(color, tween(600), label = "albumColor").value

    val animated = target.copy(
        primary = fade(target.primary), onPrimary = fade(target.onPrimary),
        primaryContainer = fade(target.primaryContainer), onPrimaryContainer = fade(target.onPrimaryContainer),
        secondary = fade(target.secondary), onSecondary = fade(target.onSecondary),
        secondaryContainer = fade(target.secondaryContainer), onSecondaryContainer = fade(target.onSecondaryContainer),
        tertiary = fade(target.tertiary), onTertiary = fade(target.onTertiary),
        tertiaryContainer = fade(target.tertiaryContainer), onTertiaryContainer = fade(target.onTertiaryContainer),
        background = fade(target.background), onBackground = fade(target.onBackground),
        surface = fade(target.surface), onSurface = fade(target.onSurface),
        surfaceVariant = fade(target.surfaceVariant), onSurfaceVariant = fade(target.onSurfaceVariant),
        surfaceContainer = fade(target.surfaceContainer),
        surfaceContainerHigh = fade(target.surfaceContainerHigh),
        surfaceContainerHighest = fade(target.surfaceContainerHighest),
        outline = fade(target.outline), outlineVariant = fade(target.outlineVariant),
    )
    MaterialTheme(colorScheme = animated, content = content)
}
