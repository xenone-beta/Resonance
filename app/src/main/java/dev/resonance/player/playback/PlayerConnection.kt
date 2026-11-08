package dev.resonance.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.resonance.player.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val connected: Boolean = false,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    /** One of Player.REPEAT_MODE_OFF / ONE / ALL. */
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
) {
    val current: QueueEntry? get() = queue.getOrNull(currentIndex)
}

/** The UI's handle on the playback service, through a MediaController. Main thread only. */
class PlayerConnection(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val userFiles = (appContext as dev.resonance.player.ResonanceApplication).userFiles
    private fun Track.item(): MediaItem = toMediaItem(userFiles.customCover(id)?.let { android.net.Uri.fromFile(it) })
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    /** Playback position of the current track, refreshed while playing. */
    val position: StateFlow<Long> = _position.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = rebuild()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = rebuild()
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            rebuild()
            if (isPlaying) startPolling() else {
                positionJob?.cancel()
                syncPosition()
            }
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = rebuild()
        override fun onRepeatModeChanged(repeatMode: Int) = rebuild()
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = syncPosition()
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            rebuild()
            syncPosition()
            if (c.isPlaying) startPolling()
        }, androidx.core.content.ContextCompat.getMainExecutor(appContext))
    }

    fun disconnect() {
        positionJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = PlayerState()
    }

    // ---- Commands --------------------------------------------------------------------------

    /** Replaces the queue with [tracks] and starts at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        c.setMediaItems(tracks.map { it.item() }, startIndex.coerceIn(0, tracks.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    fun next() { controller?.seekToNextMediaItem() }

    /** Restarts the track after 3 seconds, otherwise goes to the previous one. */
    fun previous() { controller?.seekToPrevious() }

    /** Always goes to the previous song, never restarts the current one. */
    fun previousTrack() { controller?.seekToPreviousMediaItem() }

    /** Jumps [deltaMs] forward or back inside the current song. */
    fun seekBy(deltaMs: Long, durationMs: Long) {
        val target = (_position.value + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
        seekTo(target)
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _position.value = ms
    }

    fun skipTo(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun moveInQueue(from: Int, to: Int) {
        val c = controller ?: return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    /** Inserts right after the current song. With an empty queue it simply starts playing. */
    fun playNext(track: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(listOf(track))
        c.addMediaItem(c.currentMediaItemIndex + 1, track.item())
    }

    fun addToQueue(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        if (c.mediaItemCount == 0) return play(tracks)
        c.addMediaItems(tracks.map { it.item() })
    }

    // ---- State -----------------------------------------------------------------------------

    private fun rebuild() {
        val c = controller ?: return
        val previous = _state.value
        val queue = if (c.mediaItemCount == previous.queue.size && !queueChanged(c, previous)) {
            previous.queue
        } else {
            List(c.mediaItemCount) { c.getMediaItemAt(it).toQueueEntry() }
        }
        _state.value = PlayerState(
            connected = true,
            queue = queue,
            currentIndex = if (c.mediaItemCount == 0) -1 else c.currentMediaItemIndex,
            isPlaying = c.isPlaying,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
        )
    }

    /** Cheap check: same size, but did the items inside change (a move or a replace)? */
    private fun queueChanged(c: MediaController, previous: PlayerState): Boolean {
        for (i in previous.queue.indices) {
            if (c.getMediaItemAt(i).mediaId != previous.queue[i].trackId.toString()) return true
        }
        return false
    }

    private fun syncPosition() {
        _position.value = controller?.currentPosition ?: 0L
    }

    private fun startPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                syncPosition()
                delay(250)
            }
        }
    }
}
