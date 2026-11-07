package dev.resonance.player.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumSchemeTest {

    private val seeds = listOf(
        0xFF6750A4.toInt(), // violet
        0xFFE91E63.toInt(), // pink
        0xFF2E7D32.toInt(), // green
        0xFFFFC107.toInt(), // amber
        0xFF1565C0.toInt(), // blue
        0xFF808080.toInt(), // gray
        0xFF000000.toInt(), // black
        0xFFFFFFFF.toInt(), // white
    )

    private fun assertContrast(name: String, fg: Int, bg: Int, minimum: Double) {
        val ratio = ColorScience.contrast(fg, bg)
        assertTrue("$name contrast $ratio is below $minimum", ratio >= minimum)
    }

    @Test
    fun textColorsAreReadableForEverySeedInBothThemes() {
        for (seed in seeds) {
            for (dark in listOf(false, true)) {
                val s = AlbumScheme.generate(seed, dark)
                assertContrast("onPrimary", s.onPrimary, s.primary, 4.5)
                assertContrast("onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5)
                assertContrast("onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer, 4.5)
                assertContrast("onTertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer, 4.5)
                assertContrast("onSurface", s.onSurface, s.surface, 7.0)
                assertContrast("onSurface on container", s.onSurface, s.surfaceContainerHigh, 7.0)
                assertContrast("onSurfaceVariant", s.onSurfaceVariant, s.surface, 4.5)
                assertContrast("primary on surface", s.primary, s.surface, 3.0)
            }
        }
    }

    @Test
    fun lightSchemeIsLightAndDarkSchemeIsDark() {
        val light = AlbumScheme.generate(0xFF6750A4.toInt(), dark = false)
        val dark = AlbumScheme.generate(0xFF6750A4.toInt(), dark = true)
        assertTrue(ColorScience.luminance(light.surface) > 0.8)
        assertTrue(ColorScience.luminance(dark.surface) < 0.02)
    }

    @Test
    fun differentSeedsGiveDifferentSchemes() {
        val a = AlbumScheme.generate(0xFFE91E63.toInt(), dark = true)
        val b = AlbumScheme.generate(0xFF1565C0.toInt(), dark = true)
        assertNotEquals(a.primary, b.primary)
        assertNotEquals(a.primaryContainer, b.primaryContainer)
    }

    @Test
    fun errorColorsAreFixed() {
        assertEquals(0xFFB3261E.toInt(), AlbumScheme.generate(0xFF2E7D32.toInt(), dark = false).error)
        assertEquals(0xFFF2B8B5.toInt(), AlbumScheme.generate(0xFF2E7D32.toInt(), dark = true).error)
    }
}
