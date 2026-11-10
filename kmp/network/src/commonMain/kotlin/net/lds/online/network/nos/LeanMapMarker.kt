package net.lds.online.network.nos

/**
 * Lean marker for map/list — no raw HTML `popupContent` in UI state.
 * Structured popup fields are extracted at parse time (web popup parity).
 */
data class LeanMapMarker(
    val name: String,
    val lat: Double,
    val lng: Double,
    val state: Int,
    val layer: String,
    val icon: String?,
    val type: String,
    /** Device IP from popup / markgen. */
    val ip: String? = null,
    /** Web `data-t` for ajaxping (`s` switch, `w` wifi, `u` uzel, …). */
    val pingType: String? = null,
    /** `/sw/{siteId}` from popup node link. */
    val siteId: Int? = null,
    val siteLabel: String? = null,
    val place: String? = null,
    val deviceModel: String? = null,
    val serial: String? = null,
    val mac: String? = null,
    /** Web `data-nb_id` for maintenance. */
    val maintenanceId: String? = null,
    val maintenanceActive: Boolean = false,
    val maintenanceText: String? = null,
    /** MFT / entity id when present in markgen JSON. */
    val entityId: String? = null,
) {
    fun samePointAs(other: LeanMapMarker): Boolean =
        lat == other.lat && lng == other.lng && name == other.name && type == other.type
}
