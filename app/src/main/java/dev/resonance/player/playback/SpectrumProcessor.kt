package dev.resonance.player.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import dev.resonance.player.core.Spectrum
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest loudness bands, shared between the audio thread and the player screen. */
object SpectrumBus {
    const val BANDS = 32

    /** The analysis only runs while the visualizer is on screen. */
    @Volatile
    var active: Boolean = false

    private val _bands = MutableStateFlow(FloatArray(BANDS))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    fun publish(values: FloatArray) {
        _bands.value = values
    }
}

/**
 * Passes audio through untouched and, while the visualizer is visible, measures its spectrum.
 * Works on the player's own PCM stream, so no recording permission is needed.
 */
@UnstableApi
class SpectrumProcessor : BaseAudioProcessor() {
    private val window = FloatArray(Spectrum.WINDOW)
    private var filled = 0
    private var channels = 2
    private var sampleRate = 44_100

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Only 16-bit PCM is analysed; anything else is left alone (processor stays inactive).
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        sampleRate = inputAudioFormat.sampleRate
        filled = 0
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        if (SpectrumBus.active) analyze(inputBuffer) else filled = 0

        val output = replaceOutputBuffer(size)
        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush() {
        filled = 0
    }

    private fun analyze(buffer: ByteBuffer) {
        val view = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frames = view.remaining() / (2 * channels)
        var offset = view.position()
        repeat(frames) {
            var sum = 0
            for (channel in 0 until channels) {
                sum += view.getShort(offset).toInt()
                offset += 2
            }
            window[filled++] = sum / (channels * 32768f)
            if (filled == Spectrum.WINDOW) {
                SpectrumBus.publish(Spectrum.bands(window, sampleRate, SpectrumBus.BANDS))
                // Overlap windows by half so the picture updates about 40 times a second.
                val half = Spectrum.WINDOW / 2
                System.arraycopy(window, half, window, 0, half)
                filled = half
            }
        }
    }
}
