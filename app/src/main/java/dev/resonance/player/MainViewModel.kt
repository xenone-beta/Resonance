package dev.resonance.player

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.resonance.player.data.Album
import dev.resonance.player.data.Artist
import dev.resonance.player.data.FavoriteEntity
import dev.resonance.player.data.Folder
import dev.resonance.player.data.ArtistStat
import dev.resonance.player.data.LibraryGrouping
import dev.resonance.player.core.LyricLine
import dev.resonance.player.core.Streaks
import dev.resonance.player.data.DailyStatEntity
import dev.resonance.player.data.PlayStatEntity
import dev.resonance.player.data.PlaylistEntity
import dev.resonance.player.data.PlaylistSummary
import dev.resonance.player.data.PlaylistTrackEntity
import dev.resonance.player.data.Track
import dev.resonance.player.data.TrackOverrideEntity
import dev.resonance.player.core.Lrc
import dev.resonance.player.playback.ArtworkLoader
import dev.resonance.player.playback.EffectsController
import dev.resonance.player.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LyricsState {
    data object NoFolder : LyricsState
    data object NotFound : LyricsState
    data class Found(val lines: List<LyricLine>) : LyricsState
}

/** A ready-made list such as "recently added". */
enum class SmartList { RECENT, MOST_PLAYED, NEVER_PLAYED }

data class LibraryState(
    val loading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
)

