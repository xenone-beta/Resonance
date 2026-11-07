package dev.resonance.player.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

/** A song from the device's MediaStore. An empty [artist] or [album] means "unknown". */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val dateAdded: Long,
) {
    val uri: Uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val tracks: List<Track>,
)

data class Artist(
    val name: String,
    val tracks: List<Track>,
    val albumCount: Int,
)
