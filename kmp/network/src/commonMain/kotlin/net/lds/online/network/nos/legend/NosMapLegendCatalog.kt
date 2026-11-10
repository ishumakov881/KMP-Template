package net.lds.online.network.nos.legend

import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository

/**
 * Canonical legend catalog (web parity). Single source for UI legend and layer metadata.
 */
object NosMapLegendCatalog {

    val switches = MapLegendGroup(
        id = "switch_all",
        title = "Коммутаторы",
        markgenType = NosMapRepository.TYPE_SW,
        items = listOf(
            item("ok", "Свич доступен", "/images/map/mm_20_green.png", "layer_ok", NosMapRepository.TYPE_SW),
            item("dn", "Свич не доступен", "/images/map/mm_20_red.png", "layer_dn", NosMapRepository.TYPE_SW),
            item("ld", "На свиче есть Linkdown", "/images/map/mm_20_yellow.png", "layer_ld", NosMapRepository.TYPE_SW),
            item("nv", "Скрытые свичи", "/images/map/mm_20_grey.png", "layer_nv", NosMapRepository.TYPE_SW),
        ),
    )

    val nodes = MapLegendGroup(
        id = "uzel_all",
        title = "Узлы",
        markgenType = NosMapRepository.TYPE_UZEL,
        items = listOf(
            item("uzel_ok", "Узел доступен", "/images/map/green.png", "layer_uzel_ok", NosMapRepository.TYPE_UZEL),
            item("uzel_dn", "Узел не доступен", "/images/map/red.png", "layer_uzel_dn", NosMapRepository.TYPE_UZEL),
            item("uzel_ld", "На узле есть Linkdown", "/images/map/yellow.png", "layer_uzel_ld", NosMapRepository.TYPE_UZEL),
            item("line", "Связи", iconPath = null, leafletLayer = "layer_line", markgenType = NosMapRepository.TYPE_LINE),
        ),
    )

    val pon = MapLegendGroup(
        id = "pon",
        title = "PON",
        markgenType = NosMapRepository.TYPE_PON,
        items = listOf(
            item("pon_ok", "Pon доступен", "/images/map/mm_36_green.png", "layer_pon_ok", NosMapRepository.TYPE_PON),
            item("pon_ld", "Pon Link Down", "/images/map/mm_36_yellow.png", "layer_pon_ld", NosMapRepository.TYPE_PON),
            item("pon_dn", "Pon не доступен", "/images/map/mm_36_red.png", "layer_pon_dn", NosMapRepository.TYPE_PON),
            item("pon_mft", "Муфты", "/images/map/pon.png", "layer_pon_mft", NosMapRepository.TYPE_MFT),
            item("pon_mft_line", "Связи MFT", null, "layer_mft_line", NosMapRepository.TYPE_MFT_LINE),
        ),
    )

    val ktv = MapLegendGroup(
        id = "usil",
        title = "КТВ",
        markgenType = NosMapRepository.TYPE_USIL,
        items = listOf(
            item("usil_ok", "сигнал от -4 до +1", "/images/map/t_20_green.png", "layer_usil_ok", NosMapRepository.TYPE_USIL),
            item("usil_dn", "усилитель не доступен", "/images/map/t_20_red.png", "layer_usil_dn", NosMapRepository.TYPE_USIL),
            item(
                "usil_yellow",
                "сигнал от +1 до +2 или от -4 до -6",
                "/images/map/t_20_yel.png",
                "layer_usil_yellow",
                NosMapRepository.TYPE_USIL,
            ),
            item(
                "usil_brown",
                "сигнал меньше выше +2 или выше -6",
                "/images/map/t_20_brown.png",
                "layer_usil_brown",
                NosMapRepository.TYPE_USIL,
            ),
            item("amp_ok", "сигнал от -4 до 6", "/images/map/amp_green.png", "layer_amp_ok", NosMapRepository.TYPE_KTV_AMP),
            item("amp_dn", "усилитель не доступен", "/images/map/amp_red.png", "layer_amp_dn", NosMapRepository.TYPE_KTV_AMP),
            item(
                "amp_yellow",
                "сигнал от -6 до -4 или от 6 до 8",
                "/images/map/amp_yellow.png",
                "layer_amp_yellow",
                NosMapRepository.TYPE_KTV_AMP,
            ),
            item(
                "amp_brown",
                "сигнал меньше -6 или больше 8",
                "/images/map/amp_brown.png",
                "layer_amp_brown",
                NosMapRepository.TYPE_KTV_AMP,
            ),
        ),
    )

