@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoHorizontalDivider
import io.github.alexzhirkevich.cupertino.CupertinoSurface
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlinx.coroutines.delay

@Composable
fun DesktopScreen(
    padding: PaddingValues,
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

    ScreenList(padding) {
        if (!loaded) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    CupertinoActivityIndicator()
                }
            }
            return@ScreenList
        }

        item {
            Column(Modifier.padding(CupertinoSectionDefaults.PaddingValues)) {
                CupertinoText("Добрый день, Анна", style = CupertinoTheme.typography.title1)
                CupertinoText(
                    text = "Среда, 14 октября",
                    style = CupertinoTheme.typography.subhead,
                    color = CupertinoTheme.colorScheme.secondaryLabel,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CupertinoSectionDefaults.PaddingValues),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiTile("${recent.count { it.status == RequestStatus.Pending }}", "ждут решения", Modifier.weight(1f))
                KpiTile("12", "дней отпуска", Modifier.weight(1f))
            }
        }

        section(title = { CupertinoText("Сегодня".sectionTitle()) }) {
            item { rowPadding ->
                Column(Modifier.padding(rowPadding)) {
                    CupertinoText("Планёрка отдела", style = CupertinoTheme.typography.headline)
                    CupertinoText(
                        text = "10:00 · переговорная «Один»",
                        style = CupertinoTheme.typography.footnote,
                        color = CupertinoTheme.colorScheme.secondaryLabel,
                    )
                    CupertinoHorizontalDivider(Modifier.padding(vertical = 8.dp))
                    CupertinoText("Сдать квартальный отчёт", style = CupertinoTheme.typography.headline)
                    CupertinoText(
                        text = "до 18:00",
                        style = CupertinoTheme.typography.footnote,
                        color = CupertinoTheme.colorScheme.secondaryLabel,
                    )
                }
            }
        }

        section(title = { CupertinoText("Приложения".sectionTitle()) }) {
            RequestType.entries.forEach { type ->
                link(
                    key = type,
                    onClick = { onOpenType(type) },
                    icon = {
                        CupertinoLinkIcon(
                            imageVector = type.icon,
                            containerColor = CupertinoTheme.colorScheme.accent,
                        )
                    },
                ) { TitleWithSubtitle(type.title, type.subtitle) }
            }
        }

        section(title = { CupertinoText("Недавние заявки".sectionTitle()) }) {
            recent.forEach { request ->
                link(
                    key = request.id,
                    onClick = { onOpenRequest(request) },
                    icon = { StatusIcon(request.status) },
                ) { TitleWithSubtitle("${request.type.title} №${request.id}", request.status.title) }
            }
        }
    }
}

@Composable
private fun KpiTile(value: String, label: String, modifier: Modifier) {
    CupertinoSurface(
        modifier = modifier,
        shape = CupertinoTheme.shapes.large,
        color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground,
    ) {
        Column(Modifier.padding(CupertinoSectionDefaults.PaddingValues)) {
            CupertinoText(value, style = CupertinoTheme.typography.title1, color = CupertinoTheme.colorScheme.accent)
            CupertinoText(
                text = label,
                style = CupertinoTheme.typography.footnote,
                color = CupertinoTheme.colorScheme.secondaryLabel,
            )
        }
    }
}
