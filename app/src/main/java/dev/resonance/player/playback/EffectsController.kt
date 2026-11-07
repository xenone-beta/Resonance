package dev.resonance.player.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the equalizer screen shows. Levels are in millibels (1 dB = 100 mB). */
data class EffectsState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bandFrequenciesHz: List<Int> = emptyList(),
    val levels: List<Int> = emptyList(),
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    val presets: List<String> = emptyList(),
    /** Index into [presets], or -1 for a custom curve. */
    val preset: Int = -1,
    val bassSupported: Boolean = false,
    val bass: Int = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizer: Int = 0,
)

/**
 * Owns the system equalizer, bass boost and virtualizer attached to the player's audio session,
 * and remembers the user's settings. Every effect is optional: devices differ in what they offer.
 */
class EffectsController(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("resonance_effects", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EffectsState())
    val state: StateFlow<EffectsState> = _state.asStateFlow()

    /** Called by the playback service whenever ExoPlayer reports an audio session id. */
    @Synchronized
    fun attach(audioSessionId: Int) {
        releaseEffects()
        if (audioSessionId == 0) {
            _state.value = EffectsState()
            return
        }

        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        bassBoost = createIfSupported({ BassBoost(0, audioSessionId) }, { it.strengthSupported })
        virtualizer = createIfSupported({ Virtualizer(0, audioSessionId) }, { it.strengthSupported })

        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        equalizer?.let { eq ->
            val saved = prefs.getString(KEY_LEVELS, null)?.split(',')?.mapNotNull { it.toIntOrNull() }
            val preset = prefs.getInt(KEY_PRESET, -1)
            runCatching {
                if (preset in 0 until eq.numberOfPresets) {
                    eq.usePreset(preset.toShort())
                } else if (saved != null && saved.size == eq.numberOfBands.toInt()) {
                    saved.forEachIndexed { band, level -> eq.setBandLevel(band.toShort(), level.toShort()) }
                }
            }
        }
        runCatching { bassBoost?.setStrength(prefs.getInt(KEY_BASS, 0).toShort()) }
        runCatching { virtualizer?.setStrength(prefs.getInt(KEY_VIRTUALIZER, 0).toShort()) }
        applyEnabled(enabled)
        publish()
    }

    @Synchronized
    fun release() {
        releaseEffects()
        _state.value = EffectsState()
    }

    @Synchronized
    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, on).apply()
        applyEnabled(on)
        publish()
    }

    @Synchronized
    fun setBandLevel(band: Int, levelMb: Int) {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        val level = levelMb.coerceIn(range[0].toInt(), range[1].toInt())
        runCatching { eq.setBandLevel(band.toShort(), level.toShort()) }
        prefs.edit().putInt(KEY_PRESET, -1).putString(KEY_LEVELS, currentLevels().joinToString(",")).apply()
        publish()
    }

    @Synchronized
    fun usePreset(index: Int) {
        val eq = equalizer ?: return
        runCatching { eq.usePreset(index.toShort()) }
        prefs.edit().putInt(KEY_PRESET, index).putString(KEY_LEVELS, currentLevels().joinToString(",")).apply()
        publish()
    }

    @Synchronized
    fun setBass(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        runCatching { bassBoost?.setStrength(value.toShort()) }
        prefs.edit().putInt(KEY_BASS, value).apply()
        publish()
    }

    @Synchronized
    fun setVirtualizer(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        runCatching { virtualizer?.setStrength(value.toShort()) }
        prefs.edit().putInt(KEY_VIRTUALIZER, value).apply()
        publish()
    }

    /** Flat curve, no bass boost, no virtualizer. */
    @Synchronized
    fun reset() {
        equalizer?.let { eq ->
            for (band in 0 until eq.numberOfBands) {
                runCatching { eq.setBandLevel(band.toShort(), 0) }
            }
        }
        runCatching { bassBoost?.setStrength(0) }
        runCatching { virtualizer?.setStrength(0) }
        prefs.edit()
            .putInt(KEY_PRESET, -1)
            .putString(KEY_LEVELS, currentLevels().joinToString(","))
            .putInt(KEY_BASS, 0)
            .putInt(KEY_VIRTUALIZER, 0)
            .apply()
        publish()
    }

    /** Creates an effect, or returns null (releasing it) when the device lacks it or its strength control. */
    private fun <T : android.media.audiofx.AudioEffect> createIfSupported(
        create: () -> T,
        supported: (T) -> Boolean,
    ): T? {
        val effect = runCatching(create).getOrNull() ?: return null
        val ok = runCatching { supported(effect) }.getOrDefault(false)
        if (!ok) {
            runCatching { effect.release() }
            return null
        }
        return effect
    }

    private fun applyEnabled(on: Boolean) {
        equalizer?.let { runCatching { it.setEnabled(on) } }
        bassBoost?.let { runCatching { it.setEnabled(on) } }
        virtualizer?.let { runCatching { it.setEnabled(on) } }
    }

    private fun currentLevels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    private fun publish() {
        val eq = equalizer
        val range = eq?.bandLevelRange
        _state.value = EffectsState(
            available = eq != null,
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            bandFrequenciesHz = if (eq == null) emptyList() else {
                // The API reports center frequencies in milliHertz.
                (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
            },
            levels = currentLevels(),
            minLevel = range?.get(0)?.toInt() ?: -1500,
            maxLevel = range?.get(1)?.toInt() ?: 1500,
            presets = if (eq == null) emptyList() else {
                (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
            },
            preset = prefs.getInt(KEY_PRESET, -1),
            bassSupported = bassBoost != null,
            bass = prefs.getInt(KEY_BASS, 0),
            virtualizerSupported = virtualizer != null,
            virtualizer = prefs.getInt(KEY_VIRTUALIZER, 0),
        )
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_LEVELS = "levels"
        const val KEY_PRESET = "preset"
        const val KEY_BASS = "bass"
        const val KEY_VIRTUALIZER = "virtualizer"
    }
}
