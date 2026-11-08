package dev.resonance.player.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.core.Streaks
import dev.resonance.player.core.TimeFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import dev.resonance.player.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val plays by vm.totalPlays.collectAsState()
    val totalMs by vm.totalMs.collectAsState()
    val topTracks by vm.topTracks.collectAsState()
    val topArtists by vm.topArtists.collectAsState()
    val recent by vm.recent.collectAsState()
    val daily by vm.dailyStats.collectAsState()
    val dayMap = remember(daily) { daily.associate { it.day to it.totalMs } }
    val activeDays = remember(daily) { daily.filter { it.totalMs > 0 }.map { it.day }.toSet() }
    val today = remember { LocalDate.now().toEpochDay() }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.stats_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = { vm.clearStats(); confirmClear = false }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (plays > 0 || totalMs > 0) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.stats_clear))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (plays == 0 && totalMs == 0L) {
            EmptyState(stringResource(R.string.stats_empty), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "totals") {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TotalCard(plays.toString(), stringResource(R.string.stats_plays), Modifier.weight(1f))
                    TotalCard(durationLabel(totalMs), stringResource(R.string.stats_time), Modifier.weight(1f))
                }
            }
            item(key = "streaks") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val current = Streaks.current(activeDays, today)
                    val best = Streaks.longest(activeDays)
                    TotalCard(
                        pluralStringResource(R.plurals.days_count, current, current),
                        stringResource(R.string.stats_streak),
                        Modifier.weight(1f),
                    )
                    TotalCard(
                        pluralStringResource(R.plurals.days_count, best, best),
                        stringResource(R.string.stats_streak_best),
                        Modifier.weight(1f),
                    )
                }
            }
            item(key = "chart") {
                SectionTitle(stringResource(R.string.stats_last_days))
                DayChart(dayMap, today)
            }
            if (topTracks.isNotEmpty()) {
                item(key = "h-tracks") { SectionTitle(stringResource(R.string.stats_top_tracks)) }
                items(topTracks, key = { "t${it.trackId}" }) { stat ->
                    StatRow(stat.title, stat.artist, pluralStringResource(R.plurals.plays_count, stat.playCount, stat.playCount))
                }
            }
            if (topArtists.isNotEmpty()) {
                item(key = "h-artists") { SectionTitle(stringResource(R.string.stats_top_artists)) }
                items(topArtists, key = { "a${it.artist}" }) { stat ->
                    StatRow(stat.artist, durationLabel(stat.totalMs), pluralStringResource(R.plurals.plays_count, stat.plays, stat.plays))
                }
            }
            if (recent.isNotEmpty()) {
                item(key = "h-recent") { SectionTitle(stringResource(R.string.stats_recent)) }
                items(recent, key = { "r${it.trackId}" }) { stat -> StatRow(stat.title, stat.artist, null) }
            }
        }
    }
}

@Composable
private fun durationLabel(ms: Long): String {
    val (hours, minutes) = TimeFormat.hoursAndMinutes(ms)
    return if (hours > 0) stringResource(R.string.duration_hm, hours, minutes) else stringResource(R.string.duration_m, minutes)
}

@Composable
private fun TotalCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun StatRow(title: String, subtitle: String, trailing: String?) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (subtitle.isNotEmpty()) {
            { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        trailingContent = if (trailing != null) {
            { Text(trailing, style = MaterialTheme.typography.labelLarge) }
        } else null,
    )
}

/** Bars for the last 14 days, today on the right. Height is minutes listened. */
@Composable
private fun DayChart(minutesByDay: Map<Long, Long>, today: Long) {
    val days = (today - 13..today).toList()
    val values = days.map { (minutesByDay[it] ?: 0L) / 60_000f }
    val peak = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.stats_peak, peak.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
                val slot = size.width / days.size
                val barWidth = slot * 0.62f
                values.forEachIndexed { index, minutes ->
                    val barHeight = if (minutes <= 0f) 4.dp.toPx() else (size.height * (minutes / peak)).coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        color = if (minutes <= 0f) emptyColor else barColor,
                        topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                days.forEach { day ->
                    val label = LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
