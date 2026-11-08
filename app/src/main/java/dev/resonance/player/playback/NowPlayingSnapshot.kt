package dev.resonance.player.playback

import android.graphics.Bitmap
import androidx.media3.common.Player

/** What the home screen widget shows, and the player its buttons talk to. Both live in the app process. */
object NowPlayingSnapshot {
    @Volatile var title: String = ""
    @Volatile var artist: String = ""
    @Volatile var isPlaying: Boolean = false
    @Volatile var cover: Bitmap? = null

    /** The service's player, or null while the service is not running. Use on the main thread only. */
    @Volatile var player: Player? = null
}
