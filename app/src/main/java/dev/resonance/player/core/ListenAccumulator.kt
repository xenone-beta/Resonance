package dev.resonance.player.core

/**
 * Adds up how long a track was really playing (pauses excluded) and decides whether
 * that listening counts as a "play" for the statistics.
 */
class ListenAccumulator {
    private var playingSince = NOT_PLAYING
    private var accumulatedMs = 0L

    /** Starts counting for a new track. */
    fun reset(isPlaying: Boolean, nowMs: Long) {
        accumulatedMs = 0L
        playingSince = if (isPlaying) nowMs else NOT_PLAYING
    }

    fun onPlayingChanged(isPlaying: Boolean, nowMs: Long) {
        if (isPlaying && playingSince == NOT_PLAYING) {
            playingSince = nowMs
        } else if (!isPlaying && playingSince != NOT_PLAYING) {
            accumulatedMs += (nowMs - playingSince).coerceAtLeast(0L)
            playingSince = NOT_PLAYING
        }
    }

    /** Milliseconds of actual playback so far. */
    fun listenedMs(nowMs: Long): Long {
        val running = if (playingSince != NOT_PLAYING) (nowMs - playingSince).coerceAtLeast(0L) else 0L
        return accumulatedMs + running
    }

    companion object {
        private const val NOT_PLAYING = -1L
        private const val MIN_LISTEN_MS = 30_000L

        /** A play counts after 30 seconds, or after half of a track that is shorter than a minute. */
        fun countsAsPlay(listenedMs: Long, durationMs: Long): Boolean {
            val threshold = if (durationMs in 1 until MIN_LISTEN_MS * 2) durationMs / 2 else MIN_LISTEN_MS
            return listenedMs >= threshold
        }
    }
}
