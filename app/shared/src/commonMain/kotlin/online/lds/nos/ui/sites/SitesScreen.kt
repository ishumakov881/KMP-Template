@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.sites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import online.lds.nos.ui.CupertinoEmptyListStub
import online.lds.nos.ui.ScreenList
import online.lds.nos.ui.TitleWithSubtitle
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
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
                        CupertinoEmptyListStub(
                            text = if (data.query.isBlank()) "Нет узлов" else "Ничего не найдено",
                            icon = CupertinoIcons.Default.ListBulletIndent,
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

