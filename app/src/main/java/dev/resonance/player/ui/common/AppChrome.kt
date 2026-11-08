@file:OptIn(ExperimentalMaterial3Api::class)

package dev.resonance.player.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** True on screens that let the user's background photo show through (everything but Settings and Statistics). */
val LocalHasBackground = compositionLocalOf { false }

/** Screen fill: transparent over a background photo, the normal background otherwise. */
@Composable
fun screenColor(): Color = if (LocalHasBackground.current) Color.Transparent else MaterialTheme.colorScheme.background

/** Container color for bars that normally use the surface color. */
@Composable
fun barColor(): Color = if (LocalHasBackground.current) Color.Transparent else MaterialTheme.colorScheme.surface

@Composable
fun transparentTopBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = barColor(),
        scrolledContainerColor = barColor(),
    )

/** Where a song's menu can lead. Provided once at the top of the app. */
class Navigator(
    val openAlbum: (Long) -> Unit,
    val openArtist: (String) -> Unit,
    val editTrack: (Long) -> Unit,
    val editLyrics: (Long) -> Unit,
)

val LocalNavigator = staticCompositionLocalOf { Navigator({}, {}, {}, {}) }
