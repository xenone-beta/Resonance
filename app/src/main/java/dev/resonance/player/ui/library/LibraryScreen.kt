package dev.resonance.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import dev.resonance.player.data.SortKey
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.resonance.player.data.Folder
import dev.resonance.player.ui.common.Cover
import dev.resonance.player.ui.common.EmptyState
import dev.resonance.player.ui.common.TrackRow
import dev.resonance.player.ui.common.rememberTrackActions
import dev.resonance.player.ui.playlists.PlaylistsTab
import dev.resonance.player.ui.player.MiniPlayer
import dev.resonance.player.ui.common.screenColor
import dev.resonance.player.ui.common.transparentTopBarColors
import dev.resonance.player.ui.common.barColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSmart: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val library by vm.library.collectAsState()
    val query by vm.query.collectAsState()
    val songs by vm.visibleTracks.collectAsState()
    val albums by vm.visibleAlbums.collectAsState()
    val artists by vm.visibleArtists.collectAsState()
    val folders by vm.visibleFolders.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    val sortKey by vm.settings.sortKey.collectAsState()
    val sortDesc by vm.settings.sortDescending.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val actions = rememberTrackActions(vm)

    val tabs = listOf(
        R.string.tab_songs, R.string.tab_albums, R.string.tab_artists, R.string.tab_playlists, R.string.tab_folders,
    )

    Scaffold(
        containerColor = screenColor(),
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(query, vm::setQuery, Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            shape = RoundedCornerShape(28.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 3.dp,
                            offset = DpOffset((-8).dp, 0.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stats_title)) },
                                leadingIcon = { Icon(Icons.Rounded.BarChart, null) },
                                onClick = { menuOpen = false; onOpenStats() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_title)) },
                                leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                                onClick = { menuOpen = false; onOpenSettings() },
                            )
                        }
                    }
                }
                PrimaryScrollableTabRow(
                    selectedTabIndex = tab,
                    edgePadding = 8.dp,
                    containerColor = barColor(),
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge) })
                    }
                }
            }
        },
        bottomBar = { MiniPlayer(vm, onOpenPlayer) },
    ) { padding ->
        val listPad = PaddingValues(bottom = padding.calculateBottomPadding())
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when {
                library.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                tab == 0 -> if (songs.isEmpty()) {
                    EmptyState(stringResource(if (query.isBlank()) R.string.empty_library else R.string.empty_search))
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
                        item(key = "shuffle") {
                            ShuffleHeader(
                                count = songs.size,
                                sortKey = sortKey,
                                descending = sortDesc,
                                onSort = { key ->
                                    // Tapping the active key flips the direction; a new key starts in its natural direction.
                                    if (key == sortKey) vm.settings.setSort(key, !sortDesc)
                                    else vm.settings.setSort(key, key.defaultDescending)
                                },
                                onShuffle = { vm.player.play(songs, shuffle = true) },
                            )
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
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
                        items(albums, key = { it.id }) { album -> AlbumRow(album, vm) { onOpenAlbum(album.id) } }
                    }
                }
                tab == 2 -> if (artists.isEmpty()) {
                    EmptyState(stringResource(if (query.isBlank()) R.string.empty_library else R.string.empty_search))
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
                        items(artists, key = { it.name }) { artist -> ArtistRow(artist) { onOpenArtist(artist.name) } }
                    }
                }
                tab == 3 -> PlaylistsTab(vm, onOpenPlaylist, onOpenFavorites, onOpenSmart, listPad)
                else -> if (folders.isEmpty()) {
                    EmptyState(stringResource(if (query.isBlank()) R.string.empty_library else R.string.empty_search))
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
                        items(folders, key = { it.path }) { folder -> FolderRow(folder) { onOpenFolder(folder.path) } }
                    }
                }
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
private fun ShuffleHeader(
    count: Int,
    sortKey: SortKey,
    descending: Boolean,
    onSort: (SortKey) -> Unit,
    onShuffle: () -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
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
        trailingContent = {
            Box {
                IconButton(onClick = { sortOpen = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.sort_title))
                }
                DropdownMenu(
                    expanded = sortOpen,
                    onDismissRequest = { sortOpen = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    SortKey.entries.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(stringResource(sortLabel(key))) },
                            trailingIcon = {
                                if (key == sortKey) {
                                    Icon(
                                        if (descending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            onClick = { onSort(key) },
                        )
                    }
                }
            }
        },
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
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        leadingContent = {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        },
        headlineContent = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.songs_count, folder.tracks.size, folder.tracks.size) + " · " +
                    folder.path.removePrefix("/storage/emulated/0/"),
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

private fun sortLabel(key: SortKey): Int = when (key) {
    SortKey.TITLE -> R.string.sort_by_title
    SortKey.ARTIST -> R.string.sort_by_artist
    SortKey.ALBUM -> R.string.sort_by_album
    SortKey.DATE_ADDED -> R.string.sort_by_date
    SortKey.DURATION -> R.string.sort_by_duration
    SortKey.COMPOSER -> R.string.sort_by_composer
}
