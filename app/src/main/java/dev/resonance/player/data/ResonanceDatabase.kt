package dev.resonance.player.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// ---- Playlists -----------------------------------------------------------------------------

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** One song in a playlist. [trackId] is the MediaStore id, so playlists survive a rescan. */
@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
)

@Dao
interface PlaylistDao {
    @Query(
        "SELECT p.id AS id, p.name AS name, COUNT(t.id) AS trackCount " +
            "FROM playlists p LEFT JOIN playlist_tracks t ON t.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.name COLLATE NOCASE",
    )
    fun observeSummaries(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position")
    fun observeEntries(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT name FROM playlists WHERE id = :playlistId")
    fun observeName(playlistId: Long): Flow<String?>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun insertEntries(entries: List<PlaylistTrackEntity>)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int
}

// ---- Listening statistics ------------------------------------------------------------------

/** Per-track totals. Title and artist are copied here so statistics outlive a deleted file. */
@Entity(tableName = "play_stats")
data class PlayStatEntity(
    @PrimaryKey val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val playCount: Int,
    val totalMs: Long,
    val lastPlayed: Long,
)

data class ArtistStat(
    val artist: String,
    val plays: Int,
    val totalMs: Long,
)

@Dao
interface StatsDao {
    @Query("SELECT * FROM play_stats WHERE trackId = :trackId")
    suspend fun get(trackId: Long): PlayStatEntity?

    @Upsert
    suspend fun upsert(entity: PlayStatEntity)

    @Query("SELECT * FROM play_stats WHERE playCount > 0 ORDER BY playCount DESC, totalMs DESC LIMIT :limit")
    fun observeTopTracks(limit: Int): Flow<List<PlayStatEntity>>

    @Query("SELECT * FROM play_stats WHERE playCount > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlayStatEntity>>

    @Query(
        "SELECT artist AS artist, SUM(playCount) AS plays, SUM(totalMs) AS totalMs FROM play_stats " +
            "WHERE artist != '' AND playCount > 0 GROUP BY artist ORDER BY plays DESC, totalMs DESC LIMIT :limit",
    )
    fun observeTopArtists(limit: Int): Flow<List<ArtistStat>>

    @Query("SELECT COALESCE(SUM(playCount), 0) FROM play_stats")
    fun observeTotalPlays(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalMs), 0) FROM play_stats")
    fun observeTotalMs(): Flow<Long>

    @Query("DELETE FROM play_stats")
    suspend fun clear()
}

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, PlayStatEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ResonanceDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun statsDao(): StatsDao

    companion object {
        fun create(context: Context): ResonanceDatabase =
            Room.databaseBuilder(context.applicationContext, ResonanceDatabase::class.java, "resonance.db").build()
    }
}

/** Writes listening totals. Time is always added; the play counter only when the track counted as a play. */
class StatsRepository(private val dao: StatsDao) {
    suspend fun record(
        trackId: Long,
        title: String,
        artist: String,
        album: String,
        listenedMs: Long,
        countedAsPlay: Boolean,
    ) {
        if (listenedMs < MIN_RECORDED_MS) return
        val old = dao.get(trackId)
        dao.upsert(
            PlayStatEntity(
                trackId = trackId,
                title = title,
                artist = artist,
                album = album,
                playCount = (old?.playCount ?: 0) + if (countedAsPlay) 1 else 0,
                totalMs = (old?.totalMs ?: 0L) + listenedMs,
                lastPlayed = System.currentTimeMillis(),
            ),
        )
    }

    private companion object {
        const val MIN_RECORDED_MS = 3_000L
    }
}
