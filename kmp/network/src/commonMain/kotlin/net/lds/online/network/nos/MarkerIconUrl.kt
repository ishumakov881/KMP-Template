package net.lds.online.network.nos

import net.lds.online.network.nos.legend.MapLegendItem
import net.lds.online.network.nos.legend.NosMapLegendCatalog

/**
 * Resolves display icon URL for a marker: markgen `icon` first, else legend catalog by `layer`.
 */
fun LeanMapMarker.resolvedIconUrl(baseUrl: String = NosEndpoints.BASE_URL): String? {
    val path = icon?.takeIf { it.isNotBlank() }
        ?: NosMapLegendCatalog.findItem(layer)?.iconPath
        ?: return null
    return absoluteNosAssetUrl(path, baseUrl)
}

fun absoluteNosAssetUrl(path: String, baseUrl: String = NosEndpoints.BASE_URL): String {
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}

fun MapLegendItem.resolvedIconUrl(baseUrl: String = NosEndpoints.BASE_URL): String? =
    iconPath?.let { absoluteNosAssetUrl(it, baseUrl) }
