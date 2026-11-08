package dev.resonance.player.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcAndStreakTest {
    private val sample = """
        [ar:Someone]
        [ti:Song]
        [00:12.00] First line
        [00:20.50][01:05.5] Chorus
        [01:30] Last line
    """.trimIndent()

    @Test
    fun parsesTimestampsAndSkipsTags() {
        val lines = Lrc.parse(sample)
        assertEquals(listOf(12_000L, 20_500L, 65_500L, 90_000L), lines.map { it.timeMs })
        assertEquals(listOf("First line", "Chorus", "Chorus", "Last line"), lines.map { it.text })
    }

    @Test
    fun plainTextHasNoTimestamps() {
        val lines = Lrc.parse("Hello\n\nWorld")
        assertEquals(listOf("Hello", "World"), lines.map { it.text })
        assertEquals(-1, Lrc.activeIndex(lines, 5_000))
    }

    @Test
    fun findsTheActiveLine() {
        val lines = Lrc.parse(sample)
        assertEquals(-1, Lrc.activeIndex(lines, 5_000))
        assertEquals(0, Lrc.activeIndex(lines, 12_000))
        assertEquals(0, Lrc.activeIndex(lines, 20_499))
        assertEquals(1, Lrc.activeIndex(lines, 20_500))
        assertEquals(3, Lrc.activeIndex(lines, 500_000))
    }

    @Test
    fun streaks() {
        val days = setOf(10L, 11L, 12L, 20L, 21L, 22L, 23L)
        assertEquals(3, Streaks.longest(setOf(10L, 11L, 12L)))
        assertEquals(4, Streaks.longest(days))
        assertEquals(4, Streaks.current(days, 23))
        assertEquals(4, Streaks.current(days, 24)) // yesterday still counts
        assertEquals(0, Streaks.current(days, 26))
        assertEquals(0, Streaks.current(emptySet(), 5))
    }
}
