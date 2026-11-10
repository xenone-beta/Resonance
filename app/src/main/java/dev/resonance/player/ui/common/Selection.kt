package dev.resonance.player.ui.common

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.data.Track

/** The songs ticked in a list. A long press starts selecting; taps then add or remove songs. */
class TrackSelection {
    var ids by mutableStateOf(emptySet<Long>())
        private set

    val active: Boolean get() = ids.isNotEmpty()

    fun contains(id: Long) = id in ids

    fun toggle(id: Long) { ids = if (id in ids) ids - id else ids + id }

    fun selectAll(tracks: List<Track>) { ids = tracks.map { it.id }.toSet() }

    fun clear() { ids = emptySet() }
}

@Composable
fun rememberTrackSelection(): TrackSelection = remember { TrackSelection() }

/**
 * Bar shown above the mini player while songs are selected.
 * [tracks] is the list the selection was made in, so order is kept.
 */
@Composable
fun SelectionBar(selection: TrackSelection, tracks: List<Track>, vm: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playlists by vm.playlists.collectAsState()
    var picking by remember { mutableStateOf(false) }
    val chosen = tracks.filter { selection.contains(it.id) }

    BackHandler(enabled = selection.active) { selection.clear() }

    if (picking && chosen.isNotEmpty()) {
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = {
                vm.addToPlaylist(it.id, chosen)
                Toast.makeText(context, context.getString(R.string.select_added, chosen.size), Toast.LENGTH_SHORT).show()
                picking = false
                selection.clear()
            },
            onCreate = {
                vm.createPlaylist(it, chosen)
                Toast.makeText(context, context.getString(R.string.select_added, chosen.size), Toast.LENGTH_SHORT).show()
                picking = false
                selection.clear()
            },
            onDismiss = { picking = false },
        )
    }

    AnimatedVisibility(
        visible = selection.active,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = selection::clear) { Icon(Icons.Rounded.Close, stringResource(R.string.select_clear)) }
                Text(
                    stringResource(R.string.selected_count, selection.ids.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { selection.selectAll(tracks) }) { Icon(Icons.Rounded.SelectAll, stringResource(R.string.select_all)) }
                TextButton(onClick = {
                    vm.player.addToQueue(chosen)
                    Toast.makeText(context, context.getString(R.string.select_added, chosen.size), Toast.LENGTH_SHORT).show()
                    selection.clear()
                }) { Text(stringResource(R.string.select_to_queue)) }
                Button(onClick = { picking = true }) { Text(stringResource(R.string.select_to_playlist)) }
            }
        }
    }
}
