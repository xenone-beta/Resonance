package dev.resonance.player.ui.library

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.data.Album
import dev.resonance.player.data.Artist
import dev.resonance.player.ui.common.Cover
import dev.resonance.player.ui.common.PlaylistPickerDialog
import dev.resonance.player.ui.common.rememberImagePicker

/** Albums as a two-column grid of large covers, each with a small menu. */
@Composable
fun AlbumGrid(albums: List<Album>, vm: MainViewModel, padding: PaddingValues, onOpen: (Long) -> Unit) {
    var addTo by remember { mutableStateOf<Album?>(null) }
    val playlists by vm.playlists.collectAsState()
    addTo?.let { album ->
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { vm.addToPlaylist(it.id, album.tracks); addTo = null },
            onCreate = { vm.createPlaylist(it, album.tracks); addTo = null },
            onDismiss = { addTo = null },
        )
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, vm, onClick = { onOpen(album.id) }, onAddToPlaylist = { addTo = album }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun AlbumCard(album: Album, vm: MainViewModel, onClick: () -> Unit, onAddToPlaylist: () -> Unit, modifier: Modifier = Modifier) {
    val first = album.tracks.first()
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier.clickable(onClick = onClick)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Cover(vm.artwork, first.id, album.id, maxWidth, corner = 20.dp)
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                ) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.play_all)) }, onClick = { menuOpen = false; vm.player.play(album.tracks) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shuffle)) }, onClick = { menuOpen = false; vm.player.play(album.tracks, shuffle = true) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_add_queue)) }, onClick = { menuOpen = false; vm.player.addToQueue(album.tracks) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_add_playlist)) }, onClick = { menuOpen = false; onAddToPlaylist() })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            album.title.ifEmpty { stringResource(R.string.unknown_album) },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val count = pluralStringResource(R.plurals.songs_count, album.tracks.size, album.tracks.size)
        Text(
            (album.artist.ifEmpty { stringResource(R.string.unknown_artist) }) + " · " + count,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Round artist picture: the user's photo when there is one, otherwise the first letter. */
@Composable
fun ArtistAvatar(vm: MainViewModel, name: String, size: Dp, modifier: Modifier = Modifier) {
    val version by vm.files.artistVersion.collectAsState()
    val photo by produceState<Bitmap?>(null, name, version) { value = vm.files.loadArtistPhoto(name) }
    val label = name.ifEmpty { stringResource(R.string.unknown_artist) }
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = photo
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                label.first().uppercase(),
                fontSize = (size.value / 2.4f).sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Opens the gallery or the album chooser for an artist and shows the matching dialog. */
class ArtistPhotoEditor(
    val pickFromGallery: (String) -> Unit,
    val pickFromAlbum: (Artist) -> Unit,
)

@Composable
fun rememberArtistPhotoEditor(vm: MainViewModel): ArtistPhotoEditor {
    var target by remember { mutableStateOf<String?>(null) }
    var albumFor by remember { mutableStateOf<Artist?>(null) }
    val openPicker = rememberImagePicker { uri, done ->
        val name = target
        if (name == null) done(false) else vm.setArtistPhoto(name, uri, done)
    }
    albumFor?.let { artist ->
        val covers = remember(artist) { artist.tracks.groupBy { it.albumId }.values.map { it.first() } }
        AlertDialog(
            onDismissRequest = { albumFor = null },
            title = { Text(stringResource(R.string.artist_photo_pick_album)) },
            text = {
                LazyColumn {
                    items(covers, key = { it.albumId }) { track ->
                        ListItem(
                            modifier = Modifier.clickable { vm.setArtistPhotoFromCover(artist.name, track); albumFor = null },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Cover(vm.artwork, track.id, track.albumId, 48.dp, corner = 10.dp) },
                            headlineContent = {
                                Text(track.album.ifEmpty { stringResource(R.string.unknown_album) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { albumFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    return remember(vm) {
        ArtistPhotoEditor(
            pickFromGallery = { name -> target = name; openPicker() },
            pickFromAlbum = { albumFor = it },
        )
    }
}

/** The three photo actions for one artist, shown as a dropdown. */
@Composable
fun ArtistPhotoMenu(vm: MainViewModel, artist: Artist, editor: ArtistPhotoEditor, expanded: Boolean, onDismiss: () -> Unit) {
    val version by vm.files.artistVersion.collectAsState()
    val hasPhoto = remember(artist.name, version) { vm.files.hasArtistPhoto(artist.name) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        DropdownMenuItem(text = { Text(stringResource(R.string.artist_photo_change)) }, onClick = { onDismiss(); editor.pickFromGallery(artist.name) })
        if (artist.albumCount > 0) {
            DropdownMenuItem(text = { Text(stringResource(R.string.artist_photo_from_album)) }, onClick = { onDismiss(); editor.pickFromAlbum(artist) })
        }
        if (hasPhoto) {
            DropdownMenuItem(text = { Text(stringResource(R.string.artist_photo_remove)) }, onClick = { onDismiss(); vm.removeArtistPhoto(artist.name) })
        }
    }
}

@Composable
fun ArtistRow(vm: MainViewModel, artist: Artist, editor: ArtistPhotoEditor, onClick: () -> Unit) {
    val name = artist.name.ifEmpty { stringResource(R.string.unknown_artist) }
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { ArtistAvatar(vm, artist.name, 52.dp) },
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.albums_count, artist.albumCount, artist.albumCount) + " · " +
                    pluralStringResource(R.plurals.songs_count, artist.tracks.size, artist.tracks.size),
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more)) }
                ArtistPhotoMenu(vm, artist, editor, menuOpen) { menuOpen = false }
            }
        },
    )
}

/** Big avatar at the top of an artist's page; tapping it opens the photo actions. */
@Composable
fun ArtistHeader(vm: MainViewModel, artist: Artist, editor: ArtistPhotoEditor) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            ArtistAvatar(vm, artist.name, 112.dp, Modifier.clickable { menuOpen = true })
            Box(
                Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
            ArtistPhotoMenu(vm, artist, editor, menuOpen) { menuOpen = false }
        }
    }
}
