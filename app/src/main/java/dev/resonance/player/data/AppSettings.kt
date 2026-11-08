package dev.resonance.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Colors offered when the system wallpaper colors are not used. */
val ACCENT_SEEDS: List<Int> = listOf(
    0xFF4F6BD8.toInt(), // indigo
    0xFF16897B.toInt(), // teal
    0xFF4C9A2A.toInt(), // green
    0xFFD9A400.toInt(), // amber
    0xFFE8742C.toInt(), // orange
    0xFFD2455A.toInt(), // red
    0xFFC2509B.toInt(), // pink
    0xFF8A5BD1.toInt(), // purple
)

object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
    const val AMOLED = 3
}

object ColorSource {
    const val WALLPAPER = 0
    const val ACCENT = 1
}

/** User preferences, kept in SharedPreferences and exposed as flows. Shared with the playback service. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("resonance_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getInt(KEY_THEME, ThemeMode.SYSTEM))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _colorSource = MutableStateFlow(prefs.getInt(KEY_SOURCE, ColorSource.WALLPAPER))
    val colorSource: StateFlow<Int> = _colorSource.asStateFlow()

    private val _accent = MutableStateFlow(prefs.getInt(KEY_ACCENT, ACCENT_SEEDS.first()))
    val accent: StateFlow<Int> = _accent.asStateFlow()

    private val _albumColors = MutableStateFlow(prefs.getBoolean(KEY_ALBUM_COLORS, true))
    val albumColors: StateFlow<Boolean> = _albumColors.asStateFlow()

    private val _visualizer = MutableStateFlow(prefs.getBoolean(KEY_VISUALIZER, true))
    val visualizer: StateFlow<Boolean> = _visualizer.asStateFlow()

    private val _speed = MutableStateFlow(prefs.getFloat(KEY_SPEED, 1f))
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _skipSilence = MutableStateFlow(prefs.getBoolean(KEY_SKIP_SILENCE, false))
    val skipSilence: StateFlow<Boolean> = _skipSilence.asStateFlow()

    private val _vinyl = MutableStateFlow(prefs.getBoolean(KEY_VINYL, false))
    val vinylMode: StateFlow<Boolean> = _vinyl.asStateFlow()

    private val _headphonesAutoplay = MutableStateFlow(prefs.getBoolean(KEY_HEADPHONES, false))
    val headphonesAutoplay: StateFlow<Boolean> = _headphonesAutoplay.asStateFlow()

    /** Folder (a SAF tree URI) that holds .lrc lyric files, or null. */
    private val _lyricsFolder = MutableStateFlow(prefs.getString(KEY_LYRICS, null))
    val lyricsFolder: StateFlow<String?> = _lyricsFolder.asStateFlow()

    private val _sortKey = MutableStateFlow(SortKey.fromOrdinal(prefs.getInt(KEY_SORT, 0)))
    val sortKey: StateFlow<SortKey> = _sortKey.asStateFlow()

    private val _sortDesc = MutableStateFlow(prefs.getBoolean(KEY_SORT_DESC, false))
    val sortDescending: StateFlow<Boolean> = _sortDesc.asStateFlow()

    /** Pitch shift in semitones, -6..+6. */
    private val _pitch = MutableStateFlow(prefs.getInt(KEY_PITCH, 0).coerceIn(-6, 6))
    val pitchSemitones: StateFlow<Int> = _pitch.asStateFlow()

    /** How much the background photo is darkened (or lightened in a light theme), 0.1..0.85. */
    private val _bgDim = MutableStateFlow(prefs.getFloat(KEY_BG_DIM, 0.5f))
    val backgroundDim: StateFlow<Float> = _bgDim.asStateFlow()

    fun setSort(key: SortKey, descending: Boolean) {
        prefs.edit().putInt(KEY_SORT, key.ordinal).putBoolean(KEY_SORT_DESC, descending).apply()
        _sortKey.value = key
        _sortDesc.value = descending
    }
    fun setPitchSemitones(value: Int) { val v = value.coerceIn(-6, 6); prefs.edit().putInt(KEY_PITCH, v).apply(); _pitch.value = v }
    fun setBackgroundDim(value: Float) { val v = value.coerceIn(0.1f, 0.85f); prefs.edit().putFloat(KEY_BG_DIM, v).apply(); _bgDim.value = v }

    fun setVinylMode(value: Boolean) { prefs.edit().putBoolean(KEY_VINYL, value).apply(); _vinyl.value = value }
    fun setHeadphonesAutoplay(value: Boolean) { prefs.edit().putBoolean(KEY_HEADPHONES, value).apply(); _headphonesAutoplay.value = value }
    fun setLyricsFolder(value: String?) { prefs.edit().putString(KEY_LYRICS, value).apply(); _lyricsFolder.value = value }

    fun setThemeMode(value: Int) { prefs.edit().putInt(KEY_THEME, value).apply(); _themeMode.value = value }
    fun setColorSource(value: Int) { prefs.edit().putInt(KEY_SOURCE, value).apply(); _colorSource.value = value }
    fun setAccent(value: Int) { prefs.edit().putInt(KEY_ACCENT, value).apply(); _accent.value = value }
    fun setAlbumColors(value: Boolean) { prefs.edit().putBoolean(KEY_ALBUM_COLORS, value).apply(); _albumColors.value = value }
    fun setVisualizer(value: Boolean) { prefs.edit().putBoolean(KEY_VISUALIZER, value).apply(); _visualizer.value = value }
    fun setSpeed(value: Float) { prefs.edit().putFloat(KEY_SPEED, value).apply(); _speed.value = value }
    fun setSkipSilence(value: Boolean) { prefs.edit().putBoolean(KEY_SKIP_SILENCE, value).apply(); _skipSilence.value = value }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SOURCE = "color_source"
        const val KEY_ACCENT = "accent"
        const val KEY_ALBUM_COLORS = "album_colors"
        const val KEY_VISUALIZER = "visualizer"
        const val KEY_SPEED = "speed"
        const val KEY_SKIP_SILENCE = "skip_silence"
        const val KEY_VINYL = "vinyl"
        const val KEY_HEADPHONES = "headphones_autoplay"
        const val KEY_LYRICS = "lyrics_folder"
        const val KEY_SORT = "sort_key"
        const val KEY_SORT_DESC = "sort_desc"
        const val KEY_PITCH = "pitch"
        const val KEY_BG_DIM = "bg_dim"
    }
}
