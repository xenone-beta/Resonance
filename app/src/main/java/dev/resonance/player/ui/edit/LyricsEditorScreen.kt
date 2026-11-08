@file:OptIn(ExperimentalMaterial3Api::class)

package dev.resonance.player.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import kotlinx.coroutines.launch
import java.util.Locale

/** Write lyrics by hand or import a .lrc / .txt file. Stored inside the app, per song. */
@Composable
fun LyricsEditorScreen(trackId: Long, vm: MainViewModel, onBack: () -> Unit) {
    val library by vm.library.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val position by vm.player.position.collectAsState()
    val scope = rememberCoroutineScope()
    val songTitle = remember(library, trackId) { library.tracks.firstOrNull { it.id == trackId }?.title.orEmpty() }

    var text by remember(trackId) {
        val saved = vm.ownLyrics(trackId).orEmpty()
        mutableStateOf(TextFieldValue(saved, TextRange(saved.length)))
    }
    var importFailed by remember { mutableStateOf(false) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val loaded = vm.readTextFile(uri)
            if (loaded == null) importFailed = true else {
                importFailed = false
                text = TextFieldValue(loaded, TextRange(loaded.length))
            }
        }
    }
    // A timestamp only makes sense against the song that is actually playing.
    val canStamp = playerState.current?.trackId == trackId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lyrics_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.saveLyrics(trackId, text.text); onBack() }) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            if (songTitle.isNotEmpty()) {
                Text(songTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Text(
                stringResource(R.string.lyrics_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { importer.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.lyrics_import)) }
                OutlinedButton(
                    enabled = canStamp,
                    onClick = {
                        val stamp = timestamp(position)
                        val start = text.selection.min
                        val end = text.selection.max
                        val next = text.text.replaceRange(start, end, stamp)
                        text = TextFieldValue(next, TextRange(start + stamp.length))
                    },
                ) { Text(stringResource(R.string.lyrics_stamp)) }
            }
            if (importFailed) {
                Text(
                    stringResource(R.string.lyrics_import_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
                placeholder = { Text(stringResource(R.string.lyrics_edit_placeholder)) },
            )
            if (text.text.isNotEmpty()) {
                TextButton(onClick = { text = TextFieldValue("") }) { Text(stringResource(R.string.lyrics_clear)) }
            }
        }
    }
}

/** "[mm:ss.xx]" for a playback position. */
private fun timestamp(positionMs: Long): String {
    val total = positionMs.coerceAtLeast(0L)
    val minutes = total / 60_000
    val seconds = (total / 1000) % 60
    val hundredths = (total % 1000) / 10
    return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, hundredths)
}
