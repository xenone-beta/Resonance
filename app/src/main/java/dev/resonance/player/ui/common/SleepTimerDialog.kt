package dev.resonance.player.ui.common

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.resonance.player.R
import dev.resonance.player.core.TimeFormat
import dev.resonance.player.playback.SleepTimer
import kotlinx.coroutines.delay

/** Time left on the sleep timer, ticking every second; null when no countdown runs. */
@Composable
fun rememberSleepRemaining(timer: SleepTimer): Long? {
    val state by timer.state.collectAsState()
    val remaining by produceState<Long?>(null, state) {
        if (state.endAtElapsed <= 0L) {
            value = null
            return@produceState
        }
        while (true) {
            val left = state.endAtElapsed - SystemClock.elapsedRealtime()
            value = left.coerceAtLeast(0L)
            if (left <= 0L) break
            delay(1000)
        }
    }
    return remaining
}

@Composable
fun SleepTimerDialog(timer: SleepTimer, onDismiss: () -> Unit) {
    val state by timer.state.collectAsState()
    val remaining = rememberSleepRemaining(timer)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_title)) },
        text = {
            Column {
                if (remaining != null) {
                    Text(
                        stringResource(R.string.sleep_left, TimeFormat.clock(remaining)),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else if (state.endOfTrack) {
                    Text(
                        stringResource(R.string.sleep_after_track_active),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                listOf(15, 30, 45, 60).forEach { minutes ->
                    ListItem(
                        modifier = Modifier.clickable { timer.startMinutes(minutes); onDismiss() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.sleep_minutes, minutes)) },
                    )
                }
                ListItem(
                    modifier = Modifier.clickable { timer.startEndOfTrack(); onDismiss() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.sleep_after_track)) },
                )
            }
        },
        confirmButton = {
            if (state.active) {
                TextButton(onClick = { timer.cancel(); onDismiss() }) { Text(stringResource(R.string.sleep_cancel)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
