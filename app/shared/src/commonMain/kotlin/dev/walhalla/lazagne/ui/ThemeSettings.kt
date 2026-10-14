package dev.walhalla.lazagne.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.russhwolf.settings.ObservableSettings
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.systemBlue
import io.github.alexzhirkevich.cupertino.theme.systemGreen
import io.github.alexzhirkevich.cupertino.theme.systemIndigo
import io.github.alexzhirkevich.cupertino.theme.systemOrange
import io.github.alexzhirkevich.cupertino.theme.systemPink
import io.github.alexzhirkevich.cupertino.theme.systemPurple
import io.github.alexzhirkevich.cupertino.theme.systemRed
import io.github.alexzhirkevich.cupertino.theme.systemTeal
enum class ThemeMode(val title: String) {
    System("Как в системе"),
    Light("Светлая"),
    Dark("Тёмная"),
}

enum class AccentOption(val title: String, val color: (dark: Boolean) -> Color) {
    Blue("Синий", { CupertinoColors.systemBlue(it) }),
    Indigo("Индиго", { CupertinoColors.systemIndigo(it) }),
    Purple("Фиолетовый", { CupertinoColors.systemPurple(it) }),
    Pink("Розовый", { CupertinoColors.systemPink(it) }),
    Red("Красный", { CupertinoColors.systemRed(it) }),
    Orange("Оранжевый", { CupertinoColors.systemOrange(it) }),
    Green("Зелёный", { CupertinoColors.systemGreen(it) }),
    Teal("Бирюзовый", { CupertinoColors.systemTeal(it) }),
}

data class ThemePrefs(
    val mode: ThemeMode = ThemeMode.System,
    val accent: AccentOption = AccentOption.Blue,
)

class ThemeSettings(private val settings: ObservableSettings) {

    var prefs: ThemePrefs by mutableStateOf(read())
        private set

    fun update(transform: (ThemePrefs) -> ThemePrefs) {
        val next = transform(prefs)
        settings.putString(KEY_MODE, next.mode.name)
        settings.putString(KEY_ACCENT, next.accent.name)
        prefs = next
    }

    private fun read() = ThemePrefs(
        mode = settings.getStringOrNull(KEY_MODE)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.System,
        accent = settings.getStringOrNull(KEY_ACCENT)
            ?.let { name -> AccentOption.entries.firstOrNull { it.name == name } }
            ?: AccentOption.Blue,
    )

    companion object {
        const val KEY_MODE = "pref_theme_mode"
        const val KEY_ACCENT = "pref_theme_accent"
    }
}
