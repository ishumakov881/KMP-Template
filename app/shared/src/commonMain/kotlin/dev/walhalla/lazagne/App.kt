package dev.walhalla.lazagne

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.walhalla.lazagne.ui.CorporateShell
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private const val DefaultKeyColor = 0xFF3482FFL

@Composable
@Preview
fun App() {
    var modeName by rememberSaveable { mutableStateOf(ColorSchemeMode.System.name) }
    var keyColorArgb by rememberSaveable { mutableLongStateOf(DefaultKeyColor) }

    val mode = remember(modeName) {
        runCatching { ColorSchemeMode.valueOf(modeName) }.getOrDefault(ColorSchemeMode.System)
    }
    val keyColor = Color(keyColorArgb)
    val controller = remember(mode, keyColorArgb) {
        ThemeController(
            colorSchemeMode = mode,
            keyColor = keyColor,
        )
    }

    MiuixTheme(controller = controller) {
        CorporateShell(
            themeMode = mode,
            keyColor = keyColor,
            onThemeModeChange = { modeName = it.name },
            onKeyColorChange = { color ->
                keyColorArgb = color.value.toLong() and 0xFFFFFFFFL
            },
        )
    }
}
