package dev.resonance.player.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.resonance.player.MainViewModel
import dev.resonance.player.data.Track

/** Track menu actions, including the "add to playlist" dialog that they open. */
@Composable
fun rememberTrackActions(vm: MainViewModel): TrackActions {
    var pending by remember { mutableStateOf<Track?>(null) }
    val playlists by vm.playlists.collectAsState()

    pending?.let { track ->
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { vm.addToPlaylist(it.id, listOf(track)); pending = null },
            onCreate = { vm.createPlaylist(it, listOf(track)); pending = null },
            onDismiss = { pending = null },
        )
    }
    return remember(vm) {
        TrackActions(
            playNext = { vm.player.playNext(it) },
            addToQueue = { vm.player.addToQueue(listOf(it)) },
            addToPlaylist = { pending = it },
            isFavorite = { vm.isFavorite(it.id) },
            toggleFavorite = { vm.toggleFavorite(it.id) },
        )
    }
}
