package online.lds.nos.ui.sites

import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwDevice
import net.lds.online.network.nos.SwSitePage
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel

data class SiteDetailUiData(
    val page: SwSitePage,
) {
    val devices: List<SwDevice> get() = page.devices
    val title: String get() = page.title
}

sealed interface SiteDetailEvent : BaseEvent {
    data object Refresh : SiteDetailEvent
}

class SiteDetailViewModel(
    private val swRepository: NosSwRepository,
    private val siteId: Int,
    private val fallbackTitle: String,
) : BaseViewModel<SiteDetailUiData, SiteDetailEvent>() {

    init {
        loadData()
    }

    override suspend fun onFetchData(): SiteDetailUiData {
        val page = swRepository.sitePage(siteId)
        val title = page.title.ifBlank { fallbackTitle }.ifBlank { "Узел $siteId" }
        return SiteDetailUiData(page = page.copy(title = title))
    }

    override fun onBusinessEvent(event: SiteDetailEvent) {
        when (event) {
            SiteDetailEvent.Refresh -> loadData(isRefreshing = true)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit
}
