@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui.sites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.walhalla.lazagne.ui.ScreenList
import dev.walhalla.lazagne.ui.TitleWithSubtitle
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoSearchTextField
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ListBulletIndent
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwSite
import net.lds.online.ui.base.BaseScreenLayout
import net.lds.online.ui.base.UiState

@Composable
fun SitesRoute(
    swRepository: NosSwRepository,
    padding: PaddingValues,
    onOpenSite: (SwSite) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SitesViewModel = remember(swRepository) { SitesViewModel(swRepository) },
) {
    SitesScreen(
        viewModel = viewModel,
        padding = padding,
        onOpenSite = onOpenSite,
        modifier = modifier,
    )
}

@Composable
fun SitesScreen(
    viewModel: SitesViewModel,
    padding: PaddingValues,
    onOpenSite: (SwSite) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        BaseScreenLayout(
            state = state,
            onRefresh = { viewModel.onEvent(SitesEvent.Refresh) },
            onRetry = { viewModel.loadData() },
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            val visible = data.visible
            val refreshing = (state.contentState as? UiState.Success)?.isRefreshing == true
            ScreenList(padding) {
                item {
                    CupertinoSearchTextField(
                        value = data.query,
                        onValueChange = { viewModel.onEvent(SitesEvent.QueryChanged(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        placeholder = { CupertinoText("Имя узла") },
                    )
                }

                if (refreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(CupertinoSectionDefaults.PaddingValues),
                            contentAlignment = Alignment.Center,
                        ) { CupertinoActivityIndicator() }
                    }
                }

                if (visible.isEmpty()) {
                    item {
                        EmptySitesState(
                            if (data.query.isBlank()) "Нет узлов" else "Ничего не найдено",
                        )
                    }
                } else {
                    section(
                        title = {
                            CupertinoText("Узлы · ${visible.size}".sectionTitle())
                        },
                    ) {
                        visible.forEach { site ->
                            link(
                                key = site.id,
                                onClick = { onOpenSite(site) },
                            ) {
                                TitleWithSubtitle(
                                    title = site.name.ifBlank { "— #${site.id}" },
                                    subtitle = "/sw/${site.id}",
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
private fun EmptySitesState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Default.ListBulletIndent,
            contentDescription = null,
            tint = CupertinoTheme.colorScheme.tertiaryLabel,
            modifier = Modifier.size(48.dp),
        )
        CupertinoText(
            text = text,
            style = CupertinoTheme.typography.headline,
            color = CupertinoTheme.colorScheme.secondaryLabel,
            textAlign = TextAlign.Center,
        )
    }
}
