package dev.resonance.player.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.withFrameNanos
import dev.resonance.player.playback.SpectrumBus
import dev.resonance.player.ui.common.SleepTimerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.core.TimeFormat
import dev.resonance.player.ui.theme.AlbumTheme
import dev.resonance.player.ui.common.LocalHasBackground
import dev.resonance.player.ui.common.LocalNavigator

/** Full-screen player. Its whole color scheme comes from the current album cover. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit, onOpenEqualizer: () -> Unit) {
    val state by vm.player.state.collectAsState()
    val seed by vm.seed.collectAsState()
    val cover by vm.cover.collectAsState()
    val albumColors by vm.settings.albumColors.collectAsState()
    val ringOn by vm.settings.visualizer.collectAsState()
    val visStyle by vm.settings.visualizerStyle.collectAsState()
    val favorites by vm.favoriteIds.collectAsState()
    val sleep by vm.sleepTimer.state.collectAsState()
    val vinyl by vm.settings.vinylMode.collectAsState()
    val speed by vm.settings.speed.collectAsState()
    val pitch by vm.settings.pitchSemitones.collectAsState()
    val nav = LocalNavigator.current
    var menuOpen by remember { mutableStateOf(false) }
    var sleepDialog by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    val entry = state.current

    // Nothing left to show (the queue was emptied): leave the screen.
    LaunchedEffect(entry == null) { if (entry == null) onBack() }
    if (entry == null) return

    if (sleepDialog) SleepTimerDialog(vm.sleepTimer) { sleepDialog = false }

    AlbumTheme(if (albumColors) seed else null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            // Over a background photo the cover colors tint it instead of hiding it.
            color = if (LocalHasBackground.current) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        IconButton(onClick = onOpenQueue) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.queue_title))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                shape = RoundedCornerShape(28.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_edit_info)) },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                    onClick = { menuOpen = false; nav.editTrack(entry.trackId) },
                                )
                                if (entry.albumId >= 0) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_go_album)) },
                                        leadingIcon = { Icon(Icons.Rounded.Album, null) },
                                        onClick = { menuOpen = false; nav.openAlbum(entry.albumId) },
                                    )
                                }
                                if (entry.artist.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_go_artist)) },
                                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                                        onClick = { menuOpen = false; nav.openArtist(entry.artist) },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                CoverStage(
                    vm = vm,
                    durationMs = entry.durationMs,
                    cover = cover,
                    isPlaying = state.isPlaying,
                    ringOn = ringOn,
                    style = visStyle,
                    vinyl = vinyl,
                    onToggleRing = { vm.settings.setVisualizer(!ringOn) },
                    onCollapse = onBack,
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Crossfade(targetState = entry, animationSpec = tween(300), modifier = Modifier.weight(1f), label = "title") { shown ->
                    Column {
                        Text(
                            shown.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            shown.artist.ifEmpty { stringResource(R.string.unknown_artist) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // Tap the artist to open their page.
                            modifier = if (shown.artist.isNotEmpty()) {
                                Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.openArtist(shown.artist) }
                            } else Modifier,
                        )
                    }
                    }
                    val favorite = entry.trackId in favorites
                    IconButton(onClick = { vm.toggleFavorite(entry.trackId) }) {
                        Icon(
                            if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(if (favorite) R.string.menu_unfavorite else R.string.menu_favorite),
                            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                SeekBar(vm, entry.durationMs)

                Spacer(Modifier.weight(1f))
                QuickActions(
                    speed = speed,
                    pitch = pitch,
                    sleepActive = sleep.active,
                    onEqualizer = onOpenEqualizer,
                    onSpeed = { sheet = PlayerSheet.SPEED },
                    onSleep = { sleepDialog = true },
                    onLyrics = { sheet = PlayerSheet.LYRICS },
                    onMore = { sheet = PlayerSheet.MORE },
                )
                Spacer(Modifier.weight(1f))
                Controls(vm, state.isPlaying, state.shuffle, state.repeatMode)
                Spacer(Modifier.height(24.dp))
            }
            sheet?.let { current ->
                ModalBottomSheet(onDismissRequest = { sheet = null }) {
                    when (current) {
                        PlayerSheet.SPEED -> SpeedSheetContent(vm)
                        PlayerSheet.MORE -> MoreSheetContent(vm)
                        PlayerSheet.LYRICS -> LyricsSheetContent(vm) { id -> sheet = null; nav.editLyrics(id) }
                    }
                }
            }
        }
    }
}

/**
 * The cover with the spectrum ring around it. Gestures: swipe sideways for the next or previous
 * song, swipe down to close, double-tap the left or right side to skip 10 seconds, tap to toggle the ring.
 */
