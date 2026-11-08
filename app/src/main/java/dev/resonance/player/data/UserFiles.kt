package dev.resonance.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Things the user added inside the app: a background photo, song covers and hand-written lyrics.
 * They live in the app's private storage, so no permission is needed and the music files stay untouched.
 */
class UserFiles(context: Context) {
    private val ctx = context.applicationContext
    private val root: File get() = ctx.filesDir

    private val backgroundFile: File get() = File(root, "background.jpg")
    private fun coverFile(trackId: Long) = File(File(root, "covers"), "$trackId.jpg")
    private fun lyricsFile(trackId: Long) = File(File(root, "lyrics"), "$trackId.txt")

    private val _coverVersion = MutableStateFlow(0)
    /** Bumped whenever any custom cover changes, so covers on screen reload. */
    val coverVersion: StateFlow<Int> = _coverVersion.asStateFlow()

    private val _lyricsVersion = MutableStateFlow(0)
    val lyricsVersion: StateFlow<Int> = _lyricsVersion.asStateFlow()

    private val _backgroundVersion = MutableStateFlow(if (backgroundFile.isFile) backgroundFile.lastModified() else 0L)
    /** 0 when there is no background photo, otherwise a value that changes with every new photo. */
    val backgroundVersion: StateFlow<Long> = _backgroundVersion.asStateFlow()

    // ---- Background ------------------------------------------------------------------------

    suspend fun saveBackground(uri: Uri): Boolean {
        val ok = saveImage(uri, backgroundFile, maxSide = 1920, square = false)
        if (ok) _backgroundVersion.value = System.currentTimeMillis()
        return ok
    }

    fun removeBackground() {
        backgroundFile.delete()
        _backgroundVersion.value = 0L
    }

    suspend fun loadBackground(): Bitmap? = withContext(Dispatchers.IO) {
        if (!backgroundFile.isFile) null else runCatching { BitmapFactory.decodeFile(backgroundFile.path) }.getOrNull()
    }

    // ---- Covers ----------------------------------------------------------------------------

    fun customCover(trackId: Long): File? = coverFile(trackId).takeIf { it.isFile }

    suspend fun saveCover(trackId: Long, uri: Uri): Boolean {
        val ok = saveImage(uri, coverFile(trackId), maxSide = 768, square = true)
        if (ok) _coverVersion.value++
        return ok
    }

    fun removeCover(trackId: Long) {
        if (coverFile(trackId).delete()) _coverVersion.value++
    }

    // ---- Lyrics ----------------------------------------------------------------------------

    fun ownLyrics(trackId: Long): String? =
        lyricsFile(trackId).takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }?.takeIf { it.isNotBlank() }

    fun saveLyrics(trackId: Long, text: String) {
        val file = lyricsFile(trackId)
        if (text.isBlank()) file.delete() else {
            file.parentFile?.mkdirs()
            file.writeText(text)
        }
        _lyricsVersion.value++
    }

    // ---- Images ----------------------------------------------------------------------------

    /** Decodes a picked image at a sane size, fixes its rotation and writes it as JPEG. */
    private suspend fun saveImage(uri: Uri, out: File, maxSide: Int, square: Boolean): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching false

            val degrees = resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            if (degrees != 0f) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
            }
            if (square) {
                val side = minOf(bitmap.width, bitmap.height)
                bitmap = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
                if (side > maxSide) bitmap = Bitmap.createScaledBitmap(bitmap, maxSide, maxSide, true)
            } else {
                val longest = maxOf(bitmap.width, bitmap.height)
                if (longest > maxSide) {
                    val scale = maxSide.toFloat() / longest
                    bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                }
            }
            out.parentFile?.mkdirs()
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }.getOrDefault(false)
    }
}
