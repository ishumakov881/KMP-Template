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
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.darkColorScheme
import io.github.alexzhirkevich.cupertino.theme.lightColorScheme

@Composable
@Preview
fun App(themeSettings: ThemeSettings? = null) {
    var sessionPrefs by remember { mutableStateOf(ThemePrefs()) }
    val prefs = themeSettings?.prefs ?: sessionPrefs

    val dark = when (prefs.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val accent = prefs.accent.color(dark)
    val colorScheme = if (dark) darkColorScheme(accent = accent) else lightColorScheme(accent = accent)

    CupertinoTheme(colorScheme = colorScheme) {
        CorporateShell(
            themePrefs = prefs,
            isDark = dark,
            onThemePrefsChange = { transform ->
                if (themeSettings != null) themeSettings.update(transform)
                else sessionPrefs = transform(sessionPrefs)
            },
        )
    }
}
