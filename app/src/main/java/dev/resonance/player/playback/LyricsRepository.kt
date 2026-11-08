package dev.resonance.player.playback

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import dev.resonance.player.core.Lrc
import dev.resonance.player.core.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Finds .lrc lyric files in a folder the user picked (through the system file picker, which is the
 * only way to read non-audio files on modern Android) and parses them.
 */
class LyricsRepository(private val context: Context) {
    private val mutex = Mutex()
    private var indexedTree: String? = null
    private var indexedAt = 0L
    private var index: Map<String, Uri> = emptyMap()

    suspend fun find(treeUri: String, track: QueueEntry): List<LyricLine>? = withContext(Dispatchers.IO) {
        val files = indexFor(treeUri)
        val base = track.path.substringAfterLast('/').substringBeforeLast('.', "").lowercase()
        val uri = files[base]
            ?: files["${track.artist} - ${track.title}".lowercase()]
            ?: files[track.title.lowercase()]
            ?: return@withContext null
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()?.let { Lrc.parse(it) }?.takeIf { it.isNotEmpty() }
    }

    /** The folder is scanned once, then again when it changes or the scan is over a minute old. */
    private suspend fun indexFor(treeUri: String): Map<String, Uri> = mutex.withLock {
        val fresh = SystemClock.elapsedRealtime() - indexedAt < 60_000L
        if (indexedTree == treeUri && fresh) return index
        index = runCatching { scan(Uri.parse(treeUri)) }.getOrDefault(emptyMap())
        indexedTree = treeUri
        indexedAt = SystemClock.elapsedRealtime()
        index
    }

    private fun scan(tree: Uri): Map<String, Uri> {
        val found = HashMap<String, Uri>()
        val pending = ArrayDeque<String>()
        pending.add(DocumentsContract.getTreeDocumentId(tree))
        var visited = 0
        while (pending.isNotEmpty() && visited < MAX_ENTRIES) {
            val parent = pending.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
            context.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    visited++
                    val id = cursor.getString(0)
                    val name = cursor.getString(1).orEmpty()
                    if (cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.add(id)
                    } else if (name.endsWith(".lrc", ignoreCase = true)) {
                        found.putIfAbsent(
                            name.substringBeforeLast('.').lowercase(),
                            DocumentsContract.buildDocumentUriUsingTree(tree, id),
                        )
                    }
                }
            }
        }
        return found
    }

    private companion object {
        const val MAX_ENTRIES = 30_000
    }
}
