package dev.walhalla.lazagne.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RequestFormScreen(
    tabLabel: String,
    snackbarHostState: SnackbarHostState,
) {
    RequestEditor(
        titleHint = if (tabLabel == "Черновики") "Черновик заявки" else "Новая заявка",
        snackbarHostState = snackbarHostState,
        onSaved = {
            // stays on form; snackbar confirms
        },
    )
}

@Composable
fun RequestDetailScreen(
    requestId: String,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
) {
    RequestEditor(
        titleHint = "Заявка $requestId",
        initialTitle = when (requestId) {
            "A-104" -> "Доступ в VPN"
            "A-105" -> "Замена ноутбука"
            else -> "Заявка $requestId"
        },
        snackbarHostState = snackbarHostState,
        onSaved = onSaved,
    )
}

@Composable
private fun RequestEditor(
    titleHint: String,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    initialTitle: String = "",
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var urgent by remember { mutableStateOf(false) }
    var notifyEmail by remember { mutableStateOf(true) }
    var notifyPush by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf("Средний") }
    var effort by remember { mutableFloatStateOf(0.4f) }
    var days by remember { mutableIntStateOf(3) }
    var department by remember { mutableStateOf("ИТ") }
    var showConfirm by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showDeptPopup by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val departments = listOf("ИТ", "HR", "Финансы", "Хозяйство")
    val priorities = listOf("Низкий", "Средний", "Высокий")
    val actionEntry = remember(scope, snackbarHostState) {
        DropdownEntry(
            items = listOf("Сохранить черновик", "Назначить", "Дублировать").mapIndexed { index, text ->
                DropdownItem(
                    text = text,
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                listOf("Черновик сохранён", "Назначено", "Дубликат создан")[index],
                            )
                        }
                    },
                )
            },
        )
    }
    val menuEntry = remember(scope, snackbarHostState) {
        DropdownEntry(
            items = listOf("В работу", "На паузу", "Закрыть").mapIndexed { index, text ->
                DropdownItem(
                    text = text,
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                listOf("В работе", "На паузе", "Закрыта")[index],
                            )
                        }
                    },
                )
            },
        )
    }

    Scaffold(
        floatingToolbar = {
            FloatingToolbar {
                Row(
                    modifier = Modifier.background(Color.Red),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TooltipBox(text = "Дополнительно...") {
                        IconButton(onClick = { showSheet = true }) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = "Дополнительно",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    TooltipBox(text = "Действия") {
                        OverlayIconDropdownMenu(entry = actionEntry) {
                            Icon(
                                imageVector = MiuixIcons.Send,
                                contentDescription = "Действия",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    TooltipBox(text = "Удалить") {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Заявка помечена к удалению",
                                        actionLabel = "Отменить",
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = "Удалить",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize().background(Color.Yellow)
                //.padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallTitle(text = "@@@@$titleHint")
            Card {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Тема",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Срочная",
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Switch(checked = urgent, onCheckedChange = { urgent = it })
                }
            }

            Card {
                SmallTitle(
                    text = "Уведомления",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row {
                        Checkbox(
                            state = if (notifyEmail) ToggleableState.On else ToggleableState.Off,
                            onClick = { notifyEmail = !notifyEmail },
                        )
                        Text(
                            text = "Email",
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Row {
                        Checkbox(
                            state = if (notifyPush) ToggleableState.On else ToggleableState.Off,
                            onClick = { notifyPush = !notifyPush },
                        )
                        Text(
                            text = "Push",
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Card {
                SmallTitle(
                    text = "Приоритет",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                priorities.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = item, color = MiuixTheme.colorScheme.onSurface)
                        RadioButton(
                            selected = priority == item,
                            onClick = { priority = item },
                        )
                    }
                }
            }

            Card {
                SmallTitle(
                    text = "Трудозатраты: ${(effort * 10).toInt()} ч",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Slider(
                    value = effort,
                    onValueChange = { effort = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SmallTitle(
                    text = "Срок, дней",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                NumberPicker(
                    value = days,
                    onValueChange = { days = it },
                    range = 1..14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 16.dp),
                )
            }

            Card {
                OverlayDropdownMenu(
                    title = "Статус обработки",
                    summary = "Быстрые действия",
                    entry = menuEntry,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "Подразделение: $department",
                        onClick = { showDeptPopup = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    )
                    OverlayListPopup(
                        show = showDeptPopup,
                        onDismissRequest = { showDeptPopup = false },
                    ) {
                        ListPopupColumn {
                            departments.forEachIndexed { index, string ->
                                DropdownImpl(
                                    text = string,
                                    optionSize = departments.size,
                                    isSelected = department == string,
                                    index = index,
                                    onSelectedIndexChange = {
                                        department = departments[it]
                                        showDeptPopup = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            TextButton(
                text = "Отправить на согласование",
                onClick = { showConfirm = true },
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(72.dp))
        }

        OverlayDialog(
            title = "Отправить заявку?",
            summary = "Тема: ${title.ifBlank { "без названия" }}. Приоритет: $priority.",
            show = showConfirm,
            onDismissRequest = { showConfirm = false },
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = "Отмена",
                    onClick = { showConfirm = false },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(
                    text = "Отправить",
                    onClick = {
                        showConfirm = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Заявка отправлена")
                        }
                        onSaved()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }


    }//scaffold content end

    OverlayBottomSheet(
        title = "Дополнительно",
        show = true,
        onDismissRequest = { showSheet = false },
    ) {
        Text(
            text = "Вложения и согласующие настраиваются после отправки.",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        TextButton(
            text = "Закрыть",
            onClick = { showSheet = false },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
