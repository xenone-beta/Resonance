package dev.resonance.player.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.ui.common.Cover
import dev.resonance.player.ui.theme.AlbumTheme

/** The compact player docked at the bottom of the library, tinted with the cover's colors. */
@Composable
fun MiniPlayer(vm: MainViewModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.player.state.collectAsState()
    val seed by vm.seed.collectAsState()
    val albumColors by vm.settings.albumColors.collectAsState()
    val entry = state.current ?: return

    AlbumTheme(if (albumColors) seed else null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 2.dp,
        ) {
            Column(modifier = Modifier.clickable(onClick = onOpen)) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(vm.artwork, entry.trackId, entry.albumId, 44.dp, corner = 12.dp)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            entry.artist.ifEmpty { stringResource(R.string.unknown_artist) },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { vm.player.togglePlay() }) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                        )
                    }
                    IconButton(onClick = { vm.player.next() }) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next))
                    }
                }
                MiniProgress(vm, entry.durationMs)
            }
        }
    }
}

@Composable
private fun MiniProgress(vm: MainViewModel, durationMs: Long) {
    val position by vm.player.position.collectAsState()
    val fraction = if (durationMs > 0) (position.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
    )
}
