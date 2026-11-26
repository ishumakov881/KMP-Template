@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoSearchTextField
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControl
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControlTab
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Archivebox
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemGreen
import io.github.alexzhirkevich.cupertino.theme.systemOrange
import io.github.alexzhirkevich.cupertino.theme.systemRed

@Composable
fun RequestsScreen(
    padding: PaddingValues,
    state: RequestsState,
    onOpenRequest: (CorpRequest) -> Unit,
) {
    val visible = state.items.filter { request ->
        request.folder == state.folder && (
            state.query.isBlank() ||
                request.type.title.contains(state.query, ignoreCase = true) ||
                request.author.contains(state.query, ignoreCase = true) ||
                request.id.toString().contains(state.query)
            )
    }

    ScreenList(padding) {
        item {
            CupertinoSegmentedControl(
                selectedTabIndex = state.folder.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CupertinoSectionDefaults.PaddingValues),
            ) {
                RequestFolder.entries.forEach { folder ->
                    CupertinoSegmentedControlTab(
                        onClick = { state.folder = folder },
                        isSelected = state.folder == folder,
                    ) { CupertinoText(folder.title) }
                }
            }
        }

        item {
            CupertinoSearchTextField(
                value = state.query,
                onValueChange = { state.query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { CupertinoText("Автор, тип или номер") },
            )
        }

        if (state.refreshing) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(CupertinoSectionDefaults.PaddingValues),
                    contentAlignment = Alignment.Center,
                ) { CupertinoActivityIndicator() }
            }
        }

        if (visible.isEmpty()) {
            item {
                CupertinoEmptyListStub(
                    text = if (state.query.isBlank()) {
                        "В папке «${state.folder.title}» пусто"
                    } else {
                        "Ничего не найдено"
                    },
                    icon = CupertinoIcons.Default.Archivebox,
                )
            }
        } else {
            section(title = { CupertinoText("${state.folder.title} · ${visible.size}".sectionTitle()) }) {
                visible.forEach { request ->
                    link(
                        key = request.id,
                        onClick = { onOpenRequest(request) },
                        icon = { StatusIcon(request.status) },
                    ) {
                        TitleWithSubtitle(
                            title = "${request.type.title} №${request.id}",
                            subtitle = "${request.author} · ${request.status.title}",
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIcon(status: RequestStatus) {
    CupertinoLinkIcon(imageVector = status.icon, containerColor = status.color())
}

@Composable
fun RequestStatus.color(): Color = when (this) {
    RequestStatus.Pending -> CupertinoColors.systemOrange
    RequestStatus.Approved -> CupertinoColors.systemGreen
    RequestStatus.Rejected -> CupertinoColors.systemRed
}

