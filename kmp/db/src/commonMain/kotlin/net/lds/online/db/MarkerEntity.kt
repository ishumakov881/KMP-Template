package net.lds.online.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted lean map marker. One row per point; [type] scopes markgen replace batches.
 */
@Entity(
    tableName = "markers",
    indices = [Index(value = ["type"]), Index(value = ["layer"])],
)
data class MarkerEntity(
    @PrimaryKey val stableId: String,
    val type: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val state: Int,
    val layer: String,
    val icon: String?,
    val ip: String?,
    val pingType: String?,
    val siteId: Int?,
    val siteLabel: String?,
    val place: String?,
    val deviceModel: String?,
    val serial: String?,
    val mac: String?,
    val maintenanceId: String?,
    val maintenanceActive: Boolean,
    val maintenanceText: String?,
    val entityId: String?,
) {
    companion object {
        /** Stable across markgen refreshes when server ids/ip/coords stay the same. */
        fun stableId(
            type: String,
            entityId: String?,
            ip: String?,
            lat: Double,
            lng: Double,
            name: String,
        ): String {
            val key = entityId?.takeIf { it.isNotBlank() }
                ?: ip?.takeIf { it.isNotBlank() }
                ?: "$lat,$lng,$name"
            return "$type|$key"
        }
    }
}
