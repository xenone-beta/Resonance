package dev.resonance.player.core

/** Counts consecutive listening days. Days are epoch days (days since 1970-01-01, local time). */
object Streaks {
    /**
     * The streak that is still alive on [today]: it may end today or yesterday
     * (you have until midnight to keep it going).
     */
    fun current(days: Set<Long>, today: Long): Int {
        var day = when {
            today in days -> today
            today - 1 in days -> today - 1
            else -> return 0
        }
        var count = 0
        while (day in days) {
            count++
            day--
        }
        return count
    }

    fun longest(days: Set<Long>): Int {
        var best = 0
        for (day in days) {
            if (day - 1 in days) continue // not the start of a run
            var length = 0
            var cursor = day
            while (cursor in days) {
                length++
                cursor++
            }
            if (length > best) best = length
        }
        return best
    }
}
