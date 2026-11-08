package dev.resonance.player.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.resonance.player.R
import dev.resonance.player.core.TimeFormat
import dev.resonance.player.data.PlaylistSummary
import dev.resonance.player.data.Track
import dev.resonance.player.playback.ArtworkLoader

/** Square album art with a tinted placeholder while it loads or when there is none. */
@Composable
fun Cover(
    loader: ArtworkLoader,
    trackId: Long,
    albumId: Long,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceIn(64, 1024)
    val coverVersion by loader.version.collectAsState()
    val bitmap by produceState<android.graphics.Bitmap?>(null, trackId, albumId, px, coverVersion) {
        value = loader.load(trackId, albumId, px)
    }
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val art = bitmap
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

/** What a track's overflow menu can do. */
class TrackActions(
    val playNext: (Track) -> Unit,
    val addToQueue: (Track) -> Unit,
    val addToPlaylist: (Track) -> Unit,
    val isFavorite: (Track) -> Boolean,
    val toggleFavorite: (Track) -> Unit,
)

@Composable
fun TrackRow(
    track: Track,
    loader: ArtworkLoader,
    isCurrent: Boolean,
    onClick: () -> Unit,
    actions: TrackActions,
    modifier: Modifier = Modifier,
    extraMenu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val nav = LocalNavigator.current
    val artist = track.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        leadingContent = { Cover(loader, track.id, track.albumId, 48.dp, corner = 10.dp) },
        headlineContent = {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    TimeFormat.clock(track.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_play_next)) },
                            onClick = { menuOpen = false; actions.playNext(track) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_queue)) },
                            onClick = { menuOpen = false; actions.addToQueue(track) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_playlist)) },
                            onClick = { menuOpen = false; actions.addToPlaylist(track) },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (actions.isFavorite(track)) R.string.menu_unfavorite else R.string.menu_favorite,
                                    ),
                                )
                            },
                            onClick = { menuOpen = false; actions.toggleFavorite(track) },
                        )
                        if (track.artist.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_go_artist)) },
                                onClick = { menuOpen = false; nav.openArtist(track.artist) },
                            )
                        }
                        if (track.albumId >= 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_go_album)) },
                                onClick = { menuOpen = false; nav.openAlbum(track.albumId) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit_info)) },
                            onClick = { menuOpen = false; nav.editTrack(track.id) },
                        )
                        extraMenu?.invoke { menuOpen = false }
                    }
                }
            }
        },
    )
}

/** Dialog with a single text field, used to create and rename playlists. */
@Composable
fun NameDialog(
    title: String,
    confirmLabel: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(60) },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Pick an existing playlist or start a new one for the songs about to be added. */
@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistSummary>,
    onPick: (PlaylistSummary) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog(
            title = stringResource(R.string.playlist_new),
            confirmLabel = stringResource(R.string.playlist_create),
            onConfirm = onCreate,
            onDismiss = { creating = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist_title)) },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        modifier = Modifier.clickable { creating = true },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.playlist_new)) },
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onPick(playlist) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
