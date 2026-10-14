@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoCheckBox
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.MenuPickerAction
import io.github.alexzhirkevich.cupertino.default
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.Bell
import io.github.alexzhirkevich.cupertino.icons.outlined.Envelope
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.Moon
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintbrush
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.InfoCircle
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.dropdownMenu
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.section.switch
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemGray
import io.github.alexzhirkevich.cupertino.theme.systemGreen
import io.github.alexzhirkevich.cupertino.theme.systemIndigo
import io.github.alexzhirkevich.cupertino.theme.systemOrange
import io.github.alexzhirkevich.cupertino.theme.systemRed
import kotlin.math.roundToInt

private enum class Digest(val title: String) { Daily("Ежедневно"), Weekly("Раз в неделю"), Never("Никогда") }

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    themePrefs: ThemePrefs,
    isDark: Boolean,
    onThemePrefsChange: ((ThemePrefs) -> ThemePrefs) -> Unit,
) {
    var accentMenuExpanded by remember { mutableStateOf(false) }
    var digestMenuExpanded by remember { mutableStateOf(false) }
    var digest by remember { mutableStateOf(Digest.Daily) }
    var emailNotifications by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var volume by remember { mutableFloatStateOf(0.6f) }
    var showPolicy by remember { mutableStateOf(false) }

    val followSystem = themePrefs.mode == ThemeMode.System

    ScreenList(padding) {
        section(
            title = { CupertinoText("Оформление".sectionTitle()) },
            caption = { CupertinoText("Тема и акцентный цвет сохраняются между запусками") },
        ) {
            switch(
                checked = followSystem,
                onCheckedChange = { system ->
                    onThemePrefsChange {
                        it.copy(
                            mode = when {
                                system -> ThemeMode.System
                                isDark -> ThemeMode.Dark
                                else -> ThemeMode.Light
                            }
                        )
                    }
                },
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.Paintbrush, containerColor = CupertinoColors.systemIndigo) },
            ) { CupertinoText(ThemeMode.System.title) }
            switch(
                checked = isDark,
                enabled = !followSystem,
                onCheckedChange = { dark ->
                    onThemePrefsChange { it.copy(mode = if (dark) ThemeMode.Dark else ThemeMode.Light) }
                },
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.Moon, containerColor = CupertinoColors.systemGray) },
            ) { CupertinoText("Тёмная тема") }
            dropdownMenu(
                expanded = accentMenuExpanded,
                onDismissRequest = { accentMenuExpanded = false },
                onClick = { accentMenuExpanded = true },
                icon = {
                    CupertinoLinkIcon(
                        imageVector = CupertinoIcons.Default.Paintpalette,
                        containerColor = CupertinoTheme.colorScheme.accent,
                    )
                },
                selectedLabel = { CupertinoText(themePrefs.accent.title) },
                title = { CupertinoText("Акцентный цвет") },
            ) {
                AccentOption.entries.forEach { option ->
                    MenuPickerAction(
                        isSelected = option == themePrefs.accent,
                        onClick = {
                            onThemePrefsChange { it.copy(accent = option) }
                            accentMenuExpanded = false
                        },
                        icon = {
                            CupertinoIcon(
                                imageVector = CupertinoIcons.Filled.Paintpalette,
                                contentDescription = null,
                                tint = option.color(isDark),
                            )
                        },
                    ) { CupertinoText(option.title) }
                }
            }
        }

        section(title = { CupertinoText("Уведомления".sectionTitle()) }) {
            item { rowPadding ->
                TrailingControlRow(rowPadding, "Письма на почту") {
                    CupertinoCheckBox(checked = emailNotifications, onCheckedChange = { emailNotifications = it })
                }
            }
            switch(
                checked = pushNotifications,
                onCheckedChange = { pushNotifications = it },
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.Bell, containerColor = CupertinoColors.systemRed) },
            ) { CupertinoText("Push-уведомления") }
            dropdownMenu(
                expanded = digestMenuExpanded,
                onDismissRequest = { digestMenuExpanded = false },
                onClick = { digestMenuExpanded = true },
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.Envelope, containerColor = CupertinoColors.systemGreen) },
                selectedLabel = { CupertinoText(digest.title) },
                title = { CupertinoText("Дайджест") },
            ) {
                Digest.entries.forEach { option ->
                    MenuPickerAction(
                        isSelected = option == digest,
                        onClick = {
                            digest = option
                            digestMenuExpanded = false
                        },
                    ) { CupertinoText(option.title) }
                }
            }
            item { rowPadding ->
                LabeledControlRow(rowPadding, "Громкость сигнала", "${(volume * 100).roundToInt()}%") {
                    CupertinoSlider(value = volume, onValueChange = { volume = it })
                }
            }
        }

        section(title = { CupertinoText("О приложении".sectionTitle()) }) {
            link(
                onClick = { showPolicy = true },
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.Lock, containerColor = CupertinoColors.systemOrange) },
            ) { CupertinoText("Политика конфиденциальности") }
            link(
                onClick = {},
                enabled = false,
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.InfoCircle, containerColor = CupertinoColors.systemGray) },
                caption = { CupertinoText("1.0.0") },
                trailingIcon = {},
            ) { CupertinoText("Версия") }
        }
    }

    if (showPolicy) {
        CupertinoAlertDialog(
            onDismissRequest = { showPolicy = false },
            title = { CupertinoText("Политика конфиденциальности") },
            message = { CupertinoText("Демо-приложение не собирает персональные данные.") },
        ) {
            default(onClick = { showPolicy = false }) { CupertinoText("Понятно") }
        }
    }
}
