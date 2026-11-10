package dev.walhalla.lazagne.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.resolvedIconUrl
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private val LdsHotTiles = XYTileSource(
    "LDS_HOT",
    1,
    19,
    256,
    ".png",
    arrayOf("https://maps.lds.online/hot/"),
)

private fun ensureOsmdroidConfig(context: Context) {
    val cfg = Configuration.getInstance()
    if (cfg.userAgentValue.isNullOrBlank() || cfg.userAgentValue == "osmdroid") {
        cfg.userAgentValue = context.packageName
        val base = File(context.cacheDir, "osmdroid")
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles")
    }
}

@Composable
actual fun NosMapCanvas(
    markers: List<LeanMapMarker>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val iconCache = remember { MarkerIconCache(context.applicationContext) }
    var iconsVersion by remember { mutableIntStateOf(0) }

    val mapView = remember {
        ensureOsmdroidConfig(context.applicationContext)
        MapView(context).apply {
            setTileSource(LdsHotTiles)
            setMultiTouchControls(true)
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(48.4422, 39.3544))
        }
    }

    LaunchedEffect(markers) {
        val urls = markers.mapNotNull { it.resolvedIconUrl() }.distinct()
        iconCache.ensureLoaded(urls)
        iconsVersion++
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // Depend on iconsVersion so overlays refresh after bitmaps load.
            @Suppress("UNUSED_EXPRESSION")
            iconsVersion
            val existing = view.overlays.filterIsInstance<Marker>()
            view.overlays.removeAll(existing.toSet())
            markers.forEach { item ->
                val marker = Marker(view)
                marker.position = GeoPoint(item.lat, item.lng)
                marker.title = item.name.ifBlank { item.layer.ifBlank { item.type } }
                marker.snippet = buildString {
                    append(item.type)
                    if (item.layer.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(item.layer)
                    }
                }
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.isEnabled = true
                marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                iconCache.drawableFor(item.resolvedIconUrl())?.let { drawable ->
                    // Mutate so hit-testing / bounds stay per-marker friendly with shared bitmaps.
                    marker.icon = drawable.constantState?.newDrawable()?.mutate() ?: drawable
                }
                // Explicit for every type (sw/uzel/pon/wifi/…) — same InfoWindow path as web tap.
                marker.setOnMarkerClickListener { clicked, map ->
                    map.overlays
                        .filterIsInstance<Marker>()
                        .forEach { if (it !== clicked) it.closeInfoWindow() }
                    clicked.showInfoWindow()
                    true
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        },
    )
}
