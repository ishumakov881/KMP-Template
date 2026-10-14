@file:OptIn(ExperimentalFluentApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AutoSuggestBoxDefaults
import io.github.composefluent.component.AutoSuggestionBox
import io.github.composefluent.component.CommandBar
import io.github.composefluent.component.CommandBarButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListHeader
import io.github.composefluent.component.ListItem
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.SelectorBar
import io.github.composefluent.component.SelectorBarItem
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextField
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Add
import io.github.composefluent.icons.regular.Archive
import io.github.composefluent.icons.regular.ArrowSync
import io.github.composefluent.icons.regular.Search

@Composable
fun RequestsScreen(
    state: RequestsState,
    onOpenRequest: (CorpRequest) -> Unit,
    onCreate: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var commandsExpanded by remember { mutableStateOf(false) }
    var suggestExpanded by remember { mutableStateOf(false) }

    val inFolder = state.items.filter { it.folder == state.folder }
    val visible = inFolder.filter { request ->
        state.query.isBlank() ||
            request.author.contains(state.query, ignoreCase = true) ||
            request.type.title.contains(state.query, ignoreCase = true) ||
            request.id.toString().contains(state.query)
    }
    val suggestions = RequestType.entries.filter {
        state.query.isNotBlank() && it.title.contains(state.query, ignoreCase = true) && it.title != state.query
    }

    ScreenList {
        item {
            SelectorBar {
                RequestFolder.entries.forEach { folder ->
                    SelectorBarItem(
                        selected = state.folder == folder,
                        onSelectedChange = { state.folder = folder },
                        text = { Text(folder.title) },
                    )
                }
            }
        }

        item {
            AutoSuggestionBox(
                expanded = suggestExpanded && suggestions.isNotEmpty(),
                onExpandedChange = { suggestExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                TextField(
                    value = state.query,
                    onValueChange = {
                        state.query = it
                        suggestExpanded = true
                    },
                    singleLine = true,
                    placeholder = { Text("Поиск по заявкам") },
                    leadingIcon = { Icon(Icons.Regular.Search, contentDescription = null) },
                    shape = AutoSuggestBoxDefaults.textFieldShape(suggestExpanded && suggestions.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth().flyoutAnchor(),
                )
                AutoSuggestBoxDefaults.suggestFlyout(
                    expanded = suggestExpanded && suggestions.isNotEmpty(),
                    onDismissRequest = { suggestExpanded = false },
                    modifier = Modifier.flyoutSize(matchAnchorWidth = true),
                    itemsContent = {
                        items(suggestions.size) { index ->
                            val type = suggestions[index]
                            ListItem(
                                onClick = {
                                    state.query = type.title
                                    suggestExpanded = false
                                },
                                icon = { Icon(type.icon, contentDescription = null) },
                                text = { Text(type.title) },
                            )
                        }
                    },
                )
            }
        }

        item {
            CommandBar(
                expanded = commandsExpanded,
                onExpandedChanged = { commandsExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    CommandBarButton(onClick = onCreate) {
                        Icon(Icons.Regular.Add, contentDescription = null)
                        Text("Создать")
                    }
                }
                item {
                    CommandBarButton(onClick = { state.refresh(scope) }, enabled = !state.refreshing) {
                        Icon(Icons.Regular.ArrowSync, contentDescription = null)
                        Text("Обновить")
                    }
                }
            }
        }

        if (state.refreshing) {
            item { ProgressBar(Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
        }

        if (visible.isEmpty()) {
            item {
                EmptyState(
                    if (state.query.isBlank()) "В папке «${state.folder.title}» пусто" else "Ничего не найдено",
                )
            }
        } else {
            item { ListHeader(content = { Text("${state.folder.title} · ${visible.size}") }) }
            visible.forEach { request ->
                item(key = request.id) { RequestRow(request, onClick = { onOpenRequest(request) }) }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Regular.Archive,
            contentDescription = null,
            tint = FluentTheme.colors.text.text.tertiary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = text,
            style = FluentTheme.typography.subtitle,
            color = FluentTheme.colors.text.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}
