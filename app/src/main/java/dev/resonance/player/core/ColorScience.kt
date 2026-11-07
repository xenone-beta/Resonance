package dev.resonance.player.core

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * CIELAB (D65) helpers. A Material 3 "tone" is the L* lightness of CIELAB, so building a color
 * from (tone, chroma, hue) here gives the same lightness steps as Material's tonal palettes,
 * and clipping to the sRGB gamut only reduces chroma, never the tone.
 */
object ColorScience {
    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883
    private const val EPSILON = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    class Lch(val tone: Double, val chroma: Double, val hue: Double)

    fun argbToLch(argb: Int): Lch {
        val r = srgbToLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = srgbToLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = srgbToLinear((argb and 0xFF) / 255.0)

        val x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b

        val fx = labF(x / XN)
        val fy = labF(y / YN)
        val fz = labF(z / ZN)

        val tone = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val bb = 200.0 * (fy - fz)
        val chroma = hypot(a, bb)
        var hue = Math.toDegrees(atan2(bb, a))
        if (hue < 0) hue += 360.0
        return Lch(tone, chroma, hue)
    }

    /** Opaque ARGB for the given tone (0..100), chroma and hue in degrees; chroma is reduced to fit sRGB. */
    fun lchToArgb(tone: Double, chroma: Double, hueDegrees: Double): Int {
        if (tone <= 0.0) return 0xFF000000.toInt()
        if (tone >= 100.0) return 0xFFFFFFFF.toInt()

        val hue = ((hueDegrees % 360.0) + 360.0) % 360.0
        var rgb = linearRgb(tone, chroma, hue)
        if (!inGamut(rgb)) {
            // Largest chroma that still fits in sRGB, found by bisection.
            var low = 0.0
            var high = chroma
            repeat(24) {
                val mid = (low + high) / 2.0
                if (inGamut(linearRgb(tone, mid, hue))) low = mid else high = mid
            }
            rgb = linearRgb(tone, low, hue)
        }
        return pack(rgb)
    }

    /** Relative luminance (WCAG) of an ARGB color. */
    fun luminance(argb: Int): Double {
        val r = srgbToLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = srgbToLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = srgbToLinear((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two colors, 1.0 to 21.0. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun linearRgb(tone: Double, chroma: Double, hue: Double): DoubleArray {
        val rad = Math.toRadians(hue)
        val a = chroma * cos(rad)
        val b = chroma * sin(rad)

        val fy = (tone + 16.0) / 116.0
        val fx = fy + a / 500.0
        val fz = fy - b / 200.0

        val xr = fx.pow(3).let { if (it > EPSILON) it else (116.0 * fx - 16.0) / KAPPA }
        val yr = if (tone > KAPPA * EPSILON) fy.pow(3) else tone / KAPPA
        val zr = fz.pow(3).let { if (it > EPSILON) it else (116.0 * fz - 16.0) / KAPPA }

        val x = xr * XN
        val y = yr * YN
        val z = zr * ZN

        return doubleArrayOf(
            3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
            -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
        )
    }

    private fun inGamut(rgb: DoubleArray): Boolean = rgb.all { it >= -0.0005 && it <= 1.0005 }

    private fun pack(rgb: DoubleArray): Int {
        val r = (linearToSrgb(rgb[0].coerceIn(0.0, 1.0)) * 255.0).roundToInt()
        val g = (linearToSrgb(rgb[1].coerceIn(0.0, 1.0)) * 255.0).roundToInt()
        val b = (linearToSrgb(rgb[2].coerceIn(0.0, 1.0)) * 255.0).roundToInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun labF(t: Double): Double = if (t > EPSILON) cbrt(t) else (KAPPA * t + 16.0) / 116.0

    private fun srgbToLinear(v: Double): Double =
        if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    private fun linearToSrgb(v: Double): Double =
        if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1.0 / 2.4) - 0.055
}
