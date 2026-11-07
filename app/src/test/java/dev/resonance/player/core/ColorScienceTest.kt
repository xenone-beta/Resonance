package dev.resonance.player.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorScienceTest {

    private fun channel(argb: Int, shift: Int) = (argb shr shift) and 0xFF

    @Test
    fun extremeTonesAreBlackAndWhite() {
        assertEquals(0xFF000000.toInt(), ColorScience.lchToArgb(0.0, 50.0, 120.0))
        assertEquals(0xFFFFFFFF.toInt(), ColorScience.lchToArgb(100.0, 50.0, 120.0))
    }

    @Test
    fun roundTripKeepsInGamutColors() {
        for (color in listOf(0xFF6750A4.toInt(), 0xFF336699.toInt(), 0xFF808080.toInt(), 0xFFC0392B.toInt())) {
            val lch = ColorScience.argbToLch(color)
            val back = ColorScience.lchToArgb(lch.tone, lch.chroma, lch.hue)
            for (shift in listOf(16, 8, 0)) {
                assertTrue(
                    "channel at shift $shift of ${Integer.toHexString(color)} drifted",
                    abs(channel(color, shift) - channel(back, shift)) <= 2,
                )
            }
        }
    }

    @Test
    fun clippingChromaKeepsTheTone() {
        // Chroma 120 at tone 90 is far outside sRGB; the result must still have tone 90.
        val clipped = ColorScience.lchToArgb(90.0, 120.0, 100.0)
        assertEquals(90.0, ColorScience.argbToLch(clipped).tone, 1.0)
    }

    @Test
    fun toneOrderingMatchesLuminance() {
        val dark = ColorScience.lchToArgb(30.0, 40.0, 250.0)
        val light = ColorScience.lchToArgb(80.0, 40.0, 250.0)
        assertTrue(ColorScience.luminance(light) > ColorScience.luminance(dark))
    }

    @Test
    fun contrastOfBlackAndWhiteIs21() {
        assertEquals(21.0, ColorScience.contrast(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, ColorScience.contrast(0xFF123456.toInt(), 0xFF123456.toInt()), 0.0001)
    }
}
