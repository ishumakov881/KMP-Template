package online.lds.nos.ui.sites

import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwPicPage
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel

data class SwPicUiData(
    val page: SwPicPage,
)

sealed interface SwPicEvent : BaseEvent {
    data object Refresh : SwPicEvent
}

class SwPicViewModel(
    private val swRepository: NosSwRepository,
    private val path: String,
    private val fallbackTitle: String,
) : BaseViewModel<SwPicUiData, SwPicEvent>() {

    init {
        loadData()
    }

    override suspend fun onFetchData(): SwPicUiData {
        val page = swRepository.picPage(path)
        val title = page.title.ifBlank { fallbackTitle }.ifBlank { path }
        return SwPicUiData(page = page.copy(title = title))
    }

    override fun onBusinessEvent(event: SwPicEvent) {
        when (event) {
            SwPicEvent.Refresh -> loadData(isRefreshing = true)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit
}
