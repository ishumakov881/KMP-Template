@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoCheckBox
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoRangeSlider
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoWheelPicker
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.MenuPickerAction
import io.github.alexzhirkevich.cupertino.cancel
import io.github.alexzhirkevich.cupertino.default
import io.github.alexzhirkevich.cupertino.rememberCupertinoDatePickerState
import io.github.alexzhirkevich.cupertino.rememberCupertinoPickerState
import io.github.alexzhirkevich.cupertino.rememberCupertinoTimePickerState
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.datePicker
import io.github.alexzhirkevich.cupertino.section.dropdownMenu
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.section.switch
import io.github.alexzhirkevich.cupertino.section.textField
import io.github.alexzhirkevich.cupertino.section.timePicker
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlin.math.roundToInt

private val DayOptions = (1..28).toList()

@Composable
fun RequestFormScreen(
    padding: PaddingValues,
    request: CorpRequest?,
    initialType: RequestType,
    onSubmitted: () -> Unit,
) {
    var subject by rememberSaveable { mutableStateOf(request?.let { "${it.type.title} — ${it.author}" } ?: "") }
    var type by remember { mutableStateOf(initialType) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val dateState = rememberCupertinoDatePickerState()
    var dateExpanded by remember { mutableStateOf(false) }
    val timeState = rememberCupertinoTimePickerState(initialHour = 9, initialMinute = 30, is24Hour = true)
    var timeExpanded by remember { mutableStateOf(false) }
    val daysState = rememberCupertinoPickerState(infinite = false, initiallySelectedItemIndex = 6)
    var urgent by remember { mutableStateOf(false) }
    var notifyManager by remember { mutableStateOf(true) }
    var budget by remember { mutableFloatStateOf(40f) }
    var hours by remember { mutableStateOf(9f..18f) }
    var comment by rememberSaveable { mutableStateOf("") }
    var confirmSubmit by remember { mutableStateOf(false) }

    val days = DayOptions[daysState.selectedItemIndex(DayOptions.size)]

    ScreenList(padding) {
        if (request != null) {
            section(title = { CupertinoText("Статус".sectionTitle()) }) {
                item { rowPadding ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(rowPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusIcon(request.status)
                        TitleWithSubtitle(request.status.title, "Автор: ${request.author}")
                    }
                }
            }
        }

        section(title = { CupertinoText("Основное".sectionTitle()) }) {
            textField(
                value = subject,
                onValueChange = { subject = it },
                singleLine = true,
                placeholder = { CupertinoText("Тема заявки") },
            )
            dropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
                onClick = { typeMenuExpanded = true },
                selectedLabel = { CupertinoText(type.title) },
                title = { CupertinoText("Тип") },
            ) {
                RequestType.entries.forEach { option ->
                    MenuPickerAction(
                        isSelected = option == type,
                        onClick = {
                            type = option
                            typeMenuExpanded = false
                        },
                        icon = { CupertinoIcon(option.icon, contentDescription = null) },
                    ) { CupertinoText(option.title) }
                }
            }
            datePicker(
                state = dateState,
                expanded = dateExpanded,
                onExpandedChange = { dateExpanded = it },
                title = { CupertinoText("Дата начала") },
            )
            timePicker(
                state = timeState,
                expanded = timeExpanded,
                onExpandedChange = { timeExpanded = it },
                title = { CupertinoText("Время") },
            )
            item { rowPadding ->
                LabeledControlRow(rowPadding, "Количество дней", "$days") {
                    CupertinoWheelPicker(
                        state = daysState,
                        items = DayOptions,
                        modifier = Modifier.fillMaxWidth(),
                    ) { CupertinoText("$it") }
                }
            }
        }

        section(title = { CupertinoText("Параметры".sectionTitle()) }) {
            switch(
                checked = urgent,
                onCheckedChange = { urgent = it },
            ) { CupertinoText("Срочно") }
            item { rowPadding ->
                TrailingControlRow(rowPadding, "Уведомить руководителя") {
                    CupertinoCheckBox(checked = notifyManager, onCheckedChange = { notifyManager = it })
                }
            }
            item { rowPadding ->
                LabeledControlRow(rowPadding, "Бюджет", "${budget.roundToInt()} тыс. ₽") {
                    CupertinoSlider(
                        value = budget,
                        onValueChange = { budget = it },
                        valueRange = 0f..200f,
                    )
                }
            }
            item { rowPadding ->
                LabeledControlRow(
                    rowPadding,
                    "Рабочие часы",
                    "${hours.start.roundToInt()}:00–${hours.endInclusive.roundToInt()}:00",
                ) {
                    CupertinoRangeSlider(
                        value = hours,
                        onValueChange = { hours = it },
                        valueRange = 8f..20f,
                        steps = 11,
                    )
                }
            }
        }

        section(title = { CupertinoText("Комментарий".sectionTitle()) }) {
            textField(
                value = comment,
                onValueChange = { comment = it },
                minLines = 3,
                placeholder = { CupertinoText("Пояснение для согласующего") },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(CupertinoSectionDefaults.PaddingValues),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CupertinoButton(
                    onClick = onSubmitted,
                    modifier = Modifier.weight(1f),
                    colors = CupertinoButtonDefaults.tintedButtonColors(),
                ) { CupertinoText("В черновик") }
                CupertinoButton(
                    onClick = { confirmSubmit = true },
                    modifier = Modifier.weight(1f),
                    enabled = subject.isNotBlank(),
                ) { CupertinoText("Отправить") }
            }
        }
    }

    if (confirmSubmit) {
        CupertinoAlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { CupertinoText("Отправить заявку?") },
            message = {
                CupertinoText(
                    text = "${type.title}, $days дн. Заявка уйдёт на согласование руководителю.",
                    style = CupertinoTheme.typography.footnote,
                )
            },
        ) {
            cancel(onClick = { confirmSubmit = false }) { CupertinoText("Отмена") }
            default(onClick = {
                confirmSubmit = false
                onSubmitted()
            }) { CupertinoText("Отправить") }
        }
    }
}
