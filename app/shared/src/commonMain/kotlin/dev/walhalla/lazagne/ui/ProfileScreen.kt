package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Button
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.ContentDialog
import io.github.composefluent.component.ContentDialogButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListHeader
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Briefcase
import io.github.composefluent.icons.regular.Mail
import io.github.composefluent.icons.regular.Person
import io.github.composefluent.icons.regular.PersonCircle

@Composable
fun ProfileScreen(onNotice: (String) -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }

    ScreenList {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Regular.PersonCircle,
                    contentDescription = null,
                    tint = FluentTheme.colors.fillAccent.default,
                    modifier = Modifier.size(48.dp),
                )
                Column {
                    Text("Анна Петрова", style = FluentTheme.typography.subtitle)
                    Text(
                        text = "Ведущий аналитик · Отдел продаж",
                        style = FluentTheme.typography.caption,
                        color = FluentTheme.colors.text.text.secondary,
                    )
                }
            }
        }

        item { ListHeader(content = { Text("Контакты") }) }
        item { InfoRow(Icons.Regular.Mail, "Почта", "a.petrova@walhalla.corp") }
        item { InfoRow(Icons.Regular.Briefcase, "Офис", "Москва, 7 этаж") }

        item { ListHeader(content = { Text("Руководитель") }) }
        item { InfoRow(Icons.Regular.Person, "Директор по продажам", "Иван Соколов") }

        item {
            Button(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            ) { Text("Выйти из аккаунта") }
        }
    }

    ContentDialog(
        title = "Выйти из аккаунта?",
        visible = confirmLogout,
        content = { Text("Черновики заявок на этом устройстве будут удалены.") },
        primaryButtonText = "Выйти",
        closeButtonText = "Отмена",
        onButtonClick = { button ->
            confirmLogout = false
            if (button == ContentDialogButton.Primary) onNotice("Вы вышли из аккаунта")
        },
    )
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    CardExpanderItem(
        heading = { Text(value) },
        caption = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    )
}
