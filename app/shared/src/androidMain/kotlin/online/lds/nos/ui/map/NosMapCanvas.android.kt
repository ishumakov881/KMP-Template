package online.lds.nos.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.lds.online.network.nos.LeanMapLine
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.resolvedIconUrl
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File

/** DEBUG: blink period for red square marker (remove before release). */
private const val DebugBlinkMs = 600L

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
    lines: List<LeanMapLine>,
    modifier: Modifier,
    onMarkerClick: (LeanMapMarker) -> Unit,
) {
    val context = LocalContext.current
    val iconCache = remember { MarkerIconCache(context.applicationContext) }
    var iconsVersion by remember { mutableIntStateOf(0) }
    var displayMarkers by remember { mutableStateOf<List<LeanMapMarker>>(emptyList()) }
    var displayMft by remember { mutableStateOf<List<LeanMapMarker>>(emptyList()) }
    val onMarkerClickState = rememberUpdatedState(onMarkerClick)
    // One canvas overlay: lines + mft + icons + hit (no N× Marker/Polyline).
    val featuresOverlay = remember {
        NosMapFeaturesOverlay(iconCache).also { overlay ->
            overlay.onItemClick = { lean -> onMarkerClickState.value(lean) }
        }
    }
    // DEBUG: independent Overlay so feature apply does not wipe the blink.
    val debugBlinkOverlay = remember { DebugBlinkOverlay() }
    val applied = remember { MapOverlayApplyState() }

    val mapView = remember {
        ensureOsmdroidConfig(context.applicationContext)
        MapView(context).apply {
            setTileSource(LdsHotTiles)
            setMultiTouchControls(true)
            // Keep +/- at bottom-right; legend/refresh live at top-start in Compose.
            setBuiltInZoomControls(true)
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(48.4422, 39.3544))
            overlays.add(featuresOverlay)
            overlays.add(debugBlinkOverlay)
        }
    }

    // DEBUG: add/remove red square every 600ms (map center).
    LaunchedEffect(mapView) {
        while (isActive) {
            delay(DebugBlinkMs)
            debugBlinkOverlay.visible = !debugBlinkOverlay.visible
            mapView.invalidate()
        }
    }

    LaunchedEffect(markers) {
        val mft = markers.filter { NosMapFeaturesOverlay.isMftCircleMarker(it) }
        val icons = markers.filterNot { NosMapFeaturesOverlay.isMftCircleMarker(it) }
        displayMft = mft
        if (icons.isEmpty()) {
            displayMarkers = emptyList()
            iconsVersion++
            return@LaunchedEffect
        }
        val urls = icons.mapNotNull { it.resolvedIconUrl() }.distinct()
        iconCache.ensureLoaded(urls)
        displayMarkers = icons
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
            @Suppress("UNUSED_EXPRESSION")
            iconsVersion
            val markersChanged =
                displayMarkers !== applied.markers && displayMarkers != applied.markers
            val mftChanged = displayMft !== applied.mft && displayMft != applied.mft
            val linesChanged = lines !== applied.lines && lines != applied.lines
            val iconsChanged = iconsVersion != applied.iconsVersion
            if (!markersChanged && !mftChanged && !linesChanged && !iconsChanged) {
                applied.skipCount++
                if (applied.skipCount <= 5 || applied.skipCount % 20 == 0) {
                    nosMapPerf("canvas update SKIP #${applied.skipCount} (visible unchanged)")
                }
                return@AndroidView
            }

            // Cheap apply: pointer swap + invalidate (gestures stay responsive — U28).
            nosMapPerfTimed(
                "canvas update APPLY icons=${displayMarkers.size} mft=${displayMft.size} lines=${lines.size}",
            ) {
                featuresOverlay.lines = lines
                featuresOverlay.mft = displayMft
                featuresOverlay.icons = displayMarkers
                featuresOverlay.onItemClick = { lean -> onMarkerClickState.value(lean) }
                applied.mft = displayMft
                applied.markers = displayMarkers
                applied.lines = lines
                applied.iconsVersion = iconsVersion
                if (featuresOverlay !in view.overlays) {
                    view.overlays.add(0, featuresOverlay)
                }
                if (debugBlinkOverlay !in view.overlays) {
                    view.overlays.add(debugBlinkOverlay)
                }
                view.invalidate()
            }
        },
    )
}

private class MapOverlayApplyState {
    var markers: List<LeanMapMarker> = emptyList()
    var mft: List<LeanMapMarker> = emptyList()
    var lines: List<LeanMapLine> = emptyList()
    var iconsVersion: Int = -1
    var skipCount: Int = 0
}

/** DEBUG only: flashing red square at map center (not a real NOS marker). */
private class DebugBlinkOverlay : Overlay() {
    @Volatile
    var visible: Boolean = false

    private val geo = GeoPoint(48.4422, 39.3544)
    private val scratch = Point()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !visible) return
        val half = 18f * mapView.context.resources.displayMetrics.density
        mapView.projection.toPixels(geo, scratch)
        canvas.drawRect(
            scratch.x - half,
            scratch.y - half,
            scratch.x + half,
            scratch.y + half,
            paint,
        )
    }
}
