package dev.resonance.player.ui.player

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt
import androidx.compose.material3.OutlinedButton
import dev.resonance.player.ui.common.LocalNavigator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.resonance.player.LyricsState
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.core.Lrc
import kotlin.math.max

val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

fun pitchLabel(semitones: Int): String = if (semitones > 0) "+$semitones" else "$semitones"

fun speedLabel(value: Float): String {
    val text = String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return "$text×"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedSheetContent(vm: MainViewModel) {
    val speed by vm.settings.speed.collectAsState()
    val pitch by vm.settings.pitchSemitones.collectAsState()
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Text(stringResource(R.string.settings_speed), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.speed_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.speed_short), style = MaterialTheme.typography.titleLarge)
            Text(speedLabel(speed), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = speed,
            onValueChange = { vm.settings.setSpeed(it) },
            valueRange = 0.5f..2f,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.pitch_title), style = MaterialTheme.typography.titleLarge)
            Text(pitchLabel(pitch), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            stringResource(R.string.pitch_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = pitch.toFloat(),
            onValueChange = { vm.settings.setPitchSemitones(it.roundToInt()) },
            valueRange = -6f..6f,
            steps = 11,
        )
        if (speed != 1f || pitch != 0) {
            TextButton(onClick = { vm.settings.setSpeed(1f); vm.settings.setPitchSemitones(0) }) {
                Text(stringResource(R.string.speed_pitch_reset))
            }
        }
    }
}

/** Opens the system folder picker and remembers the chosen lyrics folder. */
@Composable
fun rememberLyricsFolderPicker(vm: MainViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.settings.setLyricsFolder(uri.toString())
        }
    }
    return { launcher.launch(null) }
}

@Composable
fun MoreSheetContent(vm: MainViewModel) {
    val skipSilence by vm.settings.skipSilence.collectAsState()
    val visualizer by vm.settings.visualizer.collectAsState()
    val headphones by vm.settings.headphonesAutoplay.collectAsState()
    val lyricsFolder by vm.settings.lyricsFolder.collectAsState()
    val pickFolder = rememberLyricsFolderPicker(vm)

    Column(Modifier.padding(bottom = 32.dp)) {
        Text(
            stringResource(R.string.player_settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        ToggleRow(stringResource(R.string.settings_skip_silence), stringResource(R.string.settings_skip_silence_hint), skipSilence) {
            vm.settings.setSkipSilence(it)
        }
        ToggleRow(stringResource(R.string.settings_visualizer), stringResource(R.string.settings_visualizer_hint), visualizer) {
            vm.settings.setVisualizer(it)
        }
        if (visualizer) VisualizerStyleChips(vm, Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        ToggleRow(stringResource(R.string.settings_headphones), stringResource(R.string.settings_headphones_hint), headphones) {
            vm.settings.setHeadphonesAutoplay(it)
        }
        ListItem(
            modifier = Modifier.clickable(onClick = pickFolder),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(stringResource(R.string.settings_lyrics_folder)) },
            supportingContent = {
                Text(stringResource(if (lyricsFolder == null) R.string.lyrics_folder_none else R.string.lyrics_folder_set))
            },
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
fun LyricsSheetContent(vm: MainViewModel, onEdit: (Long) -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val pickFolder = rememberLyricsFolderPicker(vm)
    val trackId = playerState.current?.trackId ?: -1L

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(horizontal = 24.dp)) {
        when (val state = lyrics) {
            LyricsState.NoFolder -> Message(stringResource(R.string.lyrics_no_folder), stringResource(R.string.lyrics_choose_folder), pickFolder) {
                onEdit(trackId)
            }
            LyricsState.NotFound -> Message(stringResource(R.string.lyrics_not_found), stringResource(R.string.lyrics_change_folder), pickFolder) {
                onEdit(trackId)
            }
            is LyricsState.Found -> {
                val position by vm.player.position.collectAsState()
                val lines = state.lines
                val active = Lrc.activeIndex(lines, position)
                val listState = rememberLazyListState()
                LaunchedEffect(active) {
                    if (active >= 0) listState.animateScrollToItem(max(0, active - 2))
                }
                TextButton(onClick = { onEdit(trackId) }) { Text(stringResource(R.string.lyrics_edit)) }
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(lines) { index, line ->
                        val synced = line.timeMs >= 0
                        Text(
                            line.text,
                            style = MaterialTheme.typography.titleLarge,
                            color = when {
                                !synced -> MaterialTheme.colorScheme.onSurface
                                index == active -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (synced) Modifier.clickable { vm.player.seekTo(line.timeMs) } else Modifier)
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String, buttonLabel: String, onClick: () -> Unit, onWrite: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onClick) { Text(buttonLabel) }
        OutlinedButton(onClick = onWrite) { Text(stringResource(R.string.lyrics_write)) }
    }
}

/** Ring, bars or wave. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualizerStyleChips(vm: MainViewModel, modifier: Modifier = Modifier) {
    val style by vm.settings.visualizerStyle.collectAsState()
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.vis_ring, R.string.vis_bars, R.string.vis_wave).forEachIndexed { index, label ->
            FilterChip(selected = style == index, onClick = { vm.settings.setVisualizerStyle(index) }, label = { Text(stringResource(label)) })
        }
    }
}
