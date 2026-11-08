@file:OptIn(ExperimentalMaterial3Api::class)

package dev.resonance.player.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.ui.common.Cover
import dev.resonance.player.ui.common.EmptyState
import dev.resonance.player.ui.common.rememberImagePicker

/** Edit a song's title, artist and album, and give it its own cover. The audio file is never changed. */
@Composable
fun EditTrackScreen(trackId: Long, vm: MainViewModel, onBack: () -> Unit, onEditLyrics: (Long) -> Unit) {
    val library by vm.library.collectAsState()
    val track = remember(library, trackId) { library.tracks.firstOrNull { it.id == trackId } }
    val coverVersion by vm.files.coverVersion.collectAsState()
    val hasCustomCover = remember(coverVersion, trackId) { vm.hasCustomCover(trackId) }
    val pickCover = rememberImagePicker { uri, done -> vm.setCover(trackId, uri, done) }

    var title by remember(trackId, track == null) { mutableStateOf(track?.title.orEmpty()) }
    var artist by remember(trackId, track == null) { mutableStateOf(track?.artist.orEmpty()) }
    var album by remember(trackId, track == null) { mutableStateOf(track?.album.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (track != null) {
                        IconButton(
                            onClick = { vm.saveTrackInfo(trackId, title, artist, album); onBack() },
                            enabled = title.isNotBlank(),
                        ) {
                            Icon(Icons.Rounded.Check, stringResource(R.string.save))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (track == null) {
            EmptyState(stringResource(R.string.edit_not_found), Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Reloads by itself when the cover changes: Cover watches the loader's version.
            Cover(vm.artwork, track.id, track.albumId, 168.dp, corner = 28.dp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = pickCover) {
                    Icon(Icons.Rounded.Image, null)
                    Text(stringResource(R.string.edit_cover_choose), modifier = Modifier.padding(start = 8.dp))
                }
                if (hasCustomCover) {
                    TextButton(onClick = { vm.removeCover(trackId) }) { Text(stringResource(R.string.edit_cover_remove)) }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(200) },
                label = { Text(stringResource(R.string.edit_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it.take(200) },
                label = { Text(stringResource(R.string.edit_field_artist)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = album,
                onValueChange = { album = it.take(200) },
                label = { Text(stringResource(R.string.edit_field_album)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.edit_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { onEditLyrics(trackId) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lyrics_edit_title))
            }
            val original = vm.originalTrack(trackId)
            if (original != null && (original.title != track.title || original.artist != track.artist || original.album != track.album)) {
                TextButton(
                    onClick = {
                        vm.resetTrackInfo(trackId)
                        title = original.title
                        artist = original.artist
                        album = original.album
                    },
                ) { Text(stringResource(R.string.edit_reset)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
