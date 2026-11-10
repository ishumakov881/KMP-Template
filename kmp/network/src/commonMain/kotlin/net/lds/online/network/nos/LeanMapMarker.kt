package net.lds.online.network.nos

/**
 * Lean marker for map list (no HTML popupContent).
 * Groups = [layer] / [state], not separate entity copies.
 */
data class LeanMapMarker(
    val name: String,
    val lat: Double,
    val lng: Double,
    val state: Int,
    val layer: String,
    val icon: String?,
    val type: String,
)
