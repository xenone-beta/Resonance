package dev.resonance.player.core

import dev.resonance.player.data.LibraryGrouping
import dev.resonance.player.data.SortKey
import dev.resonance.player.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupingTest {

    private fun track(id: Long, title: String, artist: String, album: String, albumId: Long, number: Int = 0) =
        Track(id, title, artist, album, albumId, 180_000, number, 0)

    private val tracks = listOf(
        track(1, "Second Song", "Band A", "First Album", 10, 2),
        track(2, "First Song", "Band A", "First Album", 10, 1),
        track(3, "Другая песня", "Группа Ёлка", "Сборник", 20, 1),
        track(4, "Duet", "Band B", "Mixed", 30, 1),
        track(5, "Duet Two", "Band C", "Mixed", 30, 2),
        track(6, "Café Music", "Band A", "Solo", 40, 1),
    )

    @Test
    fun groupsAlbumsInTrackOrder() {
        val albums = LibraryGrouping.albums(tracks)
        assertEquals(listOf("First Album", "Mixed", "Solo", "Сборник"), albums.map { it.title })
        val first = albums.first()
        assertEquals(listOf("First Song", "Second Song"), first.tracks.map { it.title })
        assertEquals("Band A", first.artist)
    }

    @Test
    fun albumWithSeveralArtistsHasNoSingleArtist() {
        val mixed = LibraryGrouping.albums(tracks).first { it.title == "Mixed" }
        assertEquals("", mixed.artist)
    }

    @Test
    fun groupsArtistsWithAlbumCount() {
        val artists = LibraryGrouping.artists(tracks)
        val bandA = artists.first { it.name == "Band A" }
        assertEquals(3, bandA.tracks.size)
        assertEquals(2, bandA.albumCount)
    }

    @Test
    fun searchMatchesAllWordsIgnoringCaseAndAccents() {
        assertEquals(listOf(6L), LibraryGrouping.search(tracks, "cafe").map { it.id })
        assertEquals(listOf(1L, 2L), LibraryGrouping.search(tracks, "band a first").map { it.id }.sorted())
        assertEquals(listOf(3L), LibraryGrouping.search(tracks, "елка").map { it.id })
    }

    @Test
    fun emptyQueryKeepsEverything() {
        assertEquals(tracks, LibraryGrouping.search(tracks, "   "))
        assertTrue(LibraryGrouping.search(tracks, "zzz").isEmpty())
    }

    @Test
    fun searchesAlbumsAndArtists() {
        assertEquals(listOf("Mixed"), LibraryGrouping.searchAlbums(LibraryGrouping.albums(tracks), "mix").map { it.title })
        assertEquals(listOf("Band C"), LibraryGrouping.searchArtists(LibraryGrouping.artists(tracks), "band c").map { it.name })
    }

    @Test
    fun groupsAndSearchesFolders() {
        val withFolders = listOf(
            tracks[0].copy(folder = "/storage/emulated/0/Music/Rock"),
            tracks[1].copy(folder = "/storage/emulated/0/Music/Rock"),
            tracks[2].copy(folder = "/storage/emulated/0/Download"),
            tracks[3],
        )
        val folders = LibraryGrouping.folders(withFolders)
        assertEquals(listOf("Download", "Rock"), folders.map { it.name })
        assertEquals(2, folders.last().tracks.size)
        assertEquals(listOf("Rock"), LibraryGrouping.searchFolders(folders, "roc").map { it.name })
    }

    @Test
    fun sortsByDateLengthAndComposer() {
        val a = Track(1, "A", "", "", 1, 100_000, 0, 10, composer = "Zed")
        val b = Track(2, "B", "", "", 1, 300_000, 0, 30, composer = "")
        val c = Track(3, "C", "", "", 1, 200_000, 0, 20, composer = "Amy")
        val all = listOf(a, b, c)
        assertEquals(listOf(2L, 3L, 1L), LibraryGrouping.sort(all, SortKey.DATE_ADDED, true).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), LibraryGrouping.sort(all, SortKey.DURATION, false).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), LibraryGrouping.sort(all, SortKey.COMPOSER, false).map { it.id })
    }
}
