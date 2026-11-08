package dev.resonance.player

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.resonance.player.ui.common.LocalHasBackground
import dev.resonance.player.ui.common.LocalNavigator
import dev.resonance.player.ui.common.Navigator
import dev.resonance.player.ui.edit.EditTrackScreen
import dev.resonance.player.ui.edit.LyricsEditorScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.resonance.player.ui.about.AboutScreen
import dev.resonance.player.ui.equalizer.EqualizerScreen
import dev.resonance.player.ui.library.LibraryScreen
import dev.resonance.player.ui.library.PlaylistScreen
import dev.resonance.player.ui.library.TrackListScreen
import dev.resonance.player.ui.player.PlayerScreen
import dev.resonance.player.ui.player.QueueScreen
import dev.resonance.player.ui.settings.SettingsScreen
import dev.resonance.player.ui.stats.StatsScreen

@Composable
fun ResonanceApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val library by vm.library.collectAsState()
    val back: () -> Unit = { nav.popBackStack() }
    val openPlayer: () -> Unit = { nav.navigate("player") { launchSingleTop = true } }

    val photo by vm.background.collectAsState()
    val dim by vm.settings.backgroundDim.collectAsState()
    val navigator = remember(nav) {
        Navigator(
            openAlbum = { nav.navigate("album/$it") },
            openArtist = { nav.navigate("artist/${Uri.encode(it)}") },
            editTrack = { nav.navigate("edit/$it") },
            editLyrics = { nav.navigate("lyrics-edit/$it") },
        )
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        photo?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // A veil in the theme's own color keeps text readable in light and dark themes.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = dim)))
        }
    CompositionLocalProvider(LocalHasBackground provides (photo != null), LocalNavigator provides navigator) {
    NavHost(
        navController = nav,
        startDestination = "library",
        enterTransition = { slideInHorizontally(tween(280)) { it / 8 } + fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(tween(240)) { it / 8 } + fadeOut(tween(240)) },
    ) {
        composable("library") {
            LibraryScreen(
                vm = vm,
                onOpenPlayer = openPlayer,
                onOpenAlbum = { nav.navigate("album/$it") },
                onOpenArtist = { nav.navigate("artist/${Uri.encode(it)}") },
                onOpenPlaylist = { nav.navigate("playlist/$it") },
                onOpenFolder = { nav.navigate("folder/${Uri.encode(it)}") },
                onOpenFavorites = { nav.navigate("favorites") },
                onOpenSmart = { nav.navigate("smart/$it") },
                onOpenStats = { nav.navigate("stats") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable(
            "player",
            // The full player slides up from the mini player and back down.
            enterTransition = { slideInVertically(tween(350)) { it / 3 } + fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { slideOutVertically(tween(300)) { it / 3 } + fadeOut(tween(300)) },
        ) {
            PlayerScreen(
                vm,
                onBack = back,
                onOpenQueue = { nav.navigate("queue") },
                onOpenEqualizer = { nav.navigate("equalizer") },
            )
        }
        composable("queue") { QueueScreen(vm, onBack = back) }
        composable("equalizer") { EqualizerScreen(vm, onBack = back) }
        composable("stats") { Plain { StatsScreen(vm, onBack = back) } }
        composable("settings") { Plain { SettingsScreen(vm, onBack = back, onOpenAbout = { nav.navigate("about") }) } }
        composable("about") { Plain { AboutScreen(onBack = back) } }
        composable(
            "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            Plain { EditTrackScreen(entry.arguments?.getLong("id") ?: -1L, vm, onBack = back, onEditLyrics = navigator.editLyrics) }
        }
        composable(
            "lyrics-edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            Plain { LyricsEditorScreen(entry.arguments?.getLong("id") ?: -1L, vm, onBack = back) }
        }
        composable(
            "smart/{kind}",
            arguments = listOf(navArgument("kind") { type = NavType.StringType }),
        ) { entry ->
            val kind = remember(entry) {
                runCatching { SmartList.valueOf(entry.arguments?.getString("kind").orEmpty()) }.getOrDefault(SmartList.RECENT)
            }
            val flow = remember(kind) { vm.smartTracks(kind) }
            val tracks by flow.collectAsState(initial = emptyList())
            TrackListScreen(
                title = androidx.compose.ui.res.stringResource(
                    when (kind) {
                        SmartList.RECENT -> R.string.smart_recent
                        SmartList.MOST_PLAYED -> R.string.smart_most_played
                        SmartList.NEVER_PLAYED -> R.string.smart_never_played
                    },
                ),
                tracks = tracks,
                vm = vm,
                onBack = back,
                onOpenPlayer = openPlayer,
            )
        }
        composable("favorites") {
            val favorites by vm.favoriteTracks.collectAsState()
            TrackListScreen(
                title = androidx.compose.ui.res.stringResource(R.string.favorites_title),
                tracks = favorites,
                vm = vm,
                onBack = back,
                onOpenPlayer = openPlayer,
            )
        }
        composable(
            "folder/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType }),
        ) { entry ->
            val path = entry.arguments?.getString("path").orEmpty()
            val folder = remember(library, path) { library.folders.firstOrNull { it.path == path } }
            TrackListScreen(
                title = folder?.name.orEmpty(),
                tracks = folder?.tracks.orEmpty(),
                vm = vm,
                onBack = back,
                onOpenPlayer = openPlayer,
            )
        }
        composable(
            "album/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            val album = remember(library, id) { library.albums.firstOrNull { it.id == id } }
            TrackListScreen(
                title = album?.title.orEmpty().ifEmpty { stringResourceUnknownAlbum() },
                tracks = album?.tracks.orEmpty(),
                vm = vm,
                onBack = back,
                onOpenPlayer = openPlayer,
            )
        }
        composable(
            "artist/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType }),
        ) { entry ->
            val name = entry.arguments?.getString("name").orEmpty()
            val artist = remember(library, name) { library.artists.firstOrNull { it.name == name } }
            TrackListScreen(
                title = name.ifEmpty { stringResourceUnknownArtist() },
                tracks = artist?.tracks.orEmpty(),
                vm = vm,
                onBack = back,
                onOpenPlayer = openPlayer,
            )
        }
        composable(
            "playlist/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            PlaylistScreen(entry.arguments?.getLong("id") ?: -1L, vm, onBack = back, onOpenPlayer = openPlayer)
        }
    }
    }
    }
}

@Composable
private fun stringResourceUnknownAlbum() = androidx.compose.ui.res.stringResource(R.string.unknown_album)

@Composable
private fun stringResourceUnknownArtist() = androidx.compose.ui.res.stringResource(R.string.unknown_artist)

/** Settings, About, Statistics and the editors stay plain so their cards and fields remain readable. */
@Composable
private fun Plain(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHasBackground provides false) { content() }
}
