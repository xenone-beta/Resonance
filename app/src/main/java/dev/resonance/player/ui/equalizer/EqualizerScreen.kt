package dev.resonance.player.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.resonance.player.MainViewModel
import dev.resonance.player.R
import dev.resonance.player.ui.common.screenColor
import dev.resonance.player.ui.common.transparentTopBarColors
import dev.resonance.player.ui.common.barColor
import dev.resonance.player.ui.common.EmptyState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val fx by vm.effects.state.collectAsState()

    Scaffold(
        containerColor = screenColor(),
        topBar = {
            TopAppBar(
                colors = transparentTopBarColors(),
                title = { Text(stringResource(R.string.eq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (!fx.available) {
            EmptyState(stringResource(R.string.eq_unavailable), Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.eq_enabled),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = fx.enabled, onCheckedChange = { vm.effects.setEnabled(it) })
            }

            if (fx.presets.isNotEmpty()) {
                Text(
                    stringResource(R.string.eq_preset),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fx.presets.forEachIndexed { index, name ->
                        FilterChip(
                            selected = fx.preset == index,
                            onClick = { vm.effects.usePreset(index) },
                            label = { Text(name) },
                        )
                    }
                    FilterChip(
                        selected = fx.preset == -1,
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.eq_custom)) },
                    )
                }
            }

            fx.bandFrequenciesHz.forEachIndexed { band, hz ->
                val level = fx.levels.getOrElse(band) { 0 }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        frequencyLabel(hz),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(64.dp),
                    )
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { vm.effects.setBandLevel(band, it.toInt()) },
                        valueRange = fx.minLevel.toFloat()..fx.maxLevel.toFloat(),
                        enabled = fx.enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        String.format(Locale.ROOT, "%+.1f", level / 100f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(48.dp),
                    )
                }
            }

            if (fx.bassSupported) {
                EffectSlider(stringResource(R.string.eq_bass), fx.bass, fx.enabled) { vm.effects.setBass(it) }
            }
            if (fx.virtualizerSupported) {
                EffectSlider(stringResource(R.string.eq_virtualizer), fx.virtualizer, fx.enabled) {
                    vm.effects.setVirtualizer(it)
                }
            }

            OutlinedButton(
                onClick = { vm.effects.reset() },
                modifier = Modifier.padding(vertical = 12.dp),
            ) { Text(stringResource(R.string.eq_reset)) }
        }
    }
}

@Composable
private fun EffectSlider(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = enabled,
        )
    }
}

private fun frequencyLabel(hz: Int): String =
    if (hz >= 1000) String.format(Locale.ROOT, "%.1f kHz", hz / 1000f).replace(".0 ", " ") else "$hz Hz"
