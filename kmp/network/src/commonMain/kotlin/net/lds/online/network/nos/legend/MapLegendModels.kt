package net.lds.online.network.nos.legend

/**
 * Static map legend mirrored from web `/osm` HTML (`#more_legend`).
 * Not loaded from API — only icons/labels; counts come from loaded markers / get_count later.
 */
sealed interface MapLegendNode {
    val id: String
    val title: String
}

/**
 * One legend row (checkbox + icon + tooltip on web).
 *
 * @param leafletLayer value of markgen `layer` field (e.g. `layer_uzel_ok`), or null for non-marker rows
 * @param iconPath site-relative path under nos.lds.online, or null for text-only rows
 * @param markgenType ajax `type` that produces this layer (`uzel`, `sw`, …)
 */
data class MapLegendItem(
    override val id: String,
    override val title: String,
    val iconPath: String?,
    val leafletLayer: String?,
    val markgenType: String?,
) : MapLegendNode {

    fun iconUrl(baseUrl: String = DEFAULT_BASE_URL): String? {
        val path = iconPath?.takeIf { it.isNotBlank() } ?: return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return baseUrl.trimEnd('/') + "/" + path.trimStart('/')
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://nos.lds.online"
    }
}

/**
 * Collapsible legend section (Коммутаторы / Узлы / PON / …).
 */
data class MapLegendGroup(
    override val id: String,
    override val title: String,
    val markgenType: String?,
    val items: List<MapLegendItem>,
) : MapLegendNode {

    fun itemByLeafletLayer(layer: String): MapLegendItem? =
        items.firstOrNull { it.leafletLayer == layer }
}

/**
 * Runtime view of a legend row with optional live count from loaded markers.
 */
data class MapLegendItemState(
    val item: MapLegendItem,
    val count: Int,
)

data class MapLegendGroupState(
    val group: MapLegendGroup,
    val items: List<MapLegendItemState>,
)

data class MapLegendSnapshot(
    val groups: List<MapLegendGroupState>,
) {
    val isEmpty: Boolean get() = groups.isEmpty()

    companion object {
        val Empty = MapLegendSnapshot(emptyList())
    }
}
