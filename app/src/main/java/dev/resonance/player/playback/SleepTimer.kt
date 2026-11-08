package dev.resonance.player.playback

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [endAtElapsed] is on the SystemClock.elapsedRealtime() scale; 0 means no countdown. */
data class SleepState(val endAtElapsed: Long = 0L, val endOfTrack: Boolean = false) {
    val active: Boolean get() = endAtElapsed > 0L || endOfTrack
}

/** Set by the UI, acted on by the playback service (which pauses the music). */
class SleepTimer {
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    fun startMinutes(minutes: Int) {
        _state.value = SleepState(endAtElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L)
    }

    fun startEndOfTrack() {
        _state.value = SleepState(endOfTrack = true)
    }

    fun cancel() {
        _state.value = SleepState()
    }
}
