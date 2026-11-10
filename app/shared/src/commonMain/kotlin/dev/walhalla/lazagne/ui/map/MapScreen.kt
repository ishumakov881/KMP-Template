package dev.walhalla.lazagne.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.ui.base.BaseScreenLayout

@Composable
fun MapRoute(
    mapRepository: NosMapRepository,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember(mapRepository) { MapViewModel(mapRepository) }
    MapScreen(viewModel = viewModel, padding = padding, modifier = modifier)
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        BaseScreenLayout(
            state = state,
            onRefresh = { viewModel.onEvent(MapEvent.Refresh) },
            onRetry = { viewModel.loadData() },
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            NosMapCanvas(
                markers = data.markers,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
