package dev.resonance.player.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.core.TimeFormat
import dev.resonance.player.ui.theme.AlbumTheme

/** Full-screen player. Its whole color scheme comes from the current album cover. */
@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit) {
    val state by vm.player.state.collectAsState()
    val seed by vm.seed.collectAsState()
    val cover by vm.cover.collectAsState()
    val entry = state.current

    // Nothing left to show (the queue was emptied): leave the screen.
    LaunchedEffect(entry == null) { if (entry == null) onBack() }
    if (entry == null) return

    AlbumTheme(seed) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.queue_title))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val art = cover
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    entry.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    entry.artist.ifEmpty { stringResource(R.string.unknown_artist) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                SeekBar(vm, entry.durationMs)

                Spacer(Modifier.weight(1f))
                Controls(vm, state.isPlaying, state.shuffle, state.repeatMode)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SeekBar(vm: MainViewModel, durationMs: Long) {
    val position by vm.player.position.collectAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val max = durationMs.coerceAtLeast(1L).toFloat()
    val shown = if (dragging) dragValue else position.toFloat().coerceIn(0f, max)

    Column {
        Slider(
            value = shown,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = { vm.player.seekTo(dragValue.toLong()); dragging = false },
            valueRange = 0f..max,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                TimeFormat.clock(shown.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                TimeFormat.clock(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Controls(vm: MainViewModel, isPlaying: Boolean, shuffle: Boolean, repeatMode: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(checked = shuffle, onCheckedChange = { vm.player.toggleShuffle() }) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { vm.player.previous() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), Modifier.size(36.dp))
        }
        FilledIconButton(
            onClick = { vm.player.togglePlay() },
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(if (isPlaying) 28.dp else 40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = { vm.player.next() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.next), Modifier.size(36.dp))
        }
        IconButton(onClick = { vm.player.cycleRepeat() }) {
            Icon(
                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.repeat),
                tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}
