package dev.resonance.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.resonance.player.core.ListenAccumulator
import dev.resonance.player.data.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Watches a player and writes listening statistics: how long each track was really playing,
 * and whether that counts as a play. Runs inside the playback service.
 */
class PlayTracker(
    private val player: Player,
    private val stats: StatsRepository,
    private val scope: CoroutineScope,
) : Player.Listener {

    private val accumulator = ListenAccumulator()
    private var current: QueueEntry? = null

    fun start() {
        player.addListener(this)
        begin(player.currentMediaItem)
    }

    fun stop() {
        player.removeListener(this)
        flush()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        flush()
        begin(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        accumulator.onPlayingChanged(isPlaying, now())
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        // End of the whole queue: nothing will transition, so record the last track now.
        if (playbackState == Player.STATE_ENDED) {
            flush()
            begin(player.currentMediaItem)
        }
    }

    private fun begin(item: MediaItem?) {
        current = item?.toQueueEntry()?.takeIf { it.trackId >= 0 }
        accumulator.reset(player.isPlaying, now())
    }

    private fun flush() {
        val entry = current ?: return
        val listened = accumulator.listenedMs(now())
        accumulator.reset(false, now())
        if (listened <= 0) return
        val counted = ListenAccumulator.countsAsPlay(listened, entry.durationMs)
        scope.launch {
            stats.record(entry.trackId, entry.title, entry.artist, entry.album, listened, counted)
        }
    }

    private fun now() = System.currentTimeMillis()
}
