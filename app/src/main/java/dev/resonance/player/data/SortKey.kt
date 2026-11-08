package dev.resonance.player.data

/** How the Songs tab is ordered. */
enum class SortKey { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, COMPOSER;

    /** Dates and lengths read best biggest-first; names read best A to Z. */
    val defaultDescending: Boolean get() = this == DATE_ADDED || this == DURATION

    companion object {
        fun fromOrdinal(value: Int): SortKey = entries.getOrElse(value) { TITLE }
    }
}
