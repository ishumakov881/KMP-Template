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

    /**
     * Plain-text payload for mobile share/clipboard.
     * Only non-blank fields from markgen/popup — nothing invented.
     */
    fun formatShareText(baseUrl: String = NosEndpoints.BASE_URL): String = buildString {
        fun line(label: String, value: String?) {
            val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return
            append(label).append(": ").append(v).append('\n')
        }
        line("Имя", name)
        line("Тип", type)
        line("Слой", layer)
        line("IP", ip)
        line("Узел", siteLabel)
        siteId?.let { line("ID узла", it.toString()) }
        line("Место", place)
        line("Модель", deviceModel)
        line("Серийник", serial)
        line("MAC", mac)
        line("Entity id", entityId)
        if (maintenanceActive) {
            line("Техработы", maintenanceText ?: "активны")
        }
        append("Координаты: ").append(lat).append(", ").append(lng).append('\n')
        val root = baseUrl.trimEnd('/')
        siteId?.let { append("Узел URL: ").append(root).append("/sw/").append(it).append('\n') }
        ip?.let { ipValue ->
            append("Графики: ").append(root).append("/sw/pic/").append(ipValue).append('\n')
            append("SNMP: ").append(root).append("/sw/snmp/").append(ipValue).append('\n')
        }
    }.trimEnd()
}
