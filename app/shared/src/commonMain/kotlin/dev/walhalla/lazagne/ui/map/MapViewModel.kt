package dev.walhalla.lazagne.ui.map

import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel

data class MapUiData(
    val markers: List<LeanMapMarker> = emptyList(),
    val layerType: String = NosMapRepository.TYPE_UZEL,
)

sealed interface MapEvent : BaseEvent {
    data object Refresh : MapEvent
}

class MapViewModel(
    private val mapRepository: NosMapRepository,
    private val initialType: String = NosMapRepository.TYPE_UZEL,
) : BaseViewModel<MapUiData, MapEvent>() {

    init {
        loadData()
    }

    override suspend fun onFetchData(): MapUiData {
        val markers = mapRepository.markgen(initialType)
        return MapUiData(markers = markers, layerType = initialType)
    }

    override fun onBusinessEvent(event: MapEvent) {
        when (event) {
            MapEvent.Refresh -> loadData(isRefreshing = true)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit
}
