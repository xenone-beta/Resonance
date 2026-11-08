package dev.resonance.player.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.resonance.player.MainActivity
import dev.resonance.player.R
import dev.resonance.player.playback.NowPlayingSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CommandKey = ActionParameters.Key<String>("command")

/** Home screen widget: cover, title, artist and previous / play-pause / next buttons. */
class NowPlayingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val title = NowPlayingSnapshot.title
        val artist = NowPlayingSnapshot.artist
        val playing = NowPlayingSnapshot.isPlaying
        val cover = NowPlayingSnapshot.cover

        provideContent {
            GlanceTheme {
                val ctx = LocalContext.current
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cover != null) {
                        Image(
                            provider = ImageProvider(cover),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.size(80.dp).cornerRadius(16.dp),
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_note),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                            modifier = GlanceModifier.size(80.dp),
                        )
                    }
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = title.ifEmpty { ctx.getString(R.string.widget_nothing) },
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = artist.ifEmpty { ctx.getString(R.string.widget_open) },
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        )
                        Spacer(GlanceModifier.size(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ControlButton(R.drawable.ic_widget_previous, "previous")
                            Spacer(GlanceModifier.width(14.dp))
                            ControlButton(
                                if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                                "toggle",
                            )
                            Spacer(GlanceModifier.width(14.dp))
                            ControlButton(R.drawable.ic_widget_next, "next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(icon: Int, command: String) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = command,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
        modifier = GlanceModifier
            .size(36.dp)
            .clickable(actionRunCallback<MediaCommandAction>(actionParametersOf(CommandKey to command))),
    )
}

/** Runs a button press on the player, if the playback service is alive. Otherwise the tap does nothing. */
class MediaCommandAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val command = parameters[CommandKey] ?: return
        withContext(Dispatchers.Main) {
            val player = NowPlayingSnapshot.player ?: return@withContext
            when (command) {
                "previous" -> player.seekToPreviousMediaItem()
                "next" -> player.seekToNextMediaItem()
                else -> if (player.isPlaying) player.pause() else player.play()
            }
        }
    }
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}
