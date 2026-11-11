package online.lds.nos.ui.map

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.lds.online.network.ApiException
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.legend.MapLegendSnapshot
import net.lds.online.network.nos.legend.NosMapLegendCatalog
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel
import net.lds.online.ui.base.ScreenState
import net.lds.online.ui.base.UiState
import net.lds.online.network.nos.MarkerOfflineStore
import online.lds.nos.data.AppPreferences

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
    /**
     * Markgen still in flight (initial or refresh). Map shell stays visible;
     * legend toggles stay inactive until the first successful type batch.
     */
    val markersLoading: Boolean = false,
    /** Prefs [AppPreferences.KEY_OFFLINE_CACHE] when a [MarkerOfflineStore] is available. */
    val offlineCacheEnabled: Boolean = false,
) {
    val visibleMarkers: List<LeanMapMarker>
        get() = markers.filter { it.layer in visibleLayers }

    val allLegendLayers: Set<String>
        get() = legend.groups
            .flatMap { group -> group.items.mapNotNull { it.item.leafletLayer } }
            .toSet()

    /** Legend checkboxes: inactive while first paint still waiting for any markers. */
    val legendInteractive: Boolean
        get() = !markersLoading || markers.isNotEmpty()
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
    data class SetOfflineCache(val enabled: Boolean) : MapEvent
}

