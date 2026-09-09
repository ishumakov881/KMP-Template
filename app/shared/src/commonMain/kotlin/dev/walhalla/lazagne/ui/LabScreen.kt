package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LabScreen(
    snackbarHostState: SnackbarHostState,
) {
    var contourTab by remember { mutableIntStateOf(0) }
    var floatNav by remember { mutableIntStateOf(0) }
    var pickerColor by remember { mutableStateOf(Color(0xFF3482FF)) }
    var progress by remember { mutableFloatStateOf(0.45f) }
    var showCascadingPopup by remember { mutableStateOf(false) }
    var sortIndex by remember { mutableIntStateOf(0) }
    var viewIndex by remember { mutableIntStateOf(0) }
    var filterIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val sortLabels = listOf("По дате", "По имени")
    val viewLabels = listOf("Сетка", "Список")
    val filterLabels = listOf("Все", "Только мои")

    val cascadingEntries = remember(sortIndex, viewIndex, filterIndex) {
        listOf(
            DropdownEntry(
                items = sortLabels.mapIndexed { idx, label ->
                    DropdownItem(
                        text = label,
                        selected = sortIndex == idx,
                        onClick = { sortIndex = idx },
                    )
                },
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = "Вид",
                        children = viewLabels.mapIndexed { idx, label ->
                            DropdownItem(
                                text = label,
                                selected = viewIndex == idx,
                                onClick = { viewIndex = idx },
                            )
                        },
                    ),
                    DropdownItem(
                        text = "Фильтр",
                        children = filterLabels.mapIndexed { idx, label ->
                            DropdownItem(
                                text = label,
                                selected = filterIndex == idx,
                                onClick = { filterIndex = idx },
                            )
                        },
                    ),
                ),
            ),
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Лаборатория UI",
                actions = {
                    OverlayIconCascadingDropdownMenu(entries = cascadingEntries) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "Меню",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        bottomBar = {
            FloatingNavigationBar {
                listOf(
                    "Обзор" to MiuixIcons.Home,
                    "Формы" to MiuixIcons.Edit,
                    "Фильтры" to MiuixIcons.Filter,
                ).forEachIndexed { index, (label, icon) ->
                    FloatingNavigationBarItem(
                        selected = floatNav == index,
                        onClick = { floatNav = index },
                        icon = icon,
                        label = label,
                    )
                }
            }
        },
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabRowWithContour(
                tabs = listOf("Basic", "Cascading"),
                selectedTabIndex = contourTab,
                onTabSelected = { contourTab = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "FloatingNav: ${listOf("Обзор", "Формы", "Фильтры")[floatNav]}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            when (contourTab) {
                0 -> BasicLabPanel(
                    pickerColor = pickerColor,
                    onPickerColorChange = { pickerColor = it },
                    progress = progress,
                    onBumpProgress = {
                        progress = ((progress + 0.15f) % 1.05f).coerceAtMost(1f)
                    },
                    onPrimaryClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Primary Button")
                        }
                    },
                )
                else -> CascadingLabPanel(
                    showPopup = showCascadingPopup,
                    onShowPopupChange = { showCascadingPopup = it },
                    entries = cascadingEntries,
                    summary = "Сорт: ${sortLabels[sortIndex]} · Вид: ${viewLabels[viewIndex]} · ${filterLabels[filterIndex]}",
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun BasicLabPanel(
    pickerColor: Color,
    onPickerColorChange: (Color) -> Unit,
    progress: Float,
    onBumpProgress: () -> Unit,
    onPrimaryClick: () -> Unit,
) {
    Card {
        SmallTitle(
            text = "Button · BadgedBox · Progress",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onPrimaryClick,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("Primary")
            }
            BadgedBox(badge = { Badge { Text("2") } }) {
                Icon(
                    imageVector = MiuixIcons.Notes,
                    contentDescription = "Уведомления",
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(8.dp),
                )
            }
            BadgedBox(badge = { Badge() }) {
                Icon(
                    imageVector = MiuixIcons.Filter,
                    contentDescription = "Точка",
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Linear · ${(progress * 100).toInt()}%",
                color = MiuixTheme.colorScheme.onSurface,
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
                TextButton(text = "+15% progress", onClick = onBumpProgress)
            }
        }
    }

    Card {
        SmallTitle(
            text = "ColorPicker",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ColorPicker(
            color = pickerColor,
            onColorChanged = onPickerColorChange,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun CascadingLabPanel(
    showPopup: Boolean,
    onShowPopupChange: (Boolean) -> Unit,
    entries: List<DropdownEntry>,
    summary: String,
) {
    Card {
        SmallTitle(
            text = "OverlayCascadingListPopup",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = summary,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            TextButton(
                text = "Открыть cascading popup",
                onClick = { onShowPopupChange(true) },
                modifier = Modifier.fillMaxWidth(),
            )
            OverlayCascadingListPopup(
                show = showPopup,
                entries = entries,
                onDismissRequest = { onShowPopupChange(false) },
            )
        }
        Text(
            text = "OverlayIconCascadingDropdownMenu — в SmallTopAppBar справа",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
