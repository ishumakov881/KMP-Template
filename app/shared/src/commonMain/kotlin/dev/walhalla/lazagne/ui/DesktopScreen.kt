package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Badge
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListHeader
import io.github.composefluent.component.ListItem
import io.github.composefluent.component.ListItemSeparator
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import io.github.composefluent.surface.Card
import kotlinx.coroutines.delay

@Composable
fun DesktopScreen(
    loaded: Boolean,
    onLoaded: () -> Unit,
    recent: List<CorpRequest>,
    onOpenType: (RequestType) -> Unit,
    onOpenRequest: (CorpRequest) -> Unit,
) {
    LaunchedEffect(loaded) {
        if (!loaded) {
            delay(900)
            onLoaded()
        }
    }

    ScreenList {
        if (!loaded) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    ProgressRing()
                }
            }
            return@ScreenList
        }

        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("Добрый день, Анна", style = FluentTheme.typography.title)
                Text(
                    text = "Среда, 14 октября",
                    style = FluentTheme.typography.body,
                    color = FluentTheme.colors.text.text.secondary,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiCard("${recent.count { it.status == RequestStatus.Pending }}", "ждут решения", Modifier.weight(1f))
                KpiCard("12", "дней отпуска", Modifier.weight(1f))
            }
        }

        item { ListHeader(content = { Text("Сегодня") }) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    TitleWithSubtitle("Планёрка отдела", "10:00 · переговорная «Один»")
                    ListItemSeparator(Modifier.padding(vertical = 8.dp))
                    TitleWithSubtitle("Сдать квартальный отчёт", "до 18:00")
                }
            }
        }

        item { ListHeader(content = { Text("Приложения") }) }
        RequestType.entries.forEach { type ->
            item(key = type) {
                ListItem(
                    onClick = { onOpenType(type) },
                    icon = { Icon(type.icon, contentDescription = null) },
                    text = { TitleWithSubtitle(type.title, type.subtitle) },
                )
            }
        }

        item { ListHeader(content = { Text("Недавние заявки") }) }
        recent.forEach { request ->
            item(key = request.id) { RequestRow(request, onClick = { onOpenRequest(request) }) }
        }
    }
}

@Composable
private fun KpiCard(value: String, label: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = FluentTheme.typography.title, color = FluentTheme.colors.text.accent.primary)
            Text(label, style = FluentTheme.typography.caption, color = FluentTheme.colors.text.text.secondary)
        }
    }
}

@Composable
fun TitleWithSubtitle(title: String, subtitle: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(title, style = FluentTheme.typography.bodyStrong, maxLines = 1)
        Text(
            text = subtitle,
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.secondary,
            maxLines = 1,
        )
    }
}

@Composable
fun RequestRow(request: CorpRequest, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        icon = { Icon(request.type.icon, contentDescription = null) },
        text = { TitleWithSubtitle("${request.type.title} №${request.id}", "${request.author} · ${request.status.title}") },
        trailing = { Badge(status = request.status.badge) },
    )
}
