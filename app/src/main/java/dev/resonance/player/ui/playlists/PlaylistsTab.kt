package dev.resonance.player.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.ui.common.EmptyState
import dev.resonance.player.ui.common.NameDialog

@Composable
fun PlaylistsTab(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val playlists by vm.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        NameDialog(
            title = stringResource(R.string.playlist_new),
            confirmLabel = stringResource(R.string.playlist_create),
            onConfirm = { vm.createPlaylist(it); creating = false },
            onDismiss = { creating = false },
        )
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "new") {
            ListItem(
                modifier = Modifier.clickable { creating = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                },
                headlineContent = { Text(stringResource(R.string.playlist_new)) },
            )
        }
        if (playlists.isEmpty()) {
            item(key = "empty") { EmptyState(stringResource(R.string.playlists_empty)) }
        }
        items(playlists, key = { it.id }) { playlist ->
            ListItem(
                modifier = Modifier.clickable { onOpen(playlist.id) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistPlay,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                },
                headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.songs_count, playlist.trackCount, playlist.trackCount))
                },
            )
        }
    }
}
