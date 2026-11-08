package dev.resonance.player.core

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumTest {
    private val rate = 44_100
    private val bands = 32

    private fun tone(hz: Double, amplitude: Double) =
        FloatArray(Spectrum.WINDOW) { (amplitude * sin(2.0 * PI * hz * it / rate)).toFloat() }

    @Test
    fun silenceIsFlat() {
        val result = Spectrum.bands(FloatArray(Spectrum.WINDOW), rate, bands)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun toneLightsTheBandThatContainsIt() {
        val edges = Spectrum.bandEdges(bands, rate)
        for (hz in listOf(200.0, 440.0, 1000.0, 5000.0)) {
            val result = Spectrum.bands(tone(hz, 0.8), rate, bands)
            val loudest = result.indices.maxByOrNull { result[it] }!!
            assertTrue("$hz Hz landed in band $loudest", hz >= edges[loudest] * 0.8 && hz <= edges[loudest + 1] * 1.25)
            assertTrue(result[loudest] > 0.8f)
        }
    }

    @Test
    fun quieterToneGivesLowerLevel() {
        val loud = Spectrum.bands(tone(1000.0, 0.8), rate, bands).max()
        val quiet = Spectrum.bands(tone(1000.0, 0.08), rate, bands).max()
        assertTrue(quiet < loud)
        assertEquals(0.33f, loud - quiet, 0.1f)
    }

    @Test
    fun edgesGrowAndStayBelowNyquist() {
        val edges = Spectrum.bandEdges(bands, rate)
        assertEquals(bands + 1, edges.size)
        for (i in 1 until edges.size) assertTrue(edges[i] > edges[i - 1])
        assertTrue(edges.last() < rate / 2.0)
    }
}
