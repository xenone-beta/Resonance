package dev.resonance.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.data.Album
import dev.resonance.player.data.Artist
import dev.resonance.player.ui.common.Cover
import dev.resonance.player.ui.common.EmptyState
import dev.resonance.player.ui.common.TrackRow
import dev.resonance.player.ui.common.rememberTrackActions
import dev.resonance.player.ui.playlists.PlaylistsTab
import dev.resonance.player.ui.player.MiniPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenStats: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val library by vm.library.collectAsState()
    val query by vm.query.collectAsState()
    val songs by vm.visibleTracks.collectAsState()
    val albums by vm.visibleAlbums.collectAsState()
    val artists by vm.visibleArtists.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val actions = rememberTrackActions(vm)

    val tabs = listOf(R.string.tab_songs, R.string.tab_albums, R.string.tab_artists, R.string.tab_playlists)

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(query, vm::setQuery, Modifier.weight(1f))
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Rounded.Equalizer, contentDescription = stringResource(R.string.eq_title))
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Rounded.BarChart, contentDescription = stringResource(R.string.stats_title))
                    }
                }
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(label)) })
                    }
                }
            }
        },
        bottomBar = { MiniPlayer(vm, onOpenPlayer) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                library.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                tab == 0 -> if (songs.isEmpty()) {
                    EmptyState(stringResource(if (query.isBlank()) R.string.empty_library else R.string.empty_search))
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item(key = "shuffle") {
                            ShuffleHeader(count = songs.size) { vm.player.play(songs, shuffle = true) }
                        }
                        itemsIndexed(songs, key = { _, t -> t.id }) { index, track ->
                            TrackRow(
                                track = track,
                                loader = vm.artwork,
                                isCurrent = playerState.current?.trackId == track.id,
                                onClick = { vm.player.play(songs, startIndex = index) },
                                actions = actions,
                            )
                        }
                    }
                }
                tab == 1 -> if (albums.isEmpty()) {
                    EmptyState(stringResource(if (query.isBlank()) R.string.empty_library else R.string.empty_search))
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(albums, key = { it.id }) { album -> AlbumRow(album, vm) { onOpenAlbum(album.id) } }
                    }
                }
                tab == 2 -> if (artists.isEmpty()) {
                    EmptyState(stringResource(if (query.isBlank()) R.string.empty_library else R.string.empty_search))
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(artists, key = { it.name }) { artist -> ArtistRow(artist) { onOpenArtist(artist.name) } }
                    }
                }
                else -> PlaylistsTab(vm, onOpenPlaylist)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun ShuffleHeader(count: Int, onShuffle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onShuffle),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        leadingContent = {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Shuffle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
        headlineContent = { Text(stringResource(R.string.shuffle_all)) },
        supportingContent = { Text(pluralStringResource(R.plurals.songs_count, count, count)) },
    )
}

@Composable
private fun AlbumRow(album: Album, vm: MainViewModel, onClick: () -> Unit) {
    val first = album.tracks.first()
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        leadingContent = { Cover(vm.artwork, first.id, album.id, 56.dp, corner = 14.dp) },
        headlineContent = {
            Text(
                album.title.ifEmpty { stringResource(R.string.unknown_album) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            val count = pluralStringResource(R.plurals.songs_count, album.tracks.size, album.tracks.size)
            Text(
                if (album.artist.isEmpty()) count else "${album.artist} · $count",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    val name = artist.name.ifEmpty { stringResource(R.string.unknown_artist) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        leadingContent = {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.albums_count, artist.albumCount, artist.albumCount) + " · " +
                    pluralStringResource(R.plurals.songs_count, artist.tracks.size, artist.tracks.size),
            )
        },
    )
}
