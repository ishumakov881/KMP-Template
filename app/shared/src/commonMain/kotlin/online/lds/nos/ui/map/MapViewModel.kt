package online.lds.nos.ui.map

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.lds.online.network.ApiException
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.legend.MapLegendSnapshot
import net.lds.online.network.nos.legend.NosMapLegendCatalog
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel
import net.lds.online.ui.base.UiState

data class MapUiData(
    val markers: List<LeanMapMarker> = emptyList(),
    val loadedTypes: Set<String> = emptySet(),
    val legend: MapLegendSnapshot = MapLegendSnapshot.Empty,
    /** Leaflet `layer` keys currently shown on the map (web legend checkboxes). */
    val visibleLayers: Set<String> = emptySet(),
    /** Web `#more_intval` — auto reload interval. */
    val refreshInterval: MapRefreshInterval = MapRefreshInterval.WebDefault,
    /** Selected marker — web Leaflet popup (structured, not HTML). */
    val selectedMarker: LeanMapMarker? = null,
    val pingLoading: Boolean = false,
    val pingResult: String? = null,
    val actionError: String? = null,
    val maintenancePromptVisible: Boolean = false,
) {
    val visibleMarkers: List<LeanMapMarker>
        get() = markers.filter { it.layer in visibleLayers }

    val allLegendLayers: Set<String>
        get() = legend.groups
            .flatMap { group -> group.items.mapNotNull { it.item.leafletLayer } }
            .toSet()
}

sealed interface MapEvent : BaseEvent {
    data object Refresh : MapEvent
    data class ToggleLayer(val leafletLayer: String) : MapEvent
    data class ToggleGroup(val groupId: String, val enabled: Boolean) : MapEvent
    data class SetRefreshInterval(val interval: MapRefreshInterval) : MapEvent
    data class SelectMarker(val marker: LeanMapMarker) : MapEvent
    data object DismissPopup : MapEvent
    data object PingSelected : MapEvent
    data object DismissPingResult : MapEvent
    data object DismissActionError : MapEvent
    /** Web maintenance: enable asks for comment; disable confirms in UI then this event. */
    data object RequestEnableMaintenance : MapEvent
    data object DismissMaintenancePrompt : MapEvent
    data class ConfirmEnableMaintenance(val message: String) : MapEvent
    data object DisableMaintenance : MapEvent
}

