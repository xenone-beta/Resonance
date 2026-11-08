package dev.resonance.player.ui.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.core.TimeFormat
import dev.resonance.player.data.ACCENT_SEEDS
import dev.resonance.player.data.ColorSource
import dev.resonance.player.data.ThemeMode
import dev.resonance.player.ui.common.SleepTimerDialog
import dev.resonance.player.ui.common.rememberSleepRemaining

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun speedLabel(value: Float): String =
    if (value % 1f == 0f) "${value.toInt()}×" else "$value×"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings = vm.settings
    val themeMode by settings.themeMode.collectAsState()
    val colorSource by settings.colorSource.collectAsState()
    val accent by settings.accent.collectAsState()
    val albumColors by settings.albumColors.collectAsState()
    val visualizer by settings.visualizer.collectAsState()
    val speed by settings.speed.collectAsState()
    val skipSilence by settings.skipSilence.collectAsState()
    var sleepDialog by remember { mutableStateOf(false) }
    val remaining = rememberSleepRemaining(vm.sleepTimer)
    val sleepState by vm.sleepTimer.state.collectAsState()

    if (sleepDialog) SleepTimerDialog(vm.sleepTimer) { sleepDialog = false }

    val wallpaperAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_appearance))

            Label(stringResource(R.string.settings_theme))
            FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                    ThemeMode.AMOLED to R.string.theme_amoled,
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { settings.setThemeMode(mode) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            if (wallpaperAvailable) {
                Label(stringResource(R.string.settings_color_source))
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = colorSource == ColorSource.WALLPAPER,
                        onClick = { settings.setColorSource(ColorSource.WALLPAPER) },
                        label = { Text(stringResource(R.string.source_wallpaper)) },
                    )
                    FilterChip(
                        selected = colorSource == ColorSource.ACCENT,
                        onClick = { settings.setColorSource(ColorSource.ACCENT) },
                        label = { Text(stringResource(R.string.source_accent)) },
                    )
                }
            }
            if (!wallpaperAvailable || colorSource == ColorSource.ACCENT) {
                Label(stringResource(R.string.settings_accent))
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ACCENT_SEEDS.forEach { seed ->
                        val selected = seed == accent
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(seed))
                                .then(
                                    if (selected) {
                                        Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { settings.setAccent(seed) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White)
                        }
                    }
                }
            }

            SwitchRow(
                title = stringResource(R.string.settings_album_colors),
                subtitle = stringResource(R.string.settings_album_colors_hint),
                checked = albumColors,
                onChange = settings::setAlbumColors,
            )

            SectionHeader(stringResource(R.string.settings_playback))
            SwitchRow(
                title = stringResource(R.string.settings_visualizer),
                subtitle = stringResource(R.string.settings_visualizer_hint),
                checked = visualizer,
                onChange = settings::setVisualizer,
            )

            Label(stringResource(R.string.settings_speed))
            FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SPEEDS.forEach { value ->
                    FilterChip(
                        selected = speed == value,
                        onClick = { settings.setSpeed(value) },
                        label = { Text(speedLabel(value)) },
                    )
                }
            }

            SwitchRow(
                title = stringResource(R.string.settings_skip_silence),
                subtitle = stringResource(R.string.settings_skip_silence_hint),
                checked = skipSilence,
                onChange = settings::setSkipSilence,
            )

            ListItem(
                modifier = Modifier.clickable { sleepDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(stringResource(R.string.sleep_title)) },
                supportingContent = {
                    Text(
                        when {
                            remaining != null -> stringResource(R.string.sleep_left, TimeFormat.clock(remaining))
                            sleepState.endOfTrack -> stringResource(R.string.sleep_after_track_active)
                            else -> stringResource(R.string.sleep_off)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}
