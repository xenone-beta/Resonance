package dev.resonance.player.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Turns a window of audio samples into a handful of loudness bands (0..1) for the visualizer. */
object Spectrum {
    const val WINDOW = 2048
    private const val MIN_HZ = 50.0
    private const val MAX_HZ = 16_000.0
    private const val FLOOR_DB = 60.0

    private val hann = FloatArray(WINDOW) { (0.5 - 0.5 * cos(2.0 * PI * it / (WINDOW - 1))).toFloat() }

    /** Lower edge of each band plus the upper edge of the last one, in Hz (log-spaced). */
    fun bandEdges(bandCount: Int, sampleRate: Int): DoubleArray {
        val top = min(MAX_HZ, sampleRate / 2.0 * 0.9)
        return DoubleArray(bandCount + 1) { MIN_HZ * Math.pow(top / MIN_HZ, it.toDouble() / bandCount) }
    }

    /**
     * [samples] holds [WINDOW] mono samples in -1..1. Returns [bandCount] levels in 0..1 on a
     * decibel scale: a full-scale tone is about 1, silence is 0.
     */
    fun bands(samples: FloatArray, sampleRate: Int, bandCount: Int): FloatArray {
        require(samples.size == WINDOW) { "Expected $WINDOW samples" }
        val re = FloatArray(WINDOW) { samples[it] * hann[it] }
        val im = FloatArray(WINDOW)
        fft(re, im)

        val binHz = sampleRate.toDouble() / WINDOW
        val edges = bandEdges(bandCount, sampleRate)
        val half = WINDOW / 2
        // A sine of amplitude 1 through a Hann window peaks at WINDOW / 4.
        val scale = 4.0 / WINDOW

        return FloatArray(bandCount) { band ->
            val lo = max(1, (edges[band] / binHz).toInt())
            val hi = min(half - 1, max(lo + 1, Math.ceil(edges[band + 1] / binHz).toInt()))
            var peak = 0.0
            for (bin in lo..hi) {
                val magnitude = sqrt((re[bin] * re[bin] + im[bin] * im[bin]).toDouble()) * scale
                if (magnitude > peak) peak = magnitude
            }
            val db = 20.0 * log10(peak + 1e-9)
            ((db + FLOOR_DB) / FLOOR_DB).coerceIn(0.0, 1.0).toFloat()
        }
    }

    /** In-place iterative radix-2 FFT. Both arrays must have a power-of-two length. */
    internal fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wr = cos(angle)
            val wi = sin(angle)
            var start = 0
            while (start < n) {
                var cr = 1.0
                var ci = 0.0
                for (k in 0 until len / 2) {
                    val a = start + k
                    val b = a + len / 2
                    val xr = (re[b] * cr - im[b] * ci).toFloat()
                    val xi = (re[b] * ci + im[b] * cr).toFloat()
                    re[b] = re[a] - xr
                    im[b] = im[a] - xi
                    re[a] += xr
                    im[a] += xi
                    val nr = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = nr
                }
                start += len
            }
            len = len shl 1
        }
    }
}