class MapViewModel(
    private val mapRepository: NosMapRepository,
    /** Defaults to full web markgen set from `map_osm.js` (sw/uzel/pon/mft/wifi/usil/amp/lines). */
    private val enabledTypes: List<String> = NosMapRepository.DEFAULT_MAP_TYPES,
) : BaseViewModel<MapUiData, MapEvent>() {

    private var refreshJob: Job? = null

    init {
        loadData()
        restartAutoRefresh(MapRefreshInterval.WebDefault)
    }

    override suspend fun onFetchData(): MapUiData {
        val previous = currentDataOrNull()
        val types = enabledTypes.distinct()
        val markers = mapRepository.markgenMany(types)
        val loaded = types.toSet()
        val legend = NosMapLegendCatalog.snapshotFor(
            loadedTypes = loaded,
            markers = markers,
        )
        val allLayers = legend.groups
            .flatMap { group -> group.items.mapNotNull { it.item.leafletLayer } }
            .toSet()
        val visibleLayers = mergeVisibility(
            previousVisible = previous?.visibleLayers.orEmpty(),
            previousAll = previous?.allLegendLayers.orEmpty(),
            newAll = allLayers,
        )
        val selected = previous?.selectedMarker?.let { prev ->
            markers.firstOrNull { it.samePointAs(prev) }
        }
        return MapUiData(
            markers = markers,
            loadedTypes = loaded,
            legend = legend,
            visibleLayers = visibleLayers,
            refreshInterval = previous?.refreshInterval ?: MapRefreshInterval.WebDefault,
            selectedMarker = selected,
        )
    }

    override fun onBusinessEvent(event: MapEvent) {
        when (event) {
            MapEvent.Refresh -> loadData(isRefreshing = true)
            is MapEvent.ToggleLayer -> toggleLayer(event.leafletLayer)
            is MapEvent.ToggleGroup -> toggleGroup(event.groupId, event.enabled)
            is MapEvent.SetRefreshInterval -> setRefreshInterval(event.interval)
            is MapEvent.SelectMarker -> updateData {
                it.copy(
                    selectedMarker = event.marker,
                    pingResult = null,
                    actionError = null,
                    maintenancePromptVisible = false,
                )
            }
            MapEvent.DismissPopup -> updateData {
                it.copy(
                    selectedMarker = null,
                    pingResult = null,
                    actionError = null,
                    maintenancePromptVisible = false,
                    pingLoading = false,
                )
            }
            MapEvent.PingSelected -> pingSelected()
            MapEvent.DismissPingResult -> updateData { it.copy(pingResult = null) }
            MapEvent.DismissActionError -> updateData { it.copy(actionError = null) }
            MapEvent.RequestEnableMaintenance -> updateData {
                it.copy(maintenancePromptVisible = true)
            }
            MapEvent.DismissMaintenancePrompt -> updateData {
                it.copy(maintenancePromptVisible = false)
            }
            is MapEvent.ConfirmEnableMaintenance -> setMaintenance(enabled = true, message = event.message)
            MapEvent.DisableMaintenance -> setMaintenance(enabled = false, message = "")
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }

    private fun setRefreshInterval(interval: MapRefreshInterval) {
        updateData { it.copy(refreshInterval = interval) }
        restartAutoRefresh(interval)
    }

    /** Same as web `setInterval(Refresh, intVal)` after radio change. */
    private fun restartAutoRefresh(interval: MapRefreshInterval) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(interval.millis)
                loadData(isRefreshing = true)
            }
        }
    }

    private fun pingSelected() {
        val marker = currentDataOrNull()?.selectedMarker ?: return
        val ip = marker.ip ?: return
        val t = marker.pingType ?: "s"
        updateData { it.copy(pingLoading = true, actionError = null, pingResult = null) }
        viewModelScope.launch {
            runCatching { mapRepository.ajaxPing(ip, t) }
                .onSuccess { result ->
                    updateData { it.copy(pingLoading = false, pingResult = result) }
                }
                .onFailure { e ->
                    updateData {
                        it.copy(
                            pingLoading = false,
                            actionError = (e as? ApiException)?.getUserMessage()
                                ?: e.message
                                ?: "Ошибка ping",
                        )
                    }
                }
        }
    }

    private fun setMaintenance(enabled: Boolean, message: String) {
        val marker = currentDataOrNull()?.selectedMarker ?: return
        val id = marker.maintenanceId ?: return
        updateData { it.copy(maintenancePromptVisible = false, actionError = null) }
        viewModelScope.launch {
            runCatching { mapRepository.setMaintenance(id, enabled, message) }
                .onSuccess {
                    updateData { data ->
                        val updated = marker.copy(
                            maintenanceActive = enabled,
                            maintenanceText = message.takeIf { enabled && it.isNotBlank() },
                        )
                        data.copy(
                            markers = data.markers.map { if (it.samePointAs(marker)) updated else it },
                            selectedMarker = updated,
                        )
                    }
                }
                .onFailure { e ->
                    updateData {
                        it.copy(
                            actionError = (e as? ApiException)?.getUserMessage()
                                ?: e.message
                                ?: "Ошибка техработ",
                        )
                    }
                }
        }
    }

    private fun toggleLayer(leafletLayer: String) {
        updateData { data ->
            val next = if (leafletLayer in data.visibleLayers) {
                data.visibleLayers - leafletLayer
            } else {
                data.visibleLayers + leafletLayer
            }
            data.copy(visibleLayers = next)
        }
    }

    private fun toggleGroup(groupId: String, enabled: Boolean) {
        updateData { data ->
            val layers = data.legend.groups
                .firstOrNull { it.group.id == groupId }
                ?.items
                ?.mapNotNull { it.item.leafletLayer }
                .orEmpty()
            if (layers.isEmpty()) return@updateData data
            val next = if (enabled) {
                data.visibleLayers + layers
            } else {
                data.visibleLayers - layers.toSet()
            }
            data.copy(visibleLayers = next)
        }
    }

    private fun currentDataOrNull(): MapUiData? =
        (_state.value.contentState as? UiState.Success)?.data

    private fun updateData(transform: (MapUiData) -> MapUiData) {
        val content = _state.value.contentState
        if (content is UiState.Success) {
            _state.value = _state.value.copy(contentState = content.copy(data = transform(content.data)))
        }
    }

    private fun mergeVisibility(
        previousVisible: Set<String>,
        previousAll: Set<String>,
        newAll: Set<String>,
    ): Set<String> {
        if (previousAll.isEmpty()) return newAll
        return newAll.filter { layer ->
            if (layer !in previousAll) true else layer in previousVisible
        }.toSet()
    }
}
