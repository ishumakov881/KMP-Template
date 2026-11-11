@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.sites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import online.lds.nos.ui.ScreenList
import online.lds.nos.ui.TitleWithSubtitle
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.items
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemGray
import io.github.alexzhirkevich.cupertino.theme.systemGreen
import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwSnmpPort
import net.lds.online.ui.base.BaseScreenLayout
import net.lds.online.ui.base.UiState

@Composable
fun SnmpRoute(
    swRepository: NosSwRepository,
    ip: String,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember(swRepository, ip) { SnmpViewModel(swRepository, ip) }
    SnmpScreen(viewModel = viewModel, padding = padding, modifier = modifier)
}

@Composable
fun SnmpScreen(
    viewModel: SnmpViewModel,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        BaseScreenLayout(
            state = state,
            onRefresh = { viewModel.onEvent(SnmpEvent.Refresh) },
            onRetry = { viewModel.loadData() },
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            val page = data.page
            val refreshing = (state.contentState as? UiState.Success)?.isRefreshing == true
            ScreenList(padding) {
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

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CupertinoSectionDefaults.PaddingValues),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CupertinoText(
                            text = page.ip,
                            style = CupertinoTheme.typography.title2,
                        )
                        if (page.label.isNotBlank()) {
                            CupertinoText(
                                text = page.label,
                                style = CupertinoTheme.typography.body,
                                color = CupertinoTheme.colorScheme.secondaryLabel,
                            )
                        }
                        if (page.freeCount > 0) {
                            CupertinoText(
                                text = "Свободно портов: ${page.freeCount}" +
                                    if (page.freePorts.isNotEmpty()) {
                                        " (${page.freePorts.joinToString()})"
                                    } else {
                                        ""
                                    },
                                style = CupertinoTheme.typography.footnote,
                                color = CupertinoTheme.colorScheme.tertiaryLabel,
                            )
                        }
                    }
                }

                if (page.ports.isEmpty()) {
                    item {
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
                                text = "Нет данных портов",
                                style = CupertinoTheme.typography.headline,
                                color = CupertinoTheme.colorScheme.secondaryLabel,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    section(
                        title = {
                            CupertinoText(
                                "Порты · ${page.ports.size}".sectionTitle(),
                            )
                        },
                    ) {
                        items(
                            items = page.ports,
                            key = { it.index },
                        ) { port, itemPadding ->
                            SnmpPortRow(
                                port = port,
                                modifier = Modifier.padding(itemPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnmpPortRow(
    port: SwSnmpPort,
    modifier: Modifier = Modifier,
) {
    val gray = port.bgColor?.equals("Gray", ignoreCase = true) == true
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoLinkIcon(
            imageVector = CupertinoIcons.Default.ServerRack,
            containerColor = if (gray) {
                CupertinoColors.systemGray
            } else {
                CupertinoColors.systemGreen
            },
        )
        Spacer(Modifier.width(12.dp))
        TitleWithSubtitle(
            title = buildString {
                append(port.name.ifBlank { "#${port.index}" })
                if (port.type.isNotBlank()) {
                    append(" · ")
                    append(port.type)
                }
            },
            subtitle = buildString {
                if (port.vlan.isNotBlank()) append(port.vlan)
                if (port.description.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(port.description)
                }
                if (port.tag.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(port.tag)
                }
            },
        )
    }
}
