package dev.walhalla.lazagne.ui.map

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
}

class MapViewModel(
    private val mapRepository: NosMapRepository,
    /** Defaults to full web markgen set from `map_osm.js` (sw/uzel/pon/mft/wifi/usil/amp/lines). */
    private val enabledTypes: List<String> = NosMapRepository.DEFAULT_MAP_TYPES,
) : BaseViewModel<MapUiData, MapEvent>() {

    init {
        loadData()
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
        return MapUiData(
            markers = markers,
            loadedTypes = loaded,
            legend = legend,
            visibleLayers = visibleLayers,
        )
    }

    override fun onBusinessEvent(event: MapEvent) {
        when (event) {
            MapEvent.Refresh -> loadData(isRefreshing = true)
            is MapEvent.ToggleLayer -> toggleLayer(event.leafletLayer)
            is MapEvent.ToggleGroup -> toggleGroup(event.groupId, event.enabled)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

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
