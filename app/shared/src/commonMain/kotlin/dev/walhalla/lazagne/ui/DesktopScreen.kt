package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class DeskItem(
    val id: String,
    val title: String,
    val summary: String,
)

private val SampleItems = listOf(
    DeskItem("A-104", "Доступ в VPN", "ИТ · ожидает согласования"),
    DeskItem("A-105", "Замена ноутбука", "Хозяйство · в работе"),
    DeskItem("A-106", "Онбординг подрядчика", "HR · новый"),
    DeskItem("A-107", "Бюджет на ПО", "Финансы · на проверке"),
    DeskItem("A-108", "Парковка на неделю", "Админ · согласовано"),
    DeskItem("A-109", "Ключ от переговорной", "Охрана · выдан"),
)

@Composable
fun DesktopScreen(
    tabLabel: String,
    onOpenRequest: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var items by remember { mutableStateOf(SampleItems) }
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(700)
        loading = false
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(600)
            items = SampleItems.shuffled()
            isRefreshing = false
            scope.launch {
                snackbarHostState.showSnackbar("Список обновлён")
            }
        }
    }

    val filtered = items.filter {
        val matchesTab = when (tabLabel) {
            "Мои" -> it.id in setOf("A-104", "A-106")
            "Команда" -> it.id in setOf("A-105", "A-107", "A-108")
            else -> true
        }
        val matchesQuery = query.isBlank() ||
            it.title.contains(query, ignoreCase = true) ||
            it.id.contains(query, ignoreCase = true)
        matchesTab && matchesQuery
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            inputField = {
                InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { searchExpanded = false },
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    label = "Поиск заявок",
                )
            },
            expanded = searchExpanded,
            onExpandedChange = { searchExpanded = it },
            outsideEndAction = {
                TextButton(
                    text = "Отмена",
                    onClick = {
                        searchExpanded = false
                        query = ""
                    },
                )
            },
        ) {
            filtered.take(3).forEach { item ->
                BasicComponent(
                    title = item.title,
                    summary = item.id,
                    onClick = {
                        query = item.title
                        searchExpanded = false
                        onOpenRequest(item.id)
                    },
                )
            }
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "Ничего не найдено",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                pullToRefreshState = pullState,
                modifier = Modifier.fillMaxSize(),
                color = MiuixTheme.colorScheme.primary,
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SmallTitle(
                            text = "Активные · $tabLabel",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            filtered.forEachIndexed { index, item ->
                                BasicComponent(
                                    title = item.title,
                                    summary = item.summary,
                                    startAction = {
                                        Icon(
                                            imageVector = MiuixIcons.Notes,
                                            contentDescription = null,
                                            tint = MiuixTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(end = 12.dp),
                                        )
                                    },
                                    onClick = { onOpenRequest(item.id) },
                                )
                                if (index < filtered.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Сводка смены",
                                    style = MiuixTheme.textStyles.headline2,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${filtered.size} заявок в текущем фильтре",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveScreen(
    tabLabel: String,
    onOpenRequest: (String) -> Unit,
) {
    val archived = remember(tabLabel) {
        if (tabLabel == "Отклонённые") {
            listOf(
                DeskItem("Z-11", "Удалённый офис", "Отклонено · политика"),
                DeskItem("Z-12", "Новый монитор", "Отклонено · бюджет"),
            )
        } else {
            listOf(
                DeskItem("Z-01", "Сброс пароля", "Закрыто"),
                DeskItem("Z-02", "Пропуск гостя", "Закрыто"),
                DeskItem("Z-03", "Переезд стола", "Закрыто"),
            )
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SmallTitle(
                text = "Архив · $tabLabel",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(archived) { item ->
            BasicComponent(
                title = item.title,
                summary = item.summary,
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Notes,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
                onClick = { onOpenRequest(item.id) },
            )
            HorizontalDivider()
        }
    }
}
