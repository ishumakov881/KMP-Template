package dev.walhalla.lazagne.ui

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.walhalla.lazagne.getPlatform
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.RangeSliderPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun SettingsScreen(
    themeMode: ColorSchemeMode,
    keyColor: Color,
    onThemeModeChange: (ColorSchemeMode) -> Unit,
    onKeyColorChange: (Color) -> Unit,
) {
    var syncEnabled by remember { mutableStateOf(true) }
    var analytics by remember { mutableStateOf(false) }
    var localeIndex by remember { mutableIntStateOf(0) }
    var densityIndex by remember { mutableIntStateOf(1) }
    var cacheSize by remember { mutableFloatStateOf(0.35f) }
    var retention by remember { mutableStateOf(0.2f..0.7f) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showAboutOverlay by remember { mutableStateOf(false) }
    var showAboutWindow by remember { mutableStateOf(false) }
    var draftColor by remember(keyColor) { mutableStateOf(keyColor) }

    val locales = listOf("Русский", "English", "Deutsch")
    val densities = listOf(
        DropdownItem(text = "Compact"),
        DropdownItem(text = "Default"),
        DropdownItem(text = "Comfortable"),
    )
    val isDesktop = getPlatform().name.startsWith("Java")

    val modes = listOf(
        ColorSchemeMode.System to "System",
        ColorSchemeMode.Light to "Light",
        ColorSchemeMode.Dark to "Dark",
        ColorSchemeMode.MonetSystem to "Monet · System",
        ColorSchemeMode.MonetLight to "Monet · Light",
        ColorSchemeMode.MonetDark to "Monet · Dark",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        SmallTitle(
            text = "Тема",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            modes.forEach { (mode, label) ->
                RadioButtonPreference(
                    title = label,
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    summary = when (mode) {
                        ColorSchemeMode.System, ColorSchemeMode.MonetSystem -> "Следовать системе"
                        ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> "Всегда светлая"
                        ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> "Всегда тёмная"
                    },
                )
            }
            ArrowPreference(
                title = "Цвет темы (keyColor)",
                summary = "Для Monet-режимов",
                onClick = {
                    draftColor = keyColor
                    showColorDialog = true
                },
                holdDownState = showColorDialog,
            )
        }

        SmallTitle(
            text = "Приложение",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            SwitchPreference(
                title = "Фоновая синхронизация",
                summary = "Обновлять заявки автоматически",
                checked = syncEnabled,
                onCheckedChange = { syncEnabled = it },
            )
            CheckboxPreference(
                title = "Анонимная аналитика",
                summary = "Только служебные метрики",
                checked = analytics,
                onCheckedChange = { analytics = it },
            )
            OverlayDropdownPreference(
                title = "Язык интерфейса",
                items = locales,
                selectedIndex = localeIndex,
                onSelectedIndexChange = { localeIndex = it },
            )
            OverlaySpinnerPreference(
                title = "Плотность списка",
                items = densities,
                selectedIndex = densityIndex,
                onSelectedIndexChange = { densityIndex = it },
            )
            SliderPreference(
                title = "Кэш",
                summary = "${(cacheSize * 100).toInt()}%",
                value = cacheSize,
                onValueChange = { cacheSize = it },
            )
            RangeSliderPreference(
                title = "Хранение архива",
                summary = "${(retention.start * 12).toInt()}–${(retention.endInclusive * 12).toInt()} мес.",
                value = retention,
                onValueChange = { retention = it },
            )
            ArrowPreference(
                title = "О приложении",
                summary = if (isDesktop) "WindowDialog (desktop)" else "OverlayDialog",
                onClick = {
                    if (isDesktop) showAboutWindow = true else showAboutOverlay = true
                },
                holdDownState = showAboutOverlay || showAboutWindow,
            )
        }
    }

    OverlayDialog(
        title = "Цвет темы",
        summary = "Задаёт keyColor для Monet",
        show = showColorDialog,
        onDismissRequest = { showColorDialog = false },
    ) {
        ColorPalette(
            color = draftColor,
            onColorChanged = { draftColor = it },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = "Отмена",
                onClick = { showColorDialog = false },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = "Применить",
                onClick = {
                    onKeyColorChange(draftColor)
                    showColorDialog = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    OverlayDialog(
        title = "CorpDesk",
        summary = "Демо корпоративной оболочки на Miuix ${getPlatform().name}",
        show = showAboutOverlay,
        onDismissRequest = { showAboutOverlay = false },
    ) {
        TextButton(
            text = "Закрыть",
            onClick = { showAboutOverlay = false },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (isDesktop) {
        WindowDialog(
            title = "CorpDesk",
            summary = "Короткий desktop WindowDialog · Miuix",
            show = showAboutWindow,
            onDismissRequest = { showAboutWindow = false },
        ) {
            TextButton(
                text = "Закрыть",
                onClick = { showAboutWindow = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
