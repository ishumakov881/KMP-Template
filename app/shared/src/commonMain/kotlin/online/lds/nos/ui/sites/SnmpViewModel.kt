package online.lds.nos.ui.sites

import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwSnmpPage
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel

data class SnmpUiData(
    val page: SwSnmpPage,
)

sealed interface SnmpEvent : BaseEvent {
    data object Refresh : SnmpEvent
}

class SnmpViewModel(
    private val swRepository: NosSwRepository,
    private val ip: String,
) : BaseViewModel<SnmpUiData, SnmpEvent>() {

    init {
        loadData()
    }

    override suspend fun onFetchData(): SnmpUiData =
        SnmpUiData(page = swRepository.snmpPage(ip))

    override fun onBusinessEvent(event: SnmpEvent) {
        when (event) {
            SnmpEvent.Refresh -> loadData(isRefreshing = true)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit
}
