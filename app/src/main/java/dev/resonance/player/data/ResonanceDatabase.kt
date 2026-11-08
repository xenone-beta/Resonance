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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

// ---- Favorites -----------------------------------------------------------------------------

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long,
)

@Dao
interface FavoritesDao {
    @Query("SELECT trackId FROM favorites ORDER BY addedAt DESC")
    fun observeIds(): Flow<List<Long>>

    @Upsert
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun remove(trackId: Long)
}

// ---- Edited song info ----------------------------------------------------------------------

/** Title, artist or album the user typed for a song. Null means "keep what the file says". Files are never touched. */
@Entity(tableName = "track_overrides")
data class TrackOverrideEntity(
    @PrimaryKey val trackId: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
)

@Dao
interface OverridesDao {
    @Query("SELECT * FROM track_overrides")
    fun observeAll(): Flow<List<TrackOverrideEntity>>

    @Upsert
    suspend fun upsert(entity: TrackOverrideEntity)

    @Query("DELETE FROM track_overrides WHERE trackId = :trackId")
    suspend fun delete(trackId: Long)
}

// ---- Daily totals (for charts and streaks) -------------------------------------------------

/** Listening per calendar day. [day] is the local epoch day (days since 1970-01-01). */
@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val day: Long,
    val totalMs: Long,
    val plays: Int,
)

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_stats WHERE day = :day")
    suspend fun get(day: Long): DailyStatEntity?

    @Upsert
    suspend fun upsert(entity: DailyStatEntity)

    @Query("SELECT * FROM daily_stats ORDER BY day DESC LIMIT 800")
    fun observeAll(): Flow<List<DailyStatEntity>>

    @Query("DELETE FROM daily_stats")
    suspend fun clear()
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
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, PlayStatEntity::class, FavoriteEntity::class, DailyStatEntity::class, TrackOverrideEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class ResonanceDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun statsDao(): StatsDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun dailyDao(): DailyDao
    abstract fun overridesDao(): OverridesDao

    companion object {
        fun create(context: Context): ResonanceDatabase =
            Room.databaseBuilder(context.applicationContext, ResonanceDatabase::class.java, "resonance.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        /** Version 4 adds edited song info. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `track_overrides` (`trackId` INTEGER NOT NULL, " +
                        "`title` TEXT, `artist` TEXT, `album` TEXT, PRIMARY KEY(`trackId`))",
                )
            }
        }

        /** Version 3 adds per-day totals; everything else is kept. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_stats` (`day` INTEGER NOT NULL, " +
                        "`totalMs` INTEGER NOT NULL, `plays` INTEGER NOT NULL, PRIMARY KEY(`day`))",
                )
            }
        }

        /** Version 2 adds favorites; playlists and statistics are kept. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (`trackId` INTEGER NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, PRIMARY KEY(`trackId`))",
                )
            }
        }
    }
}

/** Writes listening totals. Time is always added; the play counter only when the track counted as a play. */
class StatsRepository(private val dao: StatsDao, private val daily: DailyDao) {
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
        val today = java.time.LocalDate.now().toEpochDay()
        val oldDay = daily.get(today)
        daily.upsert(
            DailyStatEntity(
                day = today,
                totalMs = (oldDay?.totalMs ?: 0L) + listenedMs,
                plays = (oldDay?.plays ?: 0) + if (countedAsPlay) 1 else 0,
            ),
        )
    }

    private companion object {
        const val MIN_RECORDED_MS = 3_000L
    }
}
