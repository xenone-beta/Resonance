package dev.resonance.player.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenAndTimeTest {

    @Test
    fun accumulatorCountsOnlyPlayingTime() {
        val acc = ListenAccumulator()
        acc.reset(isPlaying = true, nowMs = 1_000)
        acc.onPlayingChanged(false, nowMs = 11_000) // 10 s played
        acc.onPlayingChanged(false, nowMs = 50_000) // repeated pause does nothing
        acc.onPlayingChanged(true, nowMs = 100_000)
        assertEquals(15_000L, acc.listenedMs(nowMs = 105_000)) // 10 s + 5 s still running
    }

    @Test
    fun resetStartsOver() {
        val acc = ListenAccumulator()
        acc.reset(true, 0)
        acc.onPlayingChanged(false, 20_000)
        acc.reset(false, 30_000)
        assertEquals(0L, acc.listenedMs(40_000))
    }

    @Test
    fun playThresholdIs30SecondsOrHalfOfAShortTrack() {
        assertFalse(ListenAccumulator.countsAsPlay(29_999, 200_000))
        assertTrue(ListenAccumulator.countsAsPlay(30_000, 200_000))
        // A 20 s jingle counts after 10 s.
        assertFalse(ListenAccumulator.countsAsPlay(9_000, 20_000))
        assertTrue(ListenAccumulator.countsAsPlay(10_000, 20_000))
        // Unknown duration falls back to 30 s.
        assertFalse(ListenAccumulator.countsAsPlay(29_000, 0))
        assertTrue(ListenAccumulator.countsAsPlay(30_000, 0))
    }

    @Test
    fun formatsClockTime() {
        assertEquals("0:00", TimeFormat.clock(0))
        assertEquals("3:07", TimeFormat.clock(187_999))
        assertEquals("1:03:07", TimeFormat.clock(3_787_000))
        assertEquals("0:00", TimeFormat.clock(-5_000))
    }

    @Test
    fun splitsHoursAndMinutes() {
        assertEquals(0L to 0L, TimeFormat.hoursAndMinutes(30_000))
        assertEquals(1L to 5L, TimeFormat.hoursAndMinutes(65 * 60_000L))
        assertEquals(25L to 59L, TimeFormat.hoursAndMinutes((25 * 60 + 59) * 60_000L + 59_000))
    }
}
