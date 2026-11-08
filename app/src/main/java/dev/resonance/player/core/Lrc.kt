package dev.resonance.player.core

/** One lyric line. [timeMs] is -1 for plain, unsynchronised text. */
data class LyricLine(val timeMs: Long, val text: String)

/** Parser for LRC lyric files: "[mm:ss.xx] text", several timestamps per line, and plain text. */
object Lrc {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val tag = Regex("""^\[[a-zA-Z]+:.*]$""")

    fun parse(raw: String): List<LyricLine> {
        val synced = ArrayList<LyricLine>()
        val plain = ArrayList<LyricLine>()
        for (rawLine in raw.removePrefix("﻿").lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || tag.matches(line)) continue
            val stamps = timestamp.findAll(line).toList()
            if (stamps.isEmpty()) {
                plain.add(LyricLine(-1L, line))
                continue
            }
            val text = line.substring(stamps.last().range.last + 1).trim()
            for (stamp in stamps) {
                val minutes = stamp.groupValues[1].toLong()
                val seconds = stamp.groupValues[2].toLong()
                val fraction = stamp.groupValues[3]
                // ".5" is half a second, ".05" is 50 ms, ".500" is 500 ms.
                val millis = if (fraction.isEmpty()) 0L else (("0.$fraction").toDouble() * 1000).toLong()
                synced.add(LyricLine(minutes * 60_000 + seconds * 1000 + millis, text))
            }
        }
        return if (synced.isNotEmpty()) synced.sortedBy { it.timeMs } else plain
    }

    /** Index of the line being sung at [positionMs], or -1 before the first line or for plain text. */
    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty() || lines.first().timeMs < 0) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
