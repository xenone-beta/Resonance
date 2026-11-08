package dev.resonance.player

import android.app.Application
import dev.resonance.player.data.AppSettings
import dev.resonance.player.data.LibraryRepository
import dev.resonance.player.data.ResonanceDatabase
import dev.resonance.player.data.StatsRepository
import dev.resonance.player.playback.EffectsController
import dev.resonance.player.playback.LyricsRepository
import dev.resonance.player.playback.SleepTimer

/**
 * Hand-rolled dependency container. The playback service and the UI live in the same process,
 * so they share these objects directly.
 */
class ResonanceApplication : Application() {
    val database: ResonanceDatabase by lazy { ResonanceDatabase.create(this) }
    val library: LibraryRepository by lazy { LibraryRepository(this) }
    val stats: StatsRepository by lazy { StatsRepository(database.statsDao(), database.dailyDao()) }
    val effects: EffectsController by lazy { EffectsController(this) }
    val settings: AppSettings by lazy { AppSettings(this) }
    val sleepTimer: SleepTimer by lazy { SleepTimer() }
    val lyrics: LyricsRepository by lazy { LyricsRepository(this) }
}
