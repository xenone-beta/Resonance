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
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.resonance.player.core.AlbumScheme
import dev.resonance.player.core.SchemeColors
import dev.resonance.player.data.AppSettings
import dev.resonance.player.data.ColorSource
import dev.resonance.player.data.ThemeMode

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

/** Resolved light/dark choice, so nested themes (the album-colored player) follow the app setting. */
data class Appearance(val dark: Boolean, val amoled: Boolean)

val LocalAppearance = staticCompositionLocalOf { Appearance(dark = false, amoled = false) }

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
)

/** App-wide theme: wallpaper colors (Android 12+) or a chosen accent; light, dark or pure black. */
@Composable
fun ResonanceTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val mode by settings.themeMode.collectAsState()
    val source by settings.colorSource.collectAsState()
    val accent by settings.accent.collectAsState()

    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        else -> isSystemInDarkTheme()
    }
    val amoled = mode == ThemeMode.AMOLED
    val context = LocalContext.current

    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && source == ColorSource.WALLPAPER) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        AlbumScheme.generate(accent, dark).toColorScheme(dark)
    }
    val scheme = if (amoled && dark) base.toAmoled() else base

    // Keep status and navigation bar icons readable on the chosen background.
    val activity = context as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
        )
    }

    CompositionLocalProvider(LocalAppearance provides Appearance(dark, amoled && dark)) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Re-themes [content] with colors taken from the album cover, fading between songs. Null keeps the app theme. */
@Composable
fun AlbumTheme(seed: Int?, content: @Composable () -> Unit) {
    if (seed == null) {
        content()
        return
    }
    val appearance = LocalAppearance.current
    val dark = appearance.dark
    var target = AlbumScheme.generate(seed, dark).toColorScheme(dark)
    if (appearance.amoled) target = target.toAmoled()

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
