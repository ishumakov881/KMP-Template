package net.lds.online.network.nos

import kotlinx.coroutines.flow.Flow

/**
 * Offline cache for map markers.
 * - Android / iOS / Desktop: [net.lds.online.db.RoomMarkerOfflineStore] (`:kmp:db` commonMain).
 * - JS / Wasm: [NoOpMarkerOfflineStore] (Room not available).
 */
interface MarkerOfflineStore {
    fun observeAll(): Flow<List<LeanMapMarker>>

    suspend fun replaceType(type: String, markers: List<LeanMapMarker>)

    suspend fun upsert(marker: LeanMapMarker)
}
