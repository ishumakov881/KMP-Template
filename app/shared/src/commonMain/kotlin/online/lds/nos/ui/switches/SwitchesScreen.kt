@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.switches

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import online.lds.nos.ui.ScreenList
import online.lds.nos.ui.TitleWithSubtitle
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoSearchTextField
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControl
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControlTab
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.resolvedIconUrl
import net.lds.online.ui.base.BaseScreenLayout
import net.lds.online.ui.base.UiState

@Composable
fun SwitchesRoute(
    mapRepository: NosMapRepository,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SwitchesViewModel = remember(mapRepository) { SwitchesViewModel(mapRepository) },
) {
    SwitchesScreen(viewModel = viewModel, padding = padding, modifier = modifier)
}

@Composable
fun SwitchesScreen(
    viewModel: SwitchesViewModel,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        BaseScreenLayout(
            state = state,
            onRefresh = { viewModel.onEvent(SwitchesEvent.Refresh) },
            onRetry = { viewModel.loadData() },
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            val visible = data.visible
            val refreshing = (state.contentState as? UiState.Success)?.isRefreshing == true
            ScreenList(padding) {
                item {
                    CupertinoSegmentedControl(
                        selectedTabIndex = data.filter.ordinal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CupertinoSectionDefaults.PaddingValues),
                    ) {
                        SwitchStatusFilter.entries.forEach { filter ->
                            CupertinoSegmentedControlTab(
                                onClick = { viewModel.onEvent(SwitchesEvent.FilterChanged(filter)) },
                                isSelected = data.filter == filter,
                            ) { CupertinoText(filter.title) }
                        }
                    }
                }

                item {
                    CupertinoSearchTextField(
                        value = data.query,
                        onValueChange = { viewModel.onEvent(SwitchesEvent.QueryChanged(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { CupertinoText("Имя или статус") },
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
                        EmptySwitchesState(
                            if (data.query.isBlank() && data.filter == SwitchStatusFilter.All) {
                                "Нет свичей"
                            } else {
                                "Ничего не найдено"
                            },
                        )
                    }
                } else {
                    section(
                        title = {
                            CupertinoText(
                                "Свичи · ${visible.size}".sectionTitle(),
                            )
                        },
                    ) {
                        visible.forEach { item ->
                            link(
                                key = "${item.name}|${item.lat}|${item.lng}|${item.layer}",
                                onClick = { },
                                icon = {
                                    val url = item.resolvedIconUrl()
                                    if (url != null) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = statusTitle(item.layer),
                                            modifier = Modifier.size(28.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                    } else {
                                        CupertinoIcon(
                                            imageVector = CupertinoIcons.Default.ServerRack,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = CupertinoTheme.colorScheme.secondaryLabel,
                                        )
                                    }
                                },
                            ) {
                                TitleWithSubtitle(
                                    title = item.name.ifBlank { "—" },
                                    subtitle = statusTitle(item.layer),
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
private fun EmptySwitchesState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Default.ServerRack,
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
