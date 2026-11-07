package dev.resonance.player.core

import java.util.Locale

object TimeFormat {

    /** "3:07" or, for an hour and longer, "1:03:07". Negative values count as zero. */
    fun clock(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    /** Whole hours and the remaining minutes, for "12 h 5 min" style labels. */
    fun hoursAndMinutes(millis: Long): Pair<Long, Long> {
        val totalMinutes = millis.coerceAtLeast(0L) / 60_000L
        return totalMinutes / 60 to totalMinutes % 60
    }
}
