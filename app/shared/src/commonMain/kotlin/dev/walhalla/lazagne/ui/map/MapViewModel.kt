package dev.walhalla.lazagne.ui.map

import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.legend.MapLegendSnapshot
import net.lds.online.network.nos.legend.NosMapLegendCatalog
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel

data class MapUiData(
    val markers: List<LeanMapMarker> = emptyList(),
    val loadedTypes: Set<String> = emptySet(),
    val legend: MapLegendSnapshot = MapLegendSnapshot.Empty,
)

sealed interface MapEvent : BaseEvent {
    data object Refresh : MapEvent
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
        val types = enabledTypes.distinct()
        val markers = mapRepository.markgenMany(types)
        val loaded = types.toSet()
        return MapUiData(
            markers = markers,
            loadedTypes = loaded,
            legend = NosMapLegendCatalog.snapshotFor(
                loadedTypes = loaded,
                markers = markers,
            ),
        )
    }

    override fun onBusinessEvent(event: MapEvent) {
        when (event) {
            MapEvent.Refresh -> loadData(isRefreshing = true)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit
}
