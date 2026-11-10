package online.lds.nos.ui.sites

import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwSite
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel
import net.lds.online.ui.base.UiState

data class SitesUiData(
    val sites: List<SwSite> = emptyList(),
    val query: String = "",
) {
    val visible: List<SwSite>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return sites
            return sites.filter { site ->
                site.name.contains(q, ignoreCase = true) ||
                    site.id.toString().contains(q)
            }
        }
}

sealed interface SitesEvent : BaseEvent {
    data object Refresh : SitesEvent
    data class QueryChanged(val value: String) : SitesEvent
}

class SitesViewModel(
    private val swRepository: NosSwRepository,
) : BaseViewModel<SitesUiData, SitesEvent>() {

    init {
        loadData()
    }

    override suspend fun onFetchData(): SitesUiData {
        val previous = (_state.value.contentState as? UiState.Success)?.data
        val sites = swRepository.sites().sortedBy { it.name.lowercase() }
        return SitesUiData(
            sites = sites,
            query = previous?.query.orEmpty(),
        )
    }

    override fun onBusinessEvent(event: SitesEvent) {
        when (event) {
            SitesEvent.Refresh -> loadData(isRefreshing = true)
            is SitesEvent.QueryChanged -> updateData { it.copy(query = event.value) }
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

    private fun updateData(transform: (SitesUiData) -> SitesUiData) {
        val content = _state.value.contentState
        if (content is UiState.Success) {
            _state.value = _state.value.copy(contentState = content.copy(data = transform(content.data)))
        }
    }
}