class MapViewModel(
    private val mapRepository: NosMapRepository,
    private val appPreferences: AppPreferences,
    private val offlineStore: MarkerOfflineStore? = null,
    /** Defaults to full web markgen set from `map_osm.js` (sw/uzel/pon/mft/wifi/usil/amp/lines). */
    private val enabledTypes: List<String> = NosMapRepository.DEFAULT_MAP_TYPES,
) : BaseViewModel<MapUiData, MapEvent>() {

    private var refreshJob: Job? = null
    private var loadJob: Job? = null
    private var offlineObserveJob: Job? = null
    private val mergeMutex = Mutex()

    init {
        // Shell first (tiles + legend stub), then stream markgen types like the web map.
        _state.value = ScreenState(contentState = UiState.Success(emptyShell()))
        if (isOfflineEnabled()) startOfflineObserve()
        loadMarkersProgressive(isRefreshing = false)
        restartAutoRefresh(MapRefreshInterval.WebDefault)
    }

    /**
     * Fallback for [BaseViewModel.loadData] — map UI uses [loadMarkersProgressive] instead
     * so the screen never sits on a full-screen Loading shimmer waiting for all 9 types.
     */
    override suspend fun onFetchData(): MapUiData {
        val previous = currentDataOrNull() ?: emptyShell()
        val types = enabledTypes.distinct()
        val markers = mapRepository.markgenMany(types)
        return buildData(
            previous = previous,
            markers = markers,
            loadedTypes = types.toSet(),
            markersLoading = false,
        )
    }

    override fun onBusinessEvent(event: MapEvent) {
        when (event) {
            MapEvent.Refresh -> loadMarkersProgressive(isRefreshing = true)
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
            is MapEvent.SetOfflineCache -> setOfflineCache(event.enabled)
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

    override fun onCleared() {
        loadJob?.cancel()
        refreshJob?.cancel()
        offlineObserveJob?.cancel()
        super.onCleared()
    }

    private fun isOfflineEnabled(): Boolean =
        offlineStore != null && appPreferences.isOfflineCacheEnabled()

    private fun setOfflineCache(enabled: Boolean) {
        if (offlineStore == null) {
            updateData { it.copy(offlineCacheEnabled = false) }
            return
        }
        appPreferences.setOfflineCacheEnabled(enabled)
        updateData { it.copy(offlineCacheEnabled = enabled) }
        if (enabled) {
            startOfflineObserve()
            loadMarkersProgressive(isRefreshing = true)
        } else {
            offlineObserveJob?.cancel()
            offlineObserveJob = null
        }
    }

    private fun startOfflineObserve() {
        val store = offlineStore ?: return
        offlineObserveJob?.cancel()
        offlineObserveJob = viewModelScope.launch {
            store.observeAll().collect { cached ->
                mergeMutex.withLock {
                    val previous = currentDataOrNull() ?: emptyShell()
                    val next = buildData(
                        previous = previous,
                        markers = cached,
                        loadedTypes = cached.map { it.type }.toSet(),
                        markersLoading = previous.markersLoading && cached.isEmpty(),
                    )
                    _state.value = _state.value.copy(contentState = UiState.Success(next))
                }
            }
        }
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
                loadMarkersProgressive(isRefreshing = true)
            }
        }
    }

    /**
     * Progressive markgen: keep map/legend shell on screen; merge each type as it returns.
     */
    private fun loadMarkersProgressive(@Suppress("UNUSED_PARAMETER") isRefreshing: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            ensureSuccessShell()
            val types = enabledTypes.distinct()
            var successCount = 0
            var lastError: Exception? = null

            coroutineScope {
                types.map { type ->
                    async {
                        runCatching { mapRepository.markgen(type) }
                            .onSuccess { list ->
                                mergeMutex.withLock {
                                    applyTypeBatch(type, list)
                                    successCount++
                                }
                            }
                            .onFailure { e ->
                                lastError = e as? Exception ?: Exception(e)
                            }
                    }
                }.awaitAll()
            }

            mergeMutex.withLock {
                val data = currentDataOrNull()
                if (successCount == 0 && data?.markers.isNullOrEmpty()) {
                    val apiEx = (lastError as? ApiException) ?: lastError?.let { ApiException.from(it) }
                    _state.value = ScreenState(
                        contentState = UiState.Error(
                            apiEx?.getUserMessage() ?: lastError?.message ?: "Ошибка загрузки карты",
                        ),
                    )
                } else {
                    updateData { it.copy(markersLoading = false) }
                }
            }
        }
    }

    private fun ensureSuccessShell() {
        if (currentDataOrNull() == null) {
            // Hard Error → retry, or unexpected non-Success: recreate tiles+legend shell.
            _state.value = ScreenState(contentState = UiState.Success(emptyShell()))
            return
        }
        // Refresh keeps previous markers on the map while types re-stream.
        updateData { it.copy(markersLoading = true) }
    }

    private fun emptyShell(): MapUiData {
        val catalogTypes = enabledTypes.toSet()
        val legend = NosMapLegendCatalog.snapshotFor(
            loadedTypes = catalogTypes,
            markers = emptyList(),
        )
        val allLayers = legend.groups
            .flatMap { group -> group.items.mapNotNull { it.item.leafletLayer } }
            .toSet()
        return MapUiData(
            markers = emptyList(),
            loadedTypes = emptySet(),
            legend = legend,
            visibleLayers = visibleLayersFromPrefs(allLayers),
            markersLoading = true,
            offlineCacheEnabled = isOfflineEnabled(),
        )
    }

    /** Restore legend checkboxes; unknown/new layers default to visible. */
    private fun visibleLayersFromPrefs(allLayers: Set<String>): Set<String> {
        val hidden = appPreferences.getMapHiddenLayers() ?: return allLayers
        return allLayers - hidden
    }

    private fun persistVisibleLayers(data: MapUiData) {
        val hidden = data.allLegendLayers - data.visibleLayers
        appPreferences.putMapHiddenLayers(hidden)
    }

    private suspend fun applyTypeBatch(type: String, batch: List<LeanMapMarker>) {
        if (isOfflineEnabled()) {
            offlineStore?.replaceType(type, batch)
            // UI comes from Room Flow when offline cache is on.
            return
        }
        val previous = currentDataOrNull() ?: emptyShell()
        val markers = previous.markers.filter { it.type != type } + batch
        val loaded = previous.loadedTypes + type
        val next = buildData(
            previous = previous,
            markers = markers,
            loadedTypes = loaded,
            markersLoading = true,
        )
        _state.value = _state.value.copy(contentState = UiState.Success(next))
    }

    private fun buildData(
        previous: MapUiData,
        markers: List<LeanMapMarker>,
        loadedTypes: Set<String>,
        markersLoading: Boolean,
    ): MapUiData {
        // Legend stub uses full enabled set so groups appear before their markgen returns.
        val legend = NosMapLegendCatalog.snapshotFor(
            loadedTypes = enabledTypes.toSet(),
            markers = markers,
        )
        val allLayers = legend.groups
            .flatMap { group -> group.items.mapNotNull { it.item.leafletLayer } }
            .toSet()
        val visibleLayers = mergeVisibility(
            previousVisible = previous.visibleLayers,
            previousAll = previous.allLegendLayers,
            newAll = allLayers,
        )
        val selected = previous.selectedMarker?.let { prev ->
            markers.firstOrNull { it.samePointAs(prev) }
        }
        return MapUiData(
            markers = markers,
            loadedTypes = loadedTypes,
            legend = legend,
            visibleLayers = visibleLayers,
            refreshInterval = previous.refreshInterval,
            selectedMarker = selected,
            markersLoading = markersLoading,
            offlineCacheEnabled = previous.offlineCacheEnabled,
        )
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
                    val updated = marker.copy(
                        maintenanceActive = enabled,
                        maintenanceText = message.takeIf { enabled && it.isNotBlank() },
                    )
                    if (isOfflineEnabled()) {
                        offlineStore?.upsert(updated)
                        updateData { it.copy(selectedMarker = updated) }
                    } else {
                        updateData { data ->
                            data.copy(
                                markers = data.markers.map {
                                    if (it.samePointAs(marker)) updated else it
                                },
                                selectedMarker = updated,
                            )
                        }
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
            data.copy(visibleLayers = next).also { persistVisibleLayers(it) }
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
            data.copy(visibleLayers = next).also { persistVisibleLayers(it) }
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