    val wifi = MapLegendGroup(
        id = "wifi",
        title = "WIFI",
        markgenType = NosMapRepository.TYPE_WIFI,
        items = listOf(
            item("wifi_ok", "wifi доступен", "/images/map/wi-fi_green.png", "layer_wifi_ok", NosMapRepository.TYPE_WIFI),
            item("wifi_dn", "wifi не доступен", "/images/map/wi-fi_red.png", "layer_wifi_dn", NosMapRepository.TYPE_WIFI),
        ),
    )

    val allGroups: List<MapLegendGroup> = listOf(switches, nodes, pon, ktv, wifi)

    /** All markgen types referenced by the legend — must cover [NosMapRepository.DEFAULT_MAP_TYPES]. */
    val markgenTypesInLegend: List<String> =
        allGroups
            .flatMap { group ->
                listOfNotNull(group.markgenType) + group.items.mapNotNull { it.markgenType }
            }
            .distinct()

    fun groupForMarkgenType(type: String): MapLegendGroup? =
        allGroups.firstOrNull { it.markgenType == type }
            ?: allGroups.firstOrNull { group -> group.items.any { it.markgenType == type } }

    fun findItem(leafletLayer: String): MapLegendItem? =
        allGroups.asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.leafletLayer == leafletLayer }

    /**
     * Layers present in [markers] but missing from the static catalog.
     * Title/icon taken from markgen (`layer` / `icon`).
     */
    fun discoverUnknownLayers(markers: List<LeanMapMarker>): List<MapLegendItem> =
        markers.asSequence()
            .filter { it.layer.isNotBlank() }
            .groupBy { it.layer }
            .filterKeys { findItem(it) == null }
            .map { (layer, samples) ->
                val sample = samples.first()
                MapLegendItem(
                    id = "discovered_$layer",
                    title = humanizeLayer(layer),
                    iconPath = sample.icon,
                    leafletLayer = layer,
                    markgenType = sample.type.ifBlank { null },
                )
            }

    /**
     * Legend for loaded markgen types: static catalog rows + any new `layer` values from markers.
     */
    fun snapshotFor(
        loadedTypes: Set<String>,
        markers: List<LeanMapMarker>,
    ): MapLegendSnapshot {
        if (loadedTypes.isEmpty() && markers.isEmpty()) return MapLegendSnapshot.Empty
        val counts = markers.groupingBy { it.layer }.eachCount()
        val discovered = discoverUnknownLayers(markers)
        val claimedDiscovered = mutableSetOf<String>()

        val groups = mutableListOf<MapLegendGroupState>()
        allGroups.forEach { group ->
            val catalogItems = group.items.filter { item ->
                val type = item.markgenType ?: group.markgenType
                type != null && type in loadedTypes
            }
            val extras = discovered.filter { d ->
                belongsToGroup(d, group)
            }
            extras.forEach { d -> d.leafletLayer?.let { claimedDiscovered.add(it) } }
            val merged = catalogItems + extras
            if (merged.isEmpty()) return@forEach
            groups += MapLegendGroupState(
                group = group,
                items = merged.map { item ->
                    MapLegendItemState(
                        item = item,
                        count = item.leafletLayer?.let { counts[it] } ?: 0,
                    )
                },
            )
        }

        val orphans = discovered.filter { d ->
            val key = d.leafletLayer
            key != null && key !in claimedDiscovered
        }
        if (orphans.isNotEmpty()) {
            val other = MapLegendGroup(
                id = "discovered_other",
                title = "Другие",
                markgenType = null,
                items = orphans,
            )
            groups += MapLegendGroupState(
                group = other,
                items = orphans.map { item ->
                    MapLegendItemState(
                        item = item,
                        count = item.leafletLayer?.let { counts[it] } ?: 0,
                    )
                },
            )
        }

        return if (groups.isEmpty()) MapLegendSnapshot.Empty else MapLegendSnapshot(groups)
    }

    private fun belongsToGroup(item: MapLegendItem, group: MapLegendGroup): Boolean {
        val type = item.markgenType ?: return false
        if (type == group.markgenType) return true
        return group.items.any { it.markgenType == type }
    }

    private fun humanizeLayer(layer: String): String =
        layer.removePrefix("layer_").replace('_', ' ').ifBlank { layer }

    private fun item(
        id: String,
        title: String,
        iconPath: String?,
        leafletLayer: String?,
        markgenType: String?,
    ) = MapLegendItem(
        id = id,
        title = title,
        iconPath = iconPath,
        leafletLayer = leafletLayer,
        markgenType = markgenType,
    )
}
