package dev.resonance.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.glance.appwidget.updateAll
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.resonance.player.MainActivity
import dev.resonance.player.ResonanceApplication
import dev.resonance.player.ui.widget.NowPlayingWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Owns the ExoPlayer and its media session, so music keeps playing with the app closed. */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var tracker: PlayTracker? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var artwork: ArtworkLoader
    private var deviceCallback: AudioDeviceCallback? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as ResonanceApplication
        artwork = ArtworkLoader(this, (application as dev.resonance.player.ResonanceApplication).userFiles)

        // The spectrum processor sits in the audio path and feeds the visualizer.
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(SpectrumProcessor()))
                    .build()
        }

        val player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                app.effects.attach(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refreshWidget(player)

            override fun onIsPlayingChanged(isPlaying: Boolean) = refreshWidget(player)

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // "Stop after this track" has done its job.
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) app.sleepTimer.cancel()
            }
        })
        app.effects.attach(player.audioSessionId)
        NowPlayingSnapshot.player = player

        tracker = PlayTracker(player, app.stats, scope).also { it.start() }

        // Settings that change how the player behaves.
        mainScope.launch {
            combine(app.settings.speed, app.settings.pitchSemitones) { speed, semitones -> speed to semitones }
                .collect { (speed, semitones) ->
                    // Twelve semitones make an octave, which doubles the frequency.
                    player.playbackParameters = PlaybackParameters(speed, Math.pow(2.0, semitones / 12.0).toFloat())
                }
        }
        mainScope.launch { app.settings.skipSilence.collect { player.skipSilenceEnabled = it } }
        mainScope.launch {
            app.sleepTimer.state.collectLatest { state ->
                player.pauseAtEndOfMediaItems = state.endOfTrack
                if (state.endAtElapsed > 0L) {
                    delay((state.endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                    player.pause()
                    app.sleepTimer.cancel()
                }
            }
        }

        // Optionally start playing when headphones or a Bluetooth speaker connect.
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val handler = Handler(Looper.getMainLooper())
        var ready = false
        handler.postDelayed({ ready = true }, 1500) // the system reports already-connected devices at first
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (!ready || !app.settings.headphonesAutoplay.value) return
                val headphones = addedDevices.any { it.isSink && it.type in HEADPHONE_TYPES }
                if (headphones && player.mediaItemCount > 0 && !player.isPlaying) player.play()
            }
        }
        audioManager.registerAudioDeviceCallback(callback, handler)
        deviceCallback = callback

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    private fun refreshWidget(player: Player) {
        val entry = player.currentMediaItem?.toQueueEntry()
        mainScope.launch {
            NowPlayingSnapshot.title = entry?.title.orEmpty()
            NowPlayingSnapshot.artist = entry?.artist.orEmpty()
            NowPlayingSnapshot.isPlaying = player.isPlaying
            NowPlayingSnapshot.cover = entry?.let { artwork.load(it.trackId, it.albumId, WIDGET_COVER_PX) }
            runCatching { NowPlayingWidget().updateAll(this@PlaybackService) }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away stops playback only if nothing is playing.
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        tracker?.stop()
        deviceCallback?.let { (getSystemService(Context.AUDIO_SERVICE) as AudioManager).unregisterAudioDeviceCallback(it) }
        NowPlayingSnapshot.player = null
        NowPlayingSnapshot.isPlaying = false
        (application as ResonanceApplication).effects.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val WIDGET_COVER_PX = 256
        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}