@Composable
private fun CoverStage(
    vm: MainViewModel,
    durationMs: Long,
    cover: Bitmap?,
    isPlaying: Boolean,
    ringOn: Boolean,
    style: Int,
    vinyl: Boolean,
    onToggleRing: () -> Unit,
    onCollapse: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var hint by remember { mutableIntStateOf(0) }
    var hintTick by remember { mutableIntStateOf(0) }

    val fraction by animateFloatAsState(if (!ringOn) 1f else if (style == 0) 0.66f else 0.76f, tween(450), label = "coverFraction")
    val corner by animateIntAsState(if ((ringOn && style == 0) || vinyl) 50 else 12, tween(450), label = "coverCorner")
    val pauseScale by animateFloatAsState(if (isPlaying) 1f else 0.94f, tween(300), label = "pauseScale")
    val ringAlpha by animateFloatAsState(if (ringOn) 1f else 0f, tween(350), label = "ringAlpha")

    // The analysis runs only while the ring is visible.
    DisposableEffect(ringOn) {
        SpectrumBus.active = ringOn
        onDispose { SpectrumBus.active = false }
    }
    val levels = remember { FloatArray(SpectrumBus.BANDS) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(ringOn, isPlaying) {
        if (!ringOn) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val target = if (isPlaying) SpectrumBus.bands.value else null
            for (i in levels.indices) {
                val t = target?.getOrNull(i) ?: 0f
                // Rise quickly, fall slowly: this is what makes a visualizer feel alive.
                levels[i] += (t - levels[i]) * (if (t > levels[i]) 0.55f else 0.12f)
            }
            frame++
        }
    }
    val vinylAngle = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vinyl, isPlaying) {
        if (!vinyl || !isPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            vinylAngle.floatValue = (vinylAngle.floatValue + (now - last) / 1_000_000_000f * 60f) % 360f
            last = now
        }
    }
    LaunchedEffect(hintTick) {
        if (hintTick > 0) {
            delay(600)
            hint = 0
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(durationMs) {
                detectTapGestures(
                    onTap = { onToggleRing() },
                    onDoubleTap = { offset ->
                        when {
                            offset.x < size.width * 0.35f -> {
                                vm.player.seekBy(-10_000L, durationMs); hint = -1; hintTick++
                            }
                            offset.x > size.width * 0.65f -> {
                                vm.player.seekBy(10_000L, durationMs); hint = 1; hintTick++
                            }
                            else -> vm.player.togglePlay()
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDragEnd = {
                        val width = size.width
                        when {
                            abs(dragX) > abs(dragY) && abs(dragX) > width * 0.22f ->
                                if (dragX < 0) vm.player.next() else vm.player.previousTrack()
                            dragY > abs(dragX) && dragY > width * 0.25f -> onCollapse()
                        }
                        scope.launch { animate(dragX, 0f) { value, _ -> dragX = value } }
                        scope.launch { animate(dragY, 0f) { value, _ -> dragY = value } }
                    },
                    onDragCancel = {
                        scope.launch { animate(dragX, 0f) { value, _ -> dragX = value } }
                        scope.launch { animate(dragY, 0f) { value, _ -> dragY = value } }
                    },
                ) { change, amount ->
                    change.consume()
                    dragX += amount.x
                    dragY = (dragY + amount.y).coerceAtLeast(0f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val tick = frame // reading it makes the visualizer redraw every frame
            if (tick < 0 || ringAlpha <= 0.01f) return@Canvas
            when (style) {
                1 -> drawBars(levels, primary, tertiary, ringAlpha)
                2 -> drawWave(levels, primary, tertiary, ringAlpha)
                else -> {
                    val bars = SpectrumBus.BANDS * 2
                    val outer = size.minDimension / 2f
                    val inner = outer * 0.66f * pauseScale + outer * 0.05f
                    val stroke = (2f * Math.PI.toFloat() * inner / bars) * 0.55f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    for (bar in 0 until bars) {
                        // Mirrored left and right, bass at the bottom.
                        val band = if (bar < SpectrumBus.BANDS) bar else bars - 1 - bar
                        val level = levels[band].coerceIn(0f, 1f)
                        val angle = Math.toRadians(90.0 + (bar + 0.5) * 360.0 / bars)
                        val dx = cos(angle).toFloat()
                        val dy = sin(angle).toFloat()
                        val length = outer * (0.03f + 0.28f * level)
                        drawLine(
                            color = lerp(primary, tertiary, level).copy(alpha = (0.35f + 0.65f * level) * ringAlpha),
                            start = Offset(center.x + dx * inner, center.y + dy * inner),
                            end = Offset(center.x + dx * (inner + length), center.y + dy * (inner + length)),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        val swipingSideways = abs(dragX) >= dragY
        val travel = max(abs(dragX), dragY)
        Box(
            modifier = Modifier
                .align(if (ringOn && style != 0) Alignment.TopCenter else Alignment.Center)
                .fillMaxSize(fraction)
                .graphicsLayer {
                    translationX = if (swipingSideways) dragX * 0.5f else 0f
                    translationY = if (swipingSideways) 0f else dragY * 0.5f
                    alpha = 1f - (travel / size.width).coerceIn(0f, 1f) * 0.6f
                    scaleX = pauseScale
                    scaleY = pauseScale
                },
            contentAlignment = Alignment.Center,
        ) {
            if (vinyl) {
                VinylDisc(cover = cover, angle = vinylAngle)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = cover, animationSpec = tween(350), label = "cover") { art ->
                        if (art != null) {
                            Image(
                                bitmap = art.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(96.dp),
                            )
                        }
                    }
                }
            }
        }

        if (hint != 0) {
            Box(
                modifier = Modifier
                    .align(if (hint < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(8.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (hint < 0) "−10" else "+10",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SeekBar(vm: MainViewModel, durationMs: Long) {
    val position by vm.player.position.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val style by vm.settings.seekStyle.collectAsState()
    val playing = playerState.isPlaying
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    // While playing, the position is read every frame, so the bar glides instead of stepping.
    var live by remember { mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(playing, playerState.currentIndex) {
        live = vm.player.currentPosition()
        while (playing) {
            androidx.compose.runtime.withFrameMillis { live = vm.player.currentPosition() }
        }
    }
    val max = durationMs.coerceAtLeast(1L).toFloat()
    val current = if (playing) live else position
    val shown = if (dragging) dragValue else current.toFloat().coerceIn(0f, max)

    Column {
        if (style == 1) {
            WavySeekBar(
                fraction = shown / max,
                playing = playing && !dragging,
                onChange = { dragging = true; dragValue = it * max },
                onFinish = { vm.player.seekTo(dragValue.toLong()); live = dragValue.toLong(); dragging = false },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Slider(
                value = shown,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = { vm.player.seekTo(dragValue.toLong()); live = dragValue.toLong(); dragging = false },
                valueRange = 0f..max,
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                TimeFormat.clock(shown.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                TimeFormat.clock(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A seek bar whose played part is a travelling wave. It calms into a straight line when paused. */
@Composable
private fun WavySeekBar(
    fraction: Float,
    playing: Boolean,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val amplitude by androidx.compose.animation.core.animateFloatAsState(if (playing) 1f else 0f, label = "amplitude")
    var phase by remember { mutableStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(playing) {
        var last = 0L
        while (playing) {
            androidx.compose.runtime.withFrameMillis { t ->
                if (last != 0L) phase = (phase + (t - last) / 1000f * 5f) % (2f * Math.PI.toFloat())
                last = t
            }
        }
    }
    val change by androidx.compose.runtime.rememberUpdatedState(onChange)
    val finish by androidx.compose.runtime.rememberUpdatedState(onFinish)
    var pressed by remember { mutableStateOf(false) }
    val thumbWidth by androidx.compose.animation.core.animateDpAsState(if (pressed) 3.dp else 5.dp, label = "thumb")

    androidx.compose.foundation.Canvas(
        modifier
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    change((offset.x / size.width).coerceIn(0f, 1f)); finish()
                })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> pressed = true; change((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { pressed = false; finish() },
                    onDragCancel = { pressed = false; finish() },
                    onHorizontalDrag = { pointer, _ ->
                        pointer.consume()
                        change((pointer.position.x / size.width).coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        val stroke = 4.dp.toPx()
        val centerY = size.height / 2f
        val inset = 8.dp.toPx()
        val width = size.width - inset * 2
        val x = inset + width * fraction.coerceIn(0f, 1f)
        val wavelength = 26.dp.toPx()
        val peak = 4.5.dp.toPx() * amplitude

        // Not yet played: a plain rounded line.
        drawLine(inactive, androidx.compose.ui.geometry.Offset(x, centerY), androidx.compose.ui.geometry.Offset(inset + width, centerY), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)

        // Played: the wave, drawn in short steps so it stays smooth.
        val path = androidx.compose.ui.graphics.Path()
        var px = inset
        path.moveTo(px, centerY + peak * kotlin.math.sin(-phase))
        while (px < x) {
            px = (px + 2.dp.toPx()).coerceAtMost(x)
            val k = ((px - inset) / wavelength) * 2f * Math.PI.toFloat() - phase
            path.lineTo(px, centerY + peak * kotlin.math.sin(k))
        }
        drawPath(path, active, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        // Thumb: a pill that thins while it is dragged.
        val half = thumbWidth.toPx() / 2f
        drawRoundRect(
            active,
            topLeft = androidx.compose.ui.geometry.Offset(x - half, centerY - 14.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(half * 2, 28.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(half),
        )
    }
}

@Composable
private fun Controls(vm: MainViewModel, isPlaying: Boolean, shuffle: Boolean, repeatMode: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(checked = shuffle, onCheckedChange = { vm.player.toggleShuffle() }) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { vm.player.previous() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), Modifier.size(36.dp))
        }
        FilledIconButton(
            onClick = { vm.player.togglePlay() },
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(if (isPlaying) 28.dp else 40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = { vm.player.next() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.next), Modifier.size(36.dp))
        }
        IconButton(onClick = { vm.player.cycleRepeat() }) {
            Icon(
                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.repeat),
                tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

/** A black record with grooves; the cover is the center label. It spins while [angle] changes. */
@Composable
private fun VinylDisc(cover: Bitmap?, angle: androidx.compose.runtime.State<Float>) {
    val vinylBlack = Color(0xFF101010)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(vinylBlack),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                for (i in 0..14) {
                    drawCircle(
                        color = Color.White.copy(alpha = if (i % 4 == 0) 0.10f else 0.05f),
                        radius = radius * (0.42f + i * 0.037f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(0.38f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val art = cover
                if (art != null) {
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = vinylBlack, radius = size.minDimension * 0.02f)
            }
        }
        // The light sheen stays still while the record turns under it.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                ),
                radius = size.minDimension / 2f,
            )
        }
    }
}

enum class PlayerSheet { SPEED, MORE, LYRICS }

@Composable
private fun QuickActions(
    speed: Float,
    pitch: Int,
    sleepActive: Boolean,
    onEqualizer: () -> Unit,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onLyrics: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        QuickButton(stringResource(R.string.eq_title), false, onEqualizer) {
            Icon(Icons.Rounded.Equalizer, null)
        }
        QuickButton(stringResource(R.string.speed_short), speed != 1f || pitch != 0, onSpeed) {
            if (speed != 1f) Text(speedLabel(speed), style = MaterialTheme.typography.labelLarge) else Icon(Icons.Rounded.Speed, null)
        }
        QuickButton(stringResource(R.string.sleep_short), sleepActive, onSleep) {
            Icon(Icons.Rounded.Bedtime, null)
        }
        QuickButton(stringResource(R.string.lyrics_title), false, onLyrics) {
            Icon(Icons.Rounded.Lyrics, null)
        }
        QuickButton(stringResource(R.string.more_short), false, onMore) {
            Icon(Icons.Rounded.Tune, null)
        }
    }
}

@Composable
private fun QuickButton(label: String, active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 36.dp)
                .clip(CircleShape)
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides
                    if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ) { icon() }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Bars along the bottom of the stage, bass on the left, treble on the right, mirrored around the middle. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(
    levels: FloatArray,
    primary: androidx.compose.ui.graphics.Color,
    tertiary: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    val count = SpectrumBus.BANDS * 2
    val gap = size.width * 0.006f
    val barWidth = (size.width - gap * (count - 1)) / count
    val baseline = size.height - 2.dp.toPx()
    val maxHeight = size.height * 0.2f
    for (i in 0 until count) {
        // Bass in the middle, treble toward both edges.
        val band = if (i < SpectrumBus.BANDS) SpectrumBus.BANDS - 1 - i else i - SpectrumBus.BANDS
        val level = levels[band].coerceIn(0f, 1f)
        val height = barWidth + (maxHeight - barWidth) * level
        drawRoundRect(
            color = lerp(primary, tertiary, level).copy(alpha = (0.4f + 0.6f * level) * alpha),
            topLeft = Offset(i * (barWidth + gap), baseline - height),
            size = androidx.compose.ui.geometry.Size(barWidth, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
        )
    }
}

/** A smooth mirrored wave under the cover that swells with the music. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    levels: FloatArray,
    primary: androidx.compose.ui.graphics.Color,
    tertiary: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    val count = SpectrumBus.BANDS * 2
    val centerY = size.height - size.height * 0.1f
    val maxAmp = size.height * 0.09f
    val step = size.width / (count - 1)
    fun amp(i: Int): Float {
        val band = if (i < SpectrumBus.BANDS) SpectrumBus.BANDS - 1 - i else i - SpectrumBus.BANDS
        return (0.04f + 0.96f * levels[band].coerceIn(0f, 1f)) * maxAmp
    }
    val top = androidx.compose.ui.graphics.Path()
    val bottom = androidx.compose.ui.graphics.Path()
    top.moveTo(0f, centerY - amp(0))
    bottom.moveTo(0f, centerY + amp(0))
    for (i in 1 until count) {
        val x = i * step
        val midX = x - step / 2f
        val prev = amp(i - 1)
        val cur = amp(i)
        top.cubicTo(midX, centerY - prev, midX, centerY - cur, x, centerY - cur)
        bottom.cubicTo(midX, centerY + prev, midX, centerY + cur, x, centerY + cur)
    }
    val fill = androidx.compose.ui.graphics.Path().apply {
        addPath(top)
        // Walk back along the lower edge to close the shape.
        lineTo(size.width, centerY + amp(count - 1))
        for (i in count - 2 downTo 0) {
            val x = i * step
            lineTo(x, centerY + amp(i))
        }
        close()
    }
    val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(primary, tertiary, primary))
    drawPath(fill, brush, alpha = 0.28f * alpha)
    val edge = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    drawPath(top, brush, alpha = alpha, style = edge)
    drawPath(bottom, brush, alpha = alpha, style = edge)
}
