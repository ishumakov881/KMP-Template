@file:OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Badge
import io.github.composefluent.component.Button
import io.github.composefluent.component.CalendarDatePicker
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.ComboBox
import io.github.composefluent.component.ContentDialog
import io.github.composefluent.component.ContentDialogButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListHeader
import io.github.composefluent.component.MenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem
import io.github.composefluent.component.RadioButton
import io.github.composefluent.component.Slider
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextField
import io.github.composefluent.component.TooltipBox
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.MoreHorizontal
import io.github.composefluent.icons.regular.Share
import io.github.composefluent.icons.regular.Edit
import io.github.composefluent.surface.Card
import kotlin.math.roundToInt

private enum class Priority(val title: String) { Low("Низкий"), Normal("Обычный"), High("Высокий") }

@Composable
fun RequestFormScreen(
    request: CorpRequest?,
    initialType: RequestType,
    onDone: (String) -> Unit,
) {
    var subject by rememberSaveable { mutableStateOf(request?.let { "${it.type.title} — ${it.author}" } ?: "") }
    var type by remember { mutableStateOf(initialType) }
    var date by remember { mutableStateOf<String?>(null) }
    var urgent by remember { mutableStateOf(false) }
    var notifyManager by remember { mutableStateOf(true) }
    var priority by remember { mutableStateOf(Priority.Normal) }
    var budget by remember { mutableFloatStateOf(40f) }
    var comment by rememberSaveable { mutableStateOf("") }
    var confirmSubmit by remember { mutableStateOf(false) }

    ScreenList {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    if (request != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Badge(status = request.status.badge)
                            TitleWithSubtitle(request.status.title, "Автор: ${request.author}")
                        }
                    } else {
                        Text("Новая заявка", style = FluentTheme.typography.subtitle)
                    }
                }
                TooltipBox(tooltip = { Text("Ещё действия") }) {
                    MenuFlyoutContainer(
                        flyout = {
                            MenuFlyoutItem(
                                onClick = { isFlyoutVisible = false },
                                text = { Text("Дублировать") },
                                icon = { Icon(Icons.Regular.Edit, contentDescription = null) },
                            )
                            MenuFlyoutItem(
                                onClick = { isFlyoutVisible = false },
                                text = { Text("Поделиться") },
                                icon = { Icon(Icons.Regular.Share, contentDescription = null) },
                            )
                            MenuFlyoutItem(
                                onClick = {
                                    isFlyoutVisible = false
                                    onDone("Заявка отозвана")
                                },
                                text = { Text("Отозвать заявку") },
                                icon = { Icon(Icons.Regular.Delete, contentDescription = null) },
                            )
                        },
                    ) {
                        Button(onClick = { isFlyoutVisible = !isFlyoutVisible }, iconOnly = true) {
                            Icon(Icons.Regular.MoreHorizontal, contentDescription = "Ещё действия")
                        }
                    }
                }
            }
        }

        item { ListHeader(content = { Text("Основное") }) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextField(
                        value = subject,
                        onValueChange = { subject = it },
                        singleLine = true,
                        header = { Text("Тема") },
                        placeholder = { Text("Тема заявки") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ComboBox(
                        header = "Тип",
                        items = RequestType.entries.map { it.title },
                        selected = type.ordinal,
                        onSelectionChange = { index, _ -> type = RequestType.entries[index] },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Дата начала${date?.let { ": $it" } ?: ""}", style = FluentTheme.typography.body)
                    CalendarDatePicker(onChoose = { date = "${it.day}.${it.monthValue + 1}.${it.year}" })
                }
            }
        }

        item { ListHeader(content = { Text("Параметры") }) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switcher(checked = urgent, onCheckStateChange = { urgent = it }, text = "Срочно", textBefore = true)
                    CheckBox(
                        checked = notifyManager,
                        label = "Уведомить руководителя",
                        onCheckStateChange = { notifyManager = it },
                    )
                    Text("Приоритет", style = FluentTheme.typography.bodyStrong)
                    Priority.entries.forEach { option ->
                        RadioButton(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = option.title,
                        )
                    }
                    Text("Бюджет: ${budget.roundToInt()} тыс. ₽", style = FluentTheme.typography.bodyStrong)
                    Slider(
                        value = budget,
                        onValueChange = { budget = it },
                        valueRange = 0f..200f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item { ListHeader(content = { Text("Комментарий") }) }
        item {
            TextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("Пояснение для согласующего") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { onDone("Черновик сохранён") }, modifier = Modifier.weight(1f)) {
                    Text("В черновик")
                }
                AccentButton(
                    onClick = { confirmSubmit = true },
                    modifier = Modifier.weight(1f),
                    disabled = subject.isBlank(),
                ) { Text("Отправить") }
            }
        }
    }

    ContentDialog(
        title = "Отправить заявку?",
        visible = confirmSubmit,
        content = { Text("${type.title}, приоритет «${priority.title}». Заявка уйдёт на согласование руководителю.") },
        primaryButtonText = "Отправить",
        closeButtonText = "Отмена",
        onButtonClick = { button ->
            confirmSubmit = false
            if (button == ContentDialogButton.Primary) onDone("Заявка отправлена на согласование")
        },
    )
}
