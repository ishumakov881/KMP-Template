@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.map

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
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.default
import net.lds.online.components.ErrorView
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.ui.base.UiState
import online.lds.nos.data.AppPreferences
import online.lds.nos.data.InMemorySettings

@Composable
fun MapRoute(
    mapRepository: NosMapRepository,
    padding: PaddingValues,
    onOpenPic: (ip: String, titleHint: String) -> Unit = { _, _ -> },
    onOpenSite: (siteId: Int, siteName: String) -> Unit = { _, _ -> },
    onOpenSnmp: (ip: String) -> Unit = {},
    appPreferences: AppPreferences = remember { AppPreferences(InMemorySettings()) },
    viewModel: MapViewModel = remember(mapRepository, appPreferences) {
        MapViewModel(mapRepository, appPreferences)
    },
    modifier: Modifier = Modifier,
) {
    MapScreen(
        viewModel = viewModel,
        padding = padding,
        onOpenPic = onOpenPic,
        onOpenSite = onOpenSite,
        onOpenSnmp = onOpenSnmp,
        modifier = modifier,
    )
}

/**
 * Map does **not** use [net.lds.online.ui.base.BaseScreenLayout]: waiting on all markgen
 * types behind a full-screen shimmer costs ~3s. Shell (tiles + legend stub) paints immediately;
 * markers stream in per type like the web OSM map.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    padding: PaddingValues,
    onOpenPic: (ip: String, titleHint: String) -> Unit,
    onOpenSite: (siteId: Int, siteName: String) -> Unit,
    onOpenSnmp: (ip: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(
        modifier = modifier.fillMaxSize().padding(padding),
    ) {
        when (val content = state.contentState) {
            is UiState.Error -> ErrorView(
                message = content.message,
                onRetry = { viewModel.onEvent(MapEvent.Refresh) },
            )
            is UiState.Success -> MapShellContent(
                data = content.data,
                viewModel = viewModel,
                onOpenPic = onOpenPic,
                onOpenSite = onOpenSite,
                onOpenSnmp = onOpenSnmp,
            )
            // Loading/Empty should not appear for map (VM seeds Success shell); keep tiles if they do.
            else -> MapShellContent(
                data = MapUiData(markersLoading = true),
                viewModel = viewModel,
                onOpenPic = onOpenPic,
                onOpenSite = onOpenSite,
                onOpenSnmp = onOpenSnmp,
            )
        }
    }

    val success = state.contentState
    if (success is UiState.Success) {
        val data = success.data
        data.pingResult?.let { result ->
            CupertinoAlertDialog(
                onDismissRequest = { viewModel.onEvent(MapEvent.DismissPingResult) },
                title = { CupertinoText("Ping") },
                message = { CupertinoText(result) },
            ) {
                default(onClick = { viewModel.onEvent(MapEvent.DismissPingResult) }) {
                    CupertinoText("Ok")
                }
            }
        }
        data.actionError?.let { message ->
            CupertinoAlertDialog(
                onDismissRequest = { viewModel.onEvent(MapEvent.DismissActionError) },
                title = { CupertinoText("Ошибка") },
                message = { CupertinoText(message) },
            ) {
                default(onClick = { viewModel.onEvent(MapEvent.DismissActionError) }) {
                    CupertinoText("Понятно")
                }
            }
        }
        if (data.maintenancePromptVisible) {
            MapMaintenanceCommentDialog(
                onDismiss = { viewModel.onEvent(MapEvent.DismissMaintenancePrompt) },
                onConfirm = { comment ->
                    viewModel.onEvent(MapEvent.ConfirmEnableMaintenance(comment))
                },
            )
        }
    }
}

@Composable
private fun MapShellContent(
    data: MapUiData,
    viewModel: MapViewModel,
    onOpenPic: (ip: String, titleHint: String) -> Unit,
    onOpenSite: (siteId: Int, siteName: String) -> Unit,
    onOpenSnmp: (ip: String) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        NosMapCanvas(
            markers = data.visibleMarkers,
            lines = data.visibleLines,
            modifier = Modifier.fillMaxSize(),
            onMarkerClick = { marker ->
                viewModel.onEvent(MapEvent.SelectMarker(marker))
            },
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapRefreshPanel(
                selected = data.refreshInterval,
                onSelect = { interval ->
                    viewModel.onEvent(MapEvent.SetRefreshInterval(interval))
                },
            )
            MapLegendPanel(
                snapshot = data.legend,
                visibleLayers = data.visibleLayers,
                interactionsEnabled = data.legendInteractive,
                onToggleLayer = { layer ->
                    viewModel.onEvent(MapEvent.ToggleLayer(layer))
                },
                onToggleGroup = { groupId, enabled ->
                    viewModel.onEvent(MapEvent.ToggleGroup(groupId, enabled))
                },
            )
            if (data.markersLoading) {
                CupertinoActivityIndicator(modifier = Modifier.size(28.dp))
            }
        }

        data.selectedMarker?.let { marker ->
            MapMarkerPopup(
                marker = marker,
                pingLoading = data.pingLoading,
                onDismiss = { viewModel.onEvent(MapEvent.DismissPopup) },
                onPing = { viewModel.onEvent(MapEvent.PingSelected) },
                onOpenPic = {
                    val ip = marker.ip ?: return@MapMarkerPopup
                    viewModel.onEvent(MapEvent.DismissPopup)
                    onOpenPic(ip, marker.name.ifBlank { ip })
                },
                onOpenSite = {
                    val id = marker.siteId ?: return@MapMarkerPopup
                    viewModel.onEvent(MapEvent.DismissPopup)
                    onOpenSite(id, marker.siteLabel ?: "Узел $id")
                },
                onOpenSnmp = {
                    val ip = marker.ip ?: return@MapMarkerPopup
                    viewModel.onEvent(MapEvent.DismissPopup)
                    onOpenSnmp(ip)
                },
                onRequestEnableMaintenance = {
                    viewModel.onEvent(MapEvent.RequestEnableMaintenance)
                },
                onDisableMaintenance = {
                    viewModel.onEvent(MapEvent.DisableMaintenance)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }
    }
}
