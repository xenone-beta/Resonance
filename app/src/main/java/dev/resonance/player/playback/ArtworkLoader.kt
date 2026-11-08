package dev.resonance.player.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.palette.graphics.Palette
import dev.resonance.player.data.Track
import dev.resonance.player.data.UserFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads album art from MediaStore (embedded or folder art) and picks a seed colour from it. */
class ArtworkLoader(private val context: Context, private val files: UserFiles) {

    /** Changes whenever a custom cover is added or removed. */
    val version get() = files.coverVersion

    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val missing = HashSet<String>()

    suspend fun load(trackId: Long, albumId: Long, sizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val custom = files.customCover(trackId)
        val key = "$trackId:$albumId:$sizePx:${custom?.lastModified() ?: 0L}"
        cache.get(key)?.let { return@withContext it }
        if (synchronized(missing) { key in missing }) return@withContext null

        val bitmap = decode(trackId, albumId, sizePx)
        if (bitmap != null) cache.put(key, bitmap) else synchronized(missing) { missing.add(key) }
        bitmap
    }

    suspend fun load(track: Track, sizePx: Int): Bitmap? = load(track.id, track.albumId, sizePx)

    private fun decode(trackId: Long, albumId: Long, sizePx: Int): Bitmap? {
        files.customCover(trackId)?.let { file ->
            runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= 29 && trackId >= 0) {
            val uri = android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                trackId,
            )
            runCatching { context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null) }
                .getOrNull()
                ?.let { return it }
        }
        if (albumId < 0) return null
        return runCatching {
            context.contentResolver.openInputStream(albumArtUri(albumId))?.use { stream ->
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                BitmapFactory.decodeStream(stream, null, options)
            }
        }.getOrNull()
    }

    /** The colour that best represents the cover, or null when the cover is missing or grey. */
    suspend fun seedColor(bitmap: Bitmap): Int? = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).maximumColorCount(24).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: return@withContext null
        swatch.rgb or (0xFF shl 24)
    }
}
