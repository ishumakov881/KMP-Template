package online.lds.nos.ui.switches

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.lds.online.network.ApiException
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
    /** Same sheet as map marker popup. */
    val selectedMarker: LeanMapMarker? = null,
    val pingLoading: Boolean = false,
    val pingResult: String? = null,
    val actionError: String? = null,
    val maintenancePromptVisible: Boolean = false,
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
    data class SelectMarker(val marker: LeanMapMarker) : SwitchesEvent
    data object DismissPopup : SwitchesEvent
    data object PingSelected : SwitchesEvent
    data object DismissPingResult : SwitchesEvent
    data object DismissActionError : SwitchesEvent
    data object RequestEnableMaintenance : SwitchesEvent
    data object DismissMaintenancePrompt : SwitchesEvent
    data class ConfirmEnableMaintenance(val message: String) : SwitchesEvent
    data object DisableMaintenance : SwitchesEvent
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
        val selected = previous?.selectedMarker?.let { prev ->
            items.firstOrNull { it.samePointAs(prev) }
        }
        return SwitchesUiData(
            items = items.sortedBy { it.name.lowercase() },
            query = previous?.query.orEmpty(),
            filter = previous?.filter ?: SwitchStatusFilter.All,
            selectedMarker = selected,
        )
    }

    override fun onBusinessEvent(event: SwitchesEvent) {
        when (event) {
            SwitchesEvent.Refresh -> loadData(isRefreshing = true)
            is SwitchesEvent.QueryChanged -> updateData { it.copy(query = event.value) }
            is SwitchesEvent.FilterChanged -> updateData { it.copy(filter = event.filter) }
            is SwitchesEvent.SelectMarker -> updateData {
                it.copy(
                    selectedMarker = event.marker,
                    pingResult = null,
                    actionError = null,
                    maintenancePromptVisible = false,
                )
            }
            SwitchesEvent.DismissPopup -> updateData {
                it.copy(
                    selectedMarker = null,
                    pingResult = null,
                    actionError = null,
                    maintenancePromptVisible = false,
                    pingLoading = false,
                )
            }
            SwitchesEvent.PingSelected -> pingSelected()
            SwitchesEvent.DismissPingResult -> updateData { it.copy(pingResult = null) }
            SwitchesEvent.DismissActionError -> updateData { it.copy(actionError = null) }
            SwitchesEvent.RequestEnableMaintenance -> updateData {
                it.copy(maintenancePromptVisible = true)
            }
            SwitchesEvent.DismissMaintenancePrompt -> updateData {
                it.copy(maintenancePromptVisible = false)
            }
            is SwitchesEvent.ConfirmEnableMaintenance ->
                setMaintenance(enabled = true, message = event.message)
            SwitchesEvent.DisableMaintenance -> setMaintenance(enabled = false, message = "")
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

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
                            items = data.items.map { if (it.samePointAs(marker)) updated else it },
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

    private fun currentDataOrNull(): SwitchesUiData? =
        (_state.value.contentState as? UiState.Success)?.data

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
