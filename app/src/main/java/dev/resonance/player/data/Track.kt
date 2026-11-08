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
    /** Directory that holds the file, or empty when unknown. */
    val folder: String = "",
    /** Full file path, used to find a lyrics file with the same name. */
    val path: String = "",
    /** Composer tag, empty when unknown. */
    val composer: String = "",
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

data class Folder(
    val path: String,
    val name: String,
    val tracks: List<Track>,
)
