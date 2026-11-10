@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.sites

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import online.lds.nos.ui.ScreenList
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwPicGraph
import net.lds.online.ui.base.BaseScreenLayout
import net.lds.online.ui.base.UiState

@Composable
fun SwPicRoute(
    swRepository: NosSwRepository,
    path: String,
    titleHint: String,
    padding: PaddingValues,
    onOpenGraph: (path: String, titleHint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember(swRepository, path) {
        SwPicViewModel(swRepository, path, titleHint)
    }
    SwPicScreen(
        viewModel = viewModel,
        padding = padding,
        onOpenGraph = onOpenGraph,
        modifier = modifier,
    )
}

@Composable
fun SwPicScreen(
    viewModel: SwPicViewModel,
    padding: PaddingValues,
    onOpenGraph: (path: String, titleHint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        BaseScreenLayout(
            state = state,
            onRefresh = { viewModel.onEvent(SwPicEvent.Refresh) },
            onRetry = { viewModel.loadData() },
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            val graphs = data.page.graphs
            val refreshing = (state.contentState as? UiState.Success)?.isRefreshing == true
            ScreenList(padding) {
                item {
                    CupertinoText(
                        text = data.page.title,
                        style = CupertinoTheme.typography.title2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CupertinoSectionDefaults.PaddingValues),
                        textAlign = TextAlign.Center,
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

                if (graphs.isEmpty()) {
                    item { EmptyGraphsState() }
                } else {
                    items(
                        items = graphs,
                        key = { it.imageUrl },
                    ) { graph ->
                        GraphCard(
                            graph = graph,
                            titleHint = data.page.title,
                            onOpenGraph = onOpenGraph,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphCard(
    graph: SwPicGraph,
    titleHint: String,
    onOpenGraph: (path: String, titleHint: String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val canOpen = graph.nextPath != null
    AsyncImage(
        model = graph.imageUrl,
        contentDescription = titleHint,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (canOpen) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = {
                            graph.nextPath?.let { onOpenGraph(it, titleHint) }
                        },
                    )
                } else {
                    Modifier
                },
            ),
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
private fun EmptyGraphsState() {
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
            text = "Нет графиков",
            style = CupertinoTheme.typography.headline,
            color = CupertinoTheme.colorScheme.secondaryLabel,
            textAlign = TextAlign.Center,
        )
    }
}
