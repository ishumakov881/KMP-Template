package net.lds.online.network.nos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * JS / Wasm stub: no Room. Same [MarkerOfflineStore] API as
 * [net.lds.online.db.RoomMarkerOfflineStore] on Android / iOS / Desktop.
 */
object NoOpMarkerOfflineStore : MarkerOfflineStore {
    override fun observeAll(): Flow<List<LeanMapMarker>> = flowOf(emptyList())

    override suspend fun replaceType(type: String, markers: List<LeanMapMarker>) = Unit

    override suspend fun upsert(marker: LeanMapMarker) = Unit
}
