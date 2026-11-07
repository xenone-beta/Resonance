package dev.resonance.player

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.resonance.player.ui.equalizer.EqualizerScreen
import dev.resonance.player.ui.library.LibraryScreen
import dev.resonance.player.ui.library.PlaylistScreen
import dev.resonance.player.ui.library.TrackListScreen
import dev.resonance.player.ui.player.PlayerScreen
import dev.resonance.player.ui.player.QueueScreen
import dev.resonance.player.ui.stats.StatsScreen

@Composable
fun ResonanceApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val library by vm.library.collectAsState()
    val back: () -> Unit = { nav.popBackStack() }
    val openPlayer: () -> Unit = { nav.navigate("player") { launchSingleTop = true } }

    NavHost(navController = nav, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                vm = vm,
                onOpenPlayer = openPlayer,
                onOpenAlbum = { nav.navigate("album/$it") },
                onOpenArtist = { nav.navigate("artist/${Uri.encode(it)}") },
                onOpenPlaylist = { nav.navigate("playlist/$it") },
                onOpenEqualizer = { nav.navigate("equalizer") },
                onOpenStats = { nav.navigate("stats") },
            )
        }
        composable("player") { PlayerScreen(vm, onBack = back, onOpenQueue = { nav.navigate("queue") }) }
        composable("queue") { QueueScreen(vm, onBack = back) }
        composable("equalizer") { EqualizerScreen(vm, onBack = back) }
        composable("stats") { StatsScreen(vm, onBack = back) }
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

@Composable
private fun stringResourceUnknownAlbum() = androidx.compose.ui.res.stringResource(R.string.unknown_album)

@Composable
private fun stringResourceUnknownArtist() = androidx.compose.ui.res.stringResource(R.string.unknown_artist)
