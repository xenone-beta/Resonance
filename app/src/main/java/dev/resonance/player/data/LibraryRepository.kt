package dev.resonance.player.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** Reads the device's music from MediaStore. No files are copied or indexed by the app itself. */
class LibraryRepository(private val context: Context) {

    private val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun load(): List<Track> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        // Music only (no ringtones or recordings), and skip clips shorter than 10 seconds.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val result = ArrayList<Track>()
        context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val number = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val data = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                result.add(
                    Track(
                        id = cursor.getLong(id),
                        title = cursor.getString(title).orEmpty().ifBlank { "?" },
                        artist = cleanName(cursor.getString(artist)),
                        album = cleanName(cursor.getString(album)),
                        albumId = cursor.getLong(albumId),
                        durationMs = cursor.getLong(duration),
                        // MediaStore stores disc and track together: 2005 = disc 2, track 5.
                        trackNumber = cursor.getInt(number) % 1000,
                        dateAdded = cursor.getLong(added),
                        folder = cursor.getString(data).orEmpty().substringBeforeLast('/', ""),
                        path = cursor.getString(data).orEmpty(),
                    ),
                )
            }
        }
        result
    }

    /** Emits whenever the media library changes (a song was added, removed or retagged). */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(collection, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    private fun cleanName(raw: String?): String =
        if (raw.isNullOrBlank() || raw == MediaStore.UNKNOWN_STRING) "" else raw.trim()
}
