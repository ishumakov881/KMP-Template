package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Button
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.ColorPicker
import io.github.composefluent.component.ComboBox
import io.github.composefluent.component.Expander
import io.github.composefluent.component.ExpanderItem
import io.github.composefluent.component.ExpanderItemSeparator
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListHeader
import io.github.composefluent.component.RadioButton
import io.github.composefluent.component.Slider
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Alert
import io.github.composefluent.icons.regular.ChevronRight
import io.github.composefluent.icons.regular.ColorBackground
import io.github.composefluent.icons.regular.Info
import io.github.composefluent.icons.regular.Mail
import io.github.composefluent.icons.regular.Clock
import kotlin.math.roundToInt

private enum class Digest(val title: String) { Daily("Ежедневно"), Weekly("Раз в неделю"), Never("Никогда") }

@Composable
fun SettingsScreen(
    themePrefs: ThemePrefs,
    isDark: Boolean,
    onThemePrefsChange: ((ThemePrefs) -> ThemePrefs) -> Unit,
) {
    var themeExpanded by remember { mutableStateOf(true) }
    var accentExpanded by remember { mutableStateOf(false) }
    var emailNotifications by remember { mutableStateOf(true) }
    var digest by remember { mutableStateOf(Digest.Daily) }
    var volume by remember { mutableFloatStateOf(0.6f) }
    val uriHandler = LocalUriHandler.current

    ScreenList {
        item { ListHeader(content = { Text("Оформление") }) }
        item {
            Expander(
                expanded = themeExpanded,
                onExpandedChanged = { themeExpanded = it },
                heading = { Text("Тема") },
                caption = { Text("${themePrefs.mode.title} · сейчас ${if (isDark) "тёмная" else "светлая"}") },
                icon = { Icon(Icons.Regular.ColorBackground, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) ExpanderItemSeparator()
                    ExpanderItem(
                        heading = {
                            RadioButton(
                                selected = themePrefs.mode == mode,
                                onClick = { onThemePrefsChange { it.copy(mode = mode) } },
                                label = mode.title,
                            )
                        },
                    )
                }
            }
        }
        item {
            Expander(
                expanded = accentExpanded,
                onExpandedChanged = { accentExpanded = it },
                heading = { Text("Цвет акцента") },
                caption = { Text("Сохраняется между запусками") },
                icon = { Icon(Icons.Regular.ColorBackground, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColorPicker(
                        color = themePrefs.accent,
                        onSelectedColorChanged = { color -> onThemePrefsChange { it.copy(accent = color) } },
                    )
                    Button(onClick = { onThemePrefsChange { it.copy(accent = DefaultAccent) } }) {
                        Text("Сбросить")
                    }
                }
            }
        }

        item { ListHeader(content = { Text("Уведомления") }) }
        item {
            CardExpanderItem(
                heading = { Text("Письма на почту") },
                caption = { Text("Статусы ваших заявок") },
                icon = { Icon(Icons.Regular.Mail, contentDescription = null) },
                trailing = {
                    CheckBox(checked = emailNotifications, onCheckStateChange = { emailNotifications = it })
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
        }
        item {
            CardExpanderItem(
                heading = { Text("Дайджест") },
                caption = { Text("Сводка по заявкам отдела") },
                icon = { Icon(Icons.Regular.Clock, contentDescription = null) },
                trailing = {
                    ComboBox(
                        items = Digest.entries.map { it.title },
                        selected = digest.ordinal,
                        onSelectionChange = { index, _ -> digest = Digest.entries[index] },
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
        }
        item {
            CardExpanderItem(
                heading = { Text("Громкость") },
                caption = { Text("${(volume * 100).roundToInt()}%") },
                icon = { Icon(Icons.Regular.Alert, contentDescription = null) },
                trailing = {
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        modifier = Modifier.width(140.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
        }

        item { ListHeader(content = { Text("О приложении") }) }
        item {
            CardExpanderItem(
                onClick = { uriHandler.openUri("https://github.com/compose-fluent/compose-fluent-ui") },
                heading = { Text("Политика конфиденциальности") },
                caption = { Text(AppName) },
                icon = { Icon(Icons.Regular.Info, contentDescription = null) },
                trailing = { Icon(Icons.Regular.ChevronRight, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
        }
    }
}
