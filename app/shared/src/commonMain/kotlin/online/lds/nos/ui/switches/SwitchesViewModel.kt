package online.lds.nos.ui.switches

import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.legend.NosMapLegendCatalog
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel
import net.lds.online.ui.base.UiState

enum class SwitchStatusFilter(val title: String, val leafletLayer: String?) {
    All("Все", null),
    Ok("OK", "layer_ok"),
    Down("DN", "layer_dn"),
    LinkDown("LD", "layer_ld"),
    Hidden("NV", "layer_nv"),
}

data class SwitchesUiData(
    val items: List<LeanMapMarker> = emptyList(),
    val query: String = "",
    val filter: SwitchStatusFilter = SwitchStatusFilter.All,
) {
    val visible: List<LeanMapMarker>
        get() {
            val q = query.trim()
            return items.filter { item ->
                val byStatus = filter.leafletLayer == null || item.layer == filter.leafletLayer
                val byQuery = q.isEmpty() ||
                    item.name.contains(q, ignoreCase = true) ||
                    item.layer.contains(q, ignoreCase = true) ||
                    statusTitle(item.layer).contains(q, ignoreCase = true)
                byStatus && byQuery
            }
        }
}

sealed interface SwitchesEvent : BaseEvent {
    data object Refresh : SwitchesEvent
    data class QueryChanged(val value: String) : SwitchesEvent
    data class FilterChanged(val filter: SwitchStatusFilter) : SwitchesEvent
}

class SwitchesViewModel(
    private val mapRepository: NosMapRepository,
) : BaseViewModel<SwitchesUiData, SwitchesEvent>() {

    init {
        loadData()
    }

    override suspend fun onFetchData(): SwitchesUiData {
        val previous = (_state.value.contentState as? UiState.Success)?.data
        val items = mapRepository.markgen(NosMapRepository.TYPE_SW)
        return SwitchesUiData(
            items = items.sortedBy { it.name.lowercase() },
            query = previous?.query.orEmpty(),
            filter = previous?.filter ?: SwitchStatusFilter.All,
        )
    }

    override fun onBusinessEvent(event: SwitchesEvent) {
        when (event) {
            SwitchesEvent.Refresh -> loadData(isRefreshing = true)
            is SwitchesEvent.QueryChanged -> updateData { it.copy(query = event.value) }
            is SwitchesEvent.FilterChanged -> updateData { it.copy(filter = event.filter) }
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

    private fun updateData(transform: (SwitchesUiData) -> SwitchesUiData) {
        val content = _state.value.contentState
        if (content is UiState.Success) {
            _state.value = _state.value.copy(contentState = content.copy(data = transform(content.data)))
        }
    }
}

fun statusTitle(layer: String): String =
    NosMapLegendCatalog.findItem(layer)?.title
        ?: layer.removePrefix("layer_").replace('_', ' ')
