package dev.resonance.player.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.ui.common.Cover
import dev.resonance.player.ui.common.EmptyState
import dev.resonance.player.ui.common.NameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(vm: MainViewModel, onBack: () -> Unit) {
    val state by vm.player.state.collectAsState()
    val listState = rememberLazyListState()
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state.currentIndex > 0) listState.scrollToItem(state.currentIndex)
    }

    if (saving) {
        NameDialog(
            title = stringResource(R.string.queue_save),
            confirmLabel = stringResource(R.string.playlist_create),
            onConfirm = { name ->
                vm.createPlaylist(name, state.queue.mapNotNull { vm.trackById(it.trackId) })
                saving = false
            },
            onDismiss = { saving = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queue_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.queue.isNotEmpty()) {
                        IconButton(onClick = { saving = true }) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.queue_save))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.queue.isEmpty()) {
            EmptyState(stringResource(R.string.queue_empty), Modifier.padding(padding))
        } else {
            // Stable keys: "<track id>#<n-th occurrence>", so a row keeps its identity while it is dragged.
            val keys = remember(state.queue) {
                val seen = HashMap<Long, Int>()
                state.queue.map { entry ->
                    val n = seen.merge(entry.trackId, 1, Int::plus) ?: 1
                    "${entry.trackId}#$n"
                }
            }
            val latestKeys = rememberUpdatedState(keys)
            val latestSize = rememberUpdatedState(state.queue.size)
            var draggingKey by remember { mutableStateOf<String?>(null) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var dragIndex by remember { mutableIntStateOf(-1) }

            LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState) {
                itemsIndexed(state.queue, key = { index, _ -> keys[index] }) { index, entry ->
                    val key = keys[index]
                    val current = index == state.currentIndex
                    val dragging = draggingKey == key
                    ListItem(
                        modifier = Modifier
                            .then(if (dragging) Modifier else Modifier.animateItem())
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                            .clickable { vm.player.skipTo(index) },
                        colors = ListItemDefaults.colors(
                            containerColor = when {
                                dragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                                current -> MaterialTheme.colorScheme.secondaryContainer
                                else -> Color.Transparent
                            },
                        ),
                        leadingContent = { Cover(vm.artwork, entry.trackId, entry.albumId, 44.dp, corner = 10.dp) },
                        headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                entry.artist.ifEmpty { stringResource(R.string.unknown_artist) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.player.removeFromQueue(index) }) {
                                    Icon(Icons.Rounded.Close, stringResource(R.string.remove))
                                }
                                // Hold the handle and drag to reorder.
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = stringResource(R.string.drag_to_reorder),
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .pointerInput(key) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingKey = key
                                                    dragOffset = 0f
                                                    dragIndex = latestKeys.value.indexOf(key)
                                                },
                                                onDragEnd = { draggingKey = null; dragOffset = 0f },
                                                onDragCancel = { draggingKey = null; dragOffset = 0f },
                                            ) { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y
                                                val rowHeight = listState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull()?.size?.toFloat() ?: 160f
                                                // Swap with the neighbour once the row is half-way over it.
                                                if (dragOffset > rowHeight / 2 && dragIndex < latestSize.value - 1) {
                                                    vm.player.moveInQueue(dragIndex, dragIndex + 1)
                                                    dragIndex++
                                                    dragOffset -= rowHeight
                                                } else if (dragOffset < -rowHeight / 2 && dragIndex > 0) {
                                                    vm.player.moveInQueue(dragIndex, dragIndex - 1)
                                                    dragIndex--
                                                    dragOffset += rowHeight
                                                }
                                            }
                                        },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
