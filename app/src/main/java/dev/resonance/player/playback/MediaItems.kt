package dev.resonance.player.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.resonance.player.data.Track

internal const val EXTRA_ALBUM_ID = "albumId"
internal const val EXTRA_DURATION = "durationMs"

fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

fun Track.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_ALBUM_ID, albumId)
        putLong(EXTRA_DURATION, durationMs)
    }
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist.ifEmpty { null })
                .setAlbumTitle(album.ifEmpty { null })
                .setArtworkUri(albumArtUri(albumId))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(extras)
                .build(),
        )
        .build()
}

/** A song in the play queue, rebuilt from the MediaItem the player holds. */
data class QueueEntry(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
)

fun MediaItem.toQueueEntry(): QueueEntry {
    val extras = mediaMetadata.extras
    return QueueEntry(
        trackId = mediaId.toLongOrNull() ?: -1L,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        album = mediaMetadata.albumTitle?.toString().orEmpty(),
        albumId = extras?.getLong(EXTRA_ALBUM_ID, -1L) ?: -1L,
        durationMs = extras?.getLong(EXTRA_DURATION, 0L) ?: 0L,
    )
}
