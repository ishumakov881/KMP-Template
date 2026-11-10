package dev.walhalla.lazagne.ui.map

/**
 * Web `#more_intval` radio values from `/osm` (map_osm.js setInterval).
 */
enum class MapRefreshInterval(
    val millis: Long,
    val title: String,
) {
    TenSeconds(10_000L, "10 секунд"),
    OneMinute(60_000L, "1 минута"),
    FiveMinutes(300_000L, "5 минут");

    companion object {
        /** Web default: `intVal5` checked, `setInterval(Refresh, 300000)`. */
        val WebDefault: MapRefreshInterval = FiveMinutes
    }
}
