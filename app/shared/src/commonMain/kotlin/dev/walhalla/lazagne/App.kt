package dev.walhalla.lazagne

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import dev.walhalla.lazagne.ui.CorporateShell
import dev.walhalla.lazagne.ui.ThemeMode
import dev.walhalla.lazagne.ui.ThemePrefs
import dev.walhalla.lazagne.ui.ThemeSettings
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors

@OptIn(ExperimentalFluentApi::class)
@Composable
@Preview
fun App(themeSettings: ThemeSettings? = null) {
    var sessionPrefs by remember { mutableStateOf(ThemePrefs()) }
    val prefs = themeSettings?.prefs ?: sessionPrefs
    val isDark = when (prefs.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = remember(isDark, prefs.accent) {
        if (isDark) darkColors(prefs.accent) else lightColors(prefs.accent)
    }

    FluentTheme(colors = colors, compactMode = false) {
        CorporateShell(
            themePrefs = prefs,
            isDark = isDark,
            onThemePrefsChange = { transform ->
                if (themeSettings != null) themeSettings.update(transform) else sessionPrefs = transform(sessionPrefs)
            },
        )
    }
}
