package dev.walhalla.lazagne.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.russhwolf.settings.ObservableSettings

enum class ThemeMode(val title: String) {
    System("Как в системе"),
    Light("Светлая"),
    Dark("Тёмная"),
}

val DefaultAccent = Color(0xFF0078D4)

data class ThemePrefs(
    val mode: ThemeMode = ThemeMode.System,
    val accent: Color = DefaultAccent,
)

class ThemeSettings(private val settings: ObservableSettings) {

    var prefs: ThemePrefs by mutableStateOf(read())
        private set

    fun update(transform: (ThemePrefs) -> ThemePrefs) {
        val next = transform(prefs)
        settings.putString(KEY_MODE, next.mode.name)
        settings.putInt(KEY_ACCENT, next.accent.toArgb())
        prefs = next
    }

    private fun read() = ThemePrefs(
        mode = settings.getStringOrNull(KEY_MODE)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.System,
        accent = settings.getIntOrNull(KEY_ACCENT)?.let { Color(it) } ?: DefaultAccent,
    )

    companion object {
        const val KEY_MODE = "pref_theme_mode"
        const val KEY_ACCENT = "pref_theme_accent_argb"
    }
}
