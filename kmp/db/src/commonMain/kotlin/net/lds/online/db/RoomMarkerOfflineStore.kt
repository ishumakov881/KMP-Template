package net.lds.online.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.MarkerOfflineStore

/**
 * Real offline store — lives in `:kmp:db` **commonMain** and ships to all Room
 * targets (Android / iOS / Desktop JVM). JS/Wasm use [net.lds.online.network.nos.NoOpMarkerOfflineStore].
 */
class RoomMarkerOfflineStore(
    private val dao: MarkerDao,
) : MarkerOfflineStore {

    override fun observeAll(): Flow<List<LeanMapMarker>> =
        dao.observeAll().map { rows -> rows.map { it.toLean() } }

    override suspend fun replaceType(type: String, markers: List<LeanMapMarker>) {
        dao.replaceType(type, markers.map { it.toEntity() })
    }

    override suspend fun upsert(marker: LeanMapMarker) {
        dao.upsert(marker.toEntity())
    }
}

private fun LeanMapMarker.toEntity(): MarkerEntity {
    val id = MarkerEntity.stableId(
        type = type,
        entityId = entityId,
        ip = ip,
        lat = lat,
        lng = lng,
        name = name,
    )
    return MarkerEntity(
        stableId = id,
        type = type,
        name = name,
        lat = lat,
        lng = lng,
        state = state,
        layer = layer,
        icon = icon,
        ip = ip,
        pingType = pingType,
        siteId = siteId,
        siteLabel = siteLabel,
        place = place,
        deviceModel = deviceModel,
        serial = serial,
        mac = mac,
        maintenanceId = maintenanceId,
        maintenanceActive = maintenanceActive,
        maintenanceText = maintenanceText,
        entityId = entityId,
    )
}

private fun MarkerEntity.toLean(): LeanMapMarker =
    LeanMapMarker(
        name = name,
        lat = lat,
        lng = lng,
        state = state,
        layer = layer,
        icon = icon,
        type = type,
        ip = ip,
        pingType = pingType,
        siteId = siteId,
        siteLabel = siteLabel,
        place = place,
        deviceModel = deviceModel,
        serial = serial,
        mac = mac,
        maintenanceId = maintenanceId,
        maintenanceActive = maintenanceActive,
        maintenanceText = maintenanceText,
        entityId = entityId,
    )
