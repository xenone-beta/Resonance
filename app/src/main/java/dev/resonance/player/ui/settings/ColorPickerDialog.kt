package dev.resonance.player.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.resonance.player.R

private fun hsvToArgb(h: Float, s: Float, v: Float): Int =
    android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))

private fun toHex(argb: Int): String = "%06X".format(argb and 0xFFFFFF)

private fun parseHex(text: String): Int? {
    val t = text.trim().removePrefix("#")
    if (t.length != 6) return null
    return t.toIntOrNull(16)?.let { it or (0xFF shl 24) }
}

/**
 * Lets the user build any accent color: a saturation/brightness pad, a hue bar and a HEX field.
 * The three stay in sync; [onPick] gets the final ARGB color.
 */
@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val start = FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf(toHex(initial)) }
    val color = hsvToArgb(hue, sat, value)
    val valid = parseHex(hex) != null

    fun fromHsv(h: Float = hue, s: Float = sat, v: Float = value) {
        hue = h; sat = s; value = v
        hex = toHex(hsvToArgb(h, s, v))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SaturationValuePad(hue, sat, value, Modifier.fillMaxWidth().height(180.dp)) { s, v -> fromHsv(s = s, v = v) }
                HueBar(hue, Modifier.fillMaxWidth().height(28.dp)) { fromHsv(h = it) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Color(color)))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { raw ->
                            val cleaned = raw.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            hex = cleaned
                            parseHex(cleaned)?.let { argb ->
                                val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
                                hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                            }
                        },
                        label = { Text(stringResource(R.string.color_picker_hex)) },
                        prefix = { Text("#") },
                        singleLine = true,
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onPick(parseHex(hex) ?: color) }) {
                Text(stringResource(R.string.color_picker_apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SaturationValuePad(hue: Float, sat: Float, value: Float, modifier: Modifier, onChange: (Float, Float) -> Unit) {
    val pure = Color(hsvToArgb(hue, 1f, 1f))
    fun pick(offset: Offset, size: androidx.compose.ui.unit.IntSize) {
        onChange((offset.x / size.width).coerceIn(0f, 1f), 1f - (offset.y / size.height).coerceIn(0f, 1f))
    }
    Canvas(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) { detectTapGestures { pick(it, size) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { pick(it, size) }) { change, _ ->
                    change.consume()
                    pick(change.position, size)
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, pure)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val center = Offset(sat * size.width, (1f - value) * size.height)
        drawCircle(Color.White, radius = 11.dp.toPx(), center = center, style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.35f), radius = 13.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun HueBar(hue: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    val rainbow = remember { List(7) { Color(hsvToArgb(it * 60f, 1f, 1f)) } }
    fun pick(x: Float, width: Int) = onChange((x / width).coerceIn(0f, 1f) * 360f)
    Canvas(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(Unit) { detectTapGestures { pick(it.x, size.width) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { pick(it.x, size.width) }) { change, _ ->
                    change.consume()
                    pick(change.position.x, size.width)
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(rainbow))
        val x = hue / 360f * size.width
        val w = 6.dp.toPx()
        drawRoundRect(
            Color.White,
            topLeft = Offset((x - w / 2f).coerceIn(0f, size.width - w), 2.dp.toPx()),
            size = Size(w, size.height - 4.dp.toPx()),
            cornerRadius = CornerRadius(w / 2f),
        )
    }
}
