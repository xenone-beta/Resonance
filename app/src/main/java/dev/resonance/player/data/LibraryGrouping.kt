package dev.resonance.player.data

import java.text.Normalizer

/** Pure functions that turn a flat track list into albums and artists, and filter it by a search query. */
object LibraryGrouping {
    /** Orders songs by [key]. Songs without the tag (no artist, album or composer) go last when ascending. */
    fun sort(tracks: List<Track>, key: SortKey, descending: Boolean): List<Track> {
        val ci = String.CASE_INSENSITIVE_ORDER
        val last = "\uFFFF"
        val comparator: Comparator<Track> = when (key) {
            SortKey.TITLE -> compareBy<Track, String>(ci) { it.title }
            SortKey.ARTIST -> compareBy<Track, String>(ci) { it.artist.ifEmpty { last } }.thenBy(ci) { it.title }
            SortKey.ALBUM -> compareBy<Track, String>(ci) { it.album.ifEmpty { last } }
                .thenBy { it.trackNumber }.thenBy(ci) { it.title }
            SortKey.DATE_ADDED -> compareBy<Track> { it.dateAdded }.thenBy(ci) { it.title }
            SortKey.DURATION -> compareBy<Track> { it.durationMs }.thenBy(ci) { it.title }
            SortKey.COMPOSER -> compareBy<Track, String>(ci) { it.composer.ifEmpty { last } }.thenBy(ci) { it.title }
        }
        return tracks.sortedWith(if (descending) comparator.reversed() else comparator)
    }


    fun albums(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumId }
            .map { (albumId, list) ->
                val ordered = list.sortedWith(compareBy({ it.trackNumber }, { it.title.lowercase() }))
                val artists = ordered.map { it.artist }.filter { it.isNotBlank() }.distinct()
                Album(
                    id = albumId,
                    title = ordered.first().album,
                    // Several different artists on one album: show nothing specific.
                    artist = if (artists.size == 1) artists.first() else "",
                    tracks = ordered,
                )
            }
            .sortedBy { it.title.lowercase() }

    fun artists(tracks: List<Track>): List<Artist> =
        tracks.groupBy { it.artist }
            .map { (name, list) ->
                Artist(
                    name = name,
                    tracks = list.sortedBy { it.title.lowercase() },
                    albumCount = list.map { it.albumId }.distinct().size,
                )
            }
            .sortedBy { it.name.lowercase() }

    fun folders(tracks: List<Track>): List<Folder> =
        tracks.filter { it.folder.isNotEmpty() }
            .groupBy { it.folder }
            .map { (path, list) -> Folder(path, path.substringAfterLast('/'), list) }
            .sortedBy { it.name.lowercase() }

    fun searchFolders(folders: List<Folder>, query: String): List<Folder> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return folders
        return folders.filter { folder ->
            val haystack = normalize(folder.name)
            words.all { haystack.contains(it) }
        }
    }

    /** Keeps tracks whose title, artist or album contains every word of [query] (ignoring case and accents). */
    fun search(tracks: List<Track>, query: String): List<Track> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return tracks
        return tracks.filter { track ->
            val haystack = normalize("${track.title} ${track.artist} ${track.album}")
            words.all { haystack.contains(it) }
        }
    }

    fun searchAlbums(albums: List<Album>, query: String): List<Album> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return albums
        return albums.filter { album ->
            val haystack = normalize("${album.title} ${album.artist}")
            words.all { haystack.contains(it) }
        }
    }

    fun searchArtists(artists: List<Artist>, query: String): List<Artist> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return artists
        return artists.filter { artist ->
            val haystack = normalize(artist.name)
            words.all { haystack.contains(it) }
        }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('ё', 'е')
            .trim()
}
