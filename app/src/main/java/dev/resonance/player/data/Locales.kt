package dev.resonance.player.data

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/** Languages the app is translated into. An empty tag means "follow the system". */
object Locales {
    val supported: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ru" to "Русский",
    )

    /** The saved choice, read straight from preferences because it is needed before the app is set up. */
    fun saved(context: Context): String =
        context.getSharedPreferences("resonance_settings", Context.MODE_PRIVATE).getString("language", "") ?: ""

    /** Returns a context whose resources use the chosen language; the original when following the system. */
    fun wrap(base: Context): Context {
        val tag = saved(base)
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
