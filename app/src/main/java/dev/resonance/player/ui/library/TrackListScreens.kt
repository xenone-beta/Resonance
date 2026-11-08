package dev.resonance.player.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.core.TimeFormat
import dev.resonance.player.data.Track
import dev.resonance.player.ui.common.EmptyState
import dev.resonance.player.ui.common.NameDialog
import dev.resonance.player.ui.common.TrackRow
import dev.resonance.player.ui.common.rememberTrackActions
import dev.resonance.player.ui.player.MiniPlayer
import dev.resonance.player.ui.common.screenColor
import dev.resonance.player.ui.common.transparentTopBarColors
import dev.resonance.player.ui.common.barColor

/** Songs of one album or one artist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    title: String,
    tracks: List<Track>,
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val playerState by vm.player.state.collectAsState()
    val actions = rememberTrackActions(vm)
    Scaffold(
        containerColor = screenColor(),
        topBar = { ListTopBar(title, onBack) },
        bottomBar = { MiniPlayer(vm, onOpenPlayer) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
        ) {
            item(key = "header") { PlayButtons(tracks, vm) }
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    loader = vm.artwork,
                    isCurrent = playerState.current?.trackId == track.id,
                    onClick = { vm.player.play(tracks, startIndex = index) },
                    actions = actions,
                )
            }
        }
    }
}

/** One playlist: play it, reorder-free editing (remove songs), rename, delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    id: Long,
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val nameFlow = remember(id) { vm.playlistName(id) }
    val itemsFlow = remember(id) { vm.playlistItems(id) }
    val name by nameFlow.collectAsState(initial = "")
    val items by itemsFlow.collectAsState(initial = emptyList())
    val playerState by vm.player.state.collectAsState()
    val actions = rememberTrackActions(vm)
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val playable = items.mapNotNull { it.track }

    if (renaming) {
        NameDialog(
            title = stringResource(R.string.rename),
            confirmLabel = stringResource(R.string.rename),
            initial = name.orEmpty(),
            onConfirm = { vm.renamePlaylist(id, it); renaming = false },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.playlist_delete_title, name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = { deleting = false; vm.deletePlaylist(id); onBack() }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    Scaffold(
        containerColor = screenColor(),
        topBar = {
            ListTopBar(
                title = name.orEmpty(),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.rename))
                    }
                    IconButton(onClick = { deleting = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                    }
                },
            )
        },
        bottomBar = { MiniPlayer(vm, onOpenPlayer) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
        ) {
            if (items.isEmpty()) {
                item(key = "empty") { EmptyState(stringResource(R.string.playlist_empty)) }
            } else {
                item(key = "header") { PlayButtons(playable, vm) }
            }
            itemsIndexed(items, key = { _, item -> item.entryId }) { _, item ->
                val track = item.track
                if (track == null) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.missing_track)) },
                        trailingContent = {
                            IconButton(onClick = { vm.removeFromPlaylist(item.entryId) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove))
                            }
                        },
                    )
                } else {
                    TrackRow(
                        track = track,
                        loader = vm.artwork,
                        isCurrent = playerState.current?.trackId == track.id,
                        onClick = { vm.player.play(playable, startIndex = playable.indexOf(track)) },
                        actions = actions,
                        extraMenu = { dismiss ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_from_playlist)) },
                                onClick = { dismiss(); vm.removeFromPlaylist(item.entryId) },
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        colors = transparentTopBarColors(),
        title = { Text(title.ifEmpty { "…" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = actions,
    )
}

@Composable
private fun PlayButtons(tracks: List<Track>, vm: MainViewModel) {
    if (tracks.isEmpty()) return
    val total = tracks.sumOf { it.durationMs }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            pluralStringResource(R.plurals.songs_count, tracks.size, tracks.size) + " · " + TimeFormat.clock(total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { vm.player.play(tracks) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.PlayArrow, null)
                Text(stringResource(R.string.play_all), modifier = Modifier.padding(start = 8.dp))
            }
            FilledTonalButton(onClick = { vm.player.play(tracks, shuffle = true) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Shuffle, null)
                Text(stringResource(R.string.shuffle), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
