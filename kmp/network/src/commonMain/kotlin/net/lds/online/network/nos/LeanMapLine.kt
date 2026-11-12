package net.lds.online.network.nos

/**
 * Lean map connection line (web markgen `type=line` / `mft_line`).
 *
 * Server returns a flat segment with two endpoints (`lat1,lng1 -> lat2,lng2`) and
 * **no** `layer`/`color` — the layer is derived from the markgen type
 * (`line` -> `layer_line`, `mft_line` -> `layer_mft_line`), like the web Leaflet polyline.
 */
data class LeanMapLine(
    val lat1: Double,
    val lng1: Double,
    val lat2: Double,
    val lng2: Double,
    /** Leaflet layer for legend checkbox visibility (`layer_line` / `layer_mft_line`). */
    val layer: String,
    /** markgen `type` that produced this segment (`line` / `mft_line`). */
    val type: String,
)