/** A playlist row: the stored entry plus the song it points to (null when the file is gone). */
data class PlaylistItem(val entryId: Long, val track: Track?)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val resonance = app as ResonanceApplication
    private val db = resonance.database
    private val playlistDao = db.playlistDao()
    private val statsDao = db.statsDao()
    private val favoritesDao = db.favoritesDao()

    val player = PlayerConnection(app)
    val artwork = ArtworkLoader(app, resonance.userFiles)
    val effects: EffectsController = resonance.effects
    val settings = resonance.settings
    val sleepTimer = resonance.sleepTimer
    val files = resonance.userFiles
    private val overridesDao = db.overridesDao()

    // ---- Library ---------------------------------------------------------------------------

    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(text: String) { _query.value = text }

    @OptIn(FlowPreview::class)
    private val debouncedQuery = _query.debounce(150)

    val visibleTracks: StateFlow<List<Track>> = combine(
        _library, debouncedQuery, settings.sortKey, settings.sortDescending,
    ) { lib, q, key, desc ->
        LibraryGrouping.sort(LibraryGrouping.search(lib.tracks, q), key, desc)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visibleAlbums: StateFlow<List<Album>> = combine(_library, debouncedQuery) { lib, q ->
        LibraryGrouping.searchAlbums(lib.albums, q)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visibleArtists: StateFlow<List<Artist>> = combine(_library, debouncedQuery) { lib, q ->
        LibraryGrouping.searchArtists(lib.artists, q)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visibleFolders: StateFlow<List<Folder>> = combine(_library, debouncedQuery) { lib, q ->
        LibraryGrouping.searchFolders(lib.folders, q)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var libraryStarted = false

    /** Called once the audio permission is granted. Safe to call repeatedly. */
    fun startLibrary() {
        if (libraryStarted) return
        libraryStarted = true
        viewModelScope.launch { refreshLibrary() }
        viewModelScope.launch {
            resonance.library.changes().debounce(1000).collect { refreshLibrary() }
        }
    }

    /** Songs exactly as the files describe them. The library is these plus the user's edits. */
    private val _raw = MutableStateFlow<List<Track>?>(null)

    private suspend fun refreshLibrary() {
        _raw.value = runCatching { resonance.library.load() }.getOrDefault(emptyList())
    }

    /** Rebuilds the library whenever the scan or an edit changes. */
    private fun startGrouping() {
        viewModelScope.launch {
            combine(_raw.filterNotNull(), overridesDao.observeAll()) { raw, edits ->
                val byId = edits.associateBy { it.trackId }
                val tracks = raw.map { t ->
                    val e = byId[t.id] ?: return@map t
                    t.copy(title = e.title ?: t.title, artist = e.artist ?: t.artist, album = e.album ?: t.album)
                }
                LibraryState(false, tracks, LibraryGrouping.albums(tracks), LibraryGrouping.artists(tracks), LibraryGrouping.folders(tracks))
            }.flowOn(Dispatchers.Default).collect { _library.value = it }
        }
    }

    /** The song as its file describes it, ignoring edits. */
    fun originalTrack(id: Long): Track? = _raw.value?.firstOrNull { it.id == id }

    /** Saves edited info. A field left blank, or equal to the file's, goes back to what the file says. */
    fun saveTrackInfo(trackId: Long, title: String, artist: String, album: String) {
        val original = originalTrack(trackId) ?: return
        fun pick(new: String, old: String) = new.trim().takeIf { it.isNotEmpty() && it != old }
        val t = pick(title, original.title)
        val a = pick(artist, original.artist)
        val b = pick(album, original.album)
        viewModelScope.launch {
            if (t == null && a == null && b == null) overridesDao.delete(trackId)
            else overridesDao.upsert(TrackOverrideEntity(trackId, t, a, b))
        }
    }

    fun resetTrackInfo(trackId: Long) {
        viewModelScope.launch { overridesDao.delete(trackId) }
    }

    fun trackById(id: Long): Track? = _library.value.tracks.firstOrNull { it.id == id }

    // ---- Playback and cover colors ----------------------------------------------------------

    private val _cover = MutableStateFlow<Bitmap?>(null)
    val cover: StateFlow<Bitmap?> = _cover.asStateFlow()

    private val _seed = MutableStateFlow<Int?>(null)
    val seed: StateFlow<Int?> = _seed.asStateFlow()

    init {
        startGrouping()
        player.connect()
        viewModelScope.launch {
            combine(player.state.map { it.current }.distinctUntilChanged(), artwork.version) { entry, _ -> entry }
                .collect { entry ->
                    if (entry == null) {
                        _cover.value = null
                        _seed.value = null
                    } else {
                        val bitmap = artwork.load(entry.trackId, entry.albumId, COVER_SIZE)
                        _cover.value = bitmap
                        _seed.value = bitmap?.let { artwork.seedColor(it) }
                    }
                }
        }
    }

    // ---- Lyrics ----------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    val lyrics: StateFlow<LyricsState> = combine(
        player.state.map { it.current }.distinctUntilChanged(),
        settings.lyricsFolder,
        files.lyricsVersion,
    ) { entry, folder, _ -> entry to folder }
        .mapLatest { (entry, folder) ->
            if (entry == null) return@mapLatest LyricsState.NotFound
            // Lyrics the user wrote or imported win over files in the lyrics folder.
            val own = kotlinx.coroutines.withContext(Dispatchers.IO) { files.ownLyrics(entry.trackId) }
            when {
                own != null -> Lrc.parse(own).takeIf { it.isNotEmpty() }?.let { LyricsState.Found(it) } ?: LyricsState.NotFound
                folder == null -> LyricsState.NoFolder
                else -> resonance.lyrics.find(folder, entry)?.let { LyricsState.Found(it) } ?: LyricsState.NotFound
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LyricsState.NotFound)

    fun ownLyrics(trackId: Long): String? = files.ownLyrics(trackId)

    fun saveLyrics(trackId: Long, text: String) {
        viewModelScope.launch(Dispatchers.IO) { files.saveLyrics(trackId, text) }
    }

    /** Reads a picked .lrc or .txt file. Null when it cannot be read. */
    suspend fun readTextFile(uri: android.net.Uri): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
    }

    // ---- Covers and background -------------------------------------------------------------

    fun hasCustomCover(trackId: Long): Boolean = files.customCover(trackId) != null

    fun setCover(trackId: Long, uri: android.net.Uri, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(files.saveCover(trackId, uri)) }
    }

    fun removeCover(trackId: Long) = files.removeCover(trackId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val background: StateFlow<Bitmap?> = files.backgroundVersion
        .mapLatest { files.loadBackground() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setBackground(uri: android.net.Uri, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(files.saveBackground(uri)) }
    }

    fun removeBackground() = files.removeBackground()

    // ---- Smart lists -----------------------------------------------------------------------

    private val playedIds = statsDao.observeTopTracks(Int.MAX_VALUE)
        .map { stats -> stats.map { it.trackId } }

    /** The songs of a [SmartList], recomputed when the library or the statistics change. */
    fun smartTracks(kind: SmartList): kotlinx.coroutines.flow.Flow<List<Track>> =
        combine(_library, playedIds) { lib, played ->
            when (kind) {
                SmartList.RECENT -> lib.tracks.sortedByDescending { it.dateAdded }.take(SMART_LIMIT)
                SmartList.MOST_PLAYED -> {
                    val byId = lib.tracks.associateBy { it.id }
                    played.mapNotNull { byId[it] }.take(SMART_LIMIT)
                }
                SmartList.NEVER_PLAYED -> {
                    val seen = played.toSet()
                    lib.tracks.filter { it.id !in seen }
                }
            }
        }

    // ---- Playlists -------------------------------------------------------------------------

    val playlists: StateFlow<List<PlaylistSummary>> = playlistDao.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playlistName(id: Long) = playlistDao.observeName(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun playlistItems(id: Long) = combine(playlistDao.observeEntries(id), _library) { entries, lib ->
        val byId = lib.tracks.associateBy { it.id }
        entries.map { PlaylistItem(it.id, byId[it.trackId]) }
    }

    fun createPlaylist(name: String, tracks: List<Track> = emptyList()) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = playlistDao.insertPlaylist(PlaylistEntity(name = clean, createdAt = System.currentTimeMillis()))
            appendTracks(id, tracks)
        }
    }

    fun addToPlaylist(playlistId: Long, tracks: List<Track>) {
        viewModelScope.launch { appendTracks(playlistId, tracks) }
    }

    private suspend fun appendTracks(playlistId: Long, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val start = playlistDao.maxPosition(playlistId) + 1
        playlistDao.insertEntries(
            tracks.mapIndexed { i, t -> PlaylistTrackEntity(playlistId = playlistId, trackId = t.id, position = start + i) },
        )
    }

    fun renamePlaylist(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) viewModelScope.launch { playlistDao.rename(id, clean) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { playlistDao.delete(id) }
    }

    fun removeFromPlaylist(entryId: Long) {
        viewModelScope.launch { playlistDao.deleteEntry(entryId) }
    }

    // ---- Favorites -------------------------------------------------------------------------

    val favoriteIds: StateFlow<Set<Long>> = favoritesDao.observeIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Favorite songs that are still on the device, newest favorite first. */
    val favoriteTracks: StateFlow<List<Track>> = combine(favoritesDao.observeIds(), _library) { ids, lib ->
        val byId = lib.tracks.associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun isFavorite(trackId: Long) = trackId in favoriteIds.value

    fun toggleFavorite(trackId: Long) {
        if (trackId < 0) return
        viewModelScope.launch {
            if (isFavorite(trackId)) favoritesDao.remove(trackId)
            else favoritesDao.add(FavoriteEntity(trackId, System.currentTimeMillis()))
        }
    }

    // ---- Statistics ------------------------------------------------------------------------

    val topTracks: StateFlow<List<PlayStatEntity>> = statsDao.observeTopTracks(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent: StateFlow<List<PlayStatEntity>> = statsDao.observeRecent(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topArtists: StateFlow<List<ArtistStat>> = statsDao.observeTopArtists(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalPlays: StateFlow<Int> = statsDao.observeTotalPlays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val totalMs: StateFlow<Long> = statsDao.observeTotalMs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val dailyDao = db.dailyDao()

    val dailyStats: StateFlow<List<DailyStatEntity>> = dailyDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearStats() {
        viewModelScope.launch {
            statsDao.clear()
            dailyDao.clear()
        }
    }

    override fun onCleared() {
        player.disconnect()
    }

    private companion object {
        const val COVER_SIZE = 768
        const val SMART_LIMIT = 100
    }
}
