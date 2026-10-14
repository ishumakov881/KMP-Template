@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.cancel
import io.github.alexzhirkevich.cupertino.destructive
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.PersonCropCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Building
import io.github.alexzhirkevich.cupertino.icons.outlined.Envelope
import io.github.alexzhirkevich.cupertino.icons.outlined.Person2
import io.github.alexzhirkevich.cupertino.icons.outlined.Phone
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemRed

@Composable
fun ProfileScreen(padding: PaddingValues) {
    var confirmLogout by remember { mutableStateOf(false) }

    ScreenList(padding) {
        section {
            item { rowPadding ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(rowPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Filled.PersonCropCircle,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.accent,
                    )
                    Column {
                        CupertinoText("Анна Петрова", style = CupertinoTheme.typography.title2)
                        CupertinoText(
                            text = "Ведущий аналитик · Отдел продаж",
                            style = CupertinoTheme.typography.subhead,
                            color = CupertinoTheme.colorScheme.secondaryLabel,
                        )
                    }
                }
            }
        }

        section(title = { CupertinoText("Контакты".sectionTitle()) }) {
            item { rowPadding -> InfoRow(rowPadding, CupertinoIcons.Default.Phone, "Телефон", "+7 900 000-00-00") }
            item { rowPadding -> InfoRow(rowPadding, CupertinoIcons.Default.Envelope, "Почта", "a.petrova@walhalla.corp") }
            item { rowPadding -> InfoRow(rowPadding, CupertinoIcons.Default.Building, "Офис", "Москва, 7 этаж") }
        }

        section(title = { CupertinoText("Руководитель".sectionTitle()) }) {
            link(
                onClick = {},
                icon = { CupertinoLinkIcon(CupertinoIcons.Default.Person2) },
            ) { TitleWithSubtitle("Иван Соколов", "Директор по продажам") }
        }

        item {
            CupertinoButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().padding(CupertinoSectionDefaults.PaddingValues),
                colors = CupertinoButtonDefaults.grayButtonColors(),
            ) {
                CupertinoText("Выйти из аккаунта", color = CupertinoColors.systemRed)
            }
        }
    }

    if (confirmLogout) {
        CupertinoAlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { CupertinoText("Выйти из аккаунта?") },
            message = { CupertinoText("Черновики заявок на этом устройстве будут удалены.") },
        ) {
            cancel(onClick = { confirmLogout = false }) { CupertinoText("Отмена") }
            destructive(onClick = { confirmLogout = false }) { CupertinoText("Выйти") }
        }
    }
}

@Composable
private fun InfoRow(padding: PaddingValues, icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CupertinoIcon(icon, contentDescription = null, tint = CupertinoTheme.colorScheme.accent)
        TitleWithSubtitle(value, label)
    }
}
