package online.lds.nos.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import net.lds.online.network.nos.LeanMapLine
import net.lds.online.network.nos.LeanMapMarker
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView

/** Default center: Luhansk (same as Android osmdroid canvas). */
private const val DefaultLat = 48.4422
private const val DefaultLng = 39.3544
private const val DefaultRegionMeters = 40_000.0

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NosMapCanvas(
    markers: List<LeanMapMarker>,
    lines: List<LeanMapLine>,
    modifier: Modifier,
    onMarkerClick: (LeanMapMarker) -> Unit,
) {
    val onMarkerClickState = rememberUpdatedState(onMarkerClick)
    val delegate = remember {
        NosMapDelegate()
    }
    delegate.onMarkerClick = { lean -> onMarkerClickState.value(lean) }

    val mapView = remember {
        MKMapView().apply {
            this.delegate = delegate
            showsCompass = true
            zoomEnabled = true
            scrollEnabled = true
            rotateEnabled = false
            pitchEnabled = false
            setRegion(
                MKCoordinateRegionMakeWithDistance(
                    CLLocationCoordinate2DMake(DefaultLat, DefaultLng),
                    DefaultRegionMeters,
                    DefaultRegionMeters,
                ),
                animated = false,
            )
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            syncMapKitContent(view, markers, lines)
        },
        onRelease = { },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true,
        ),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun syncMapKitContent(
    mapView: MKMapView,
    markers: List<LeanMapMarker>,
    lines: List<LeanMapLine>,
) {
    val oldAnnotations = mapView.annotations.mapNotNull { it as? NosMarkerAnnotation }
    if (oldAnnotations.isNotEmpty()) {
        mapView.removeAnnotations(oldAnnotations)
    }
    mapView.overlays.toList().forEach { overlay ->
        mapView.removeOverlay(overlay)
    }

    // Lines under markers (add overlays first).
    lines.forEach { line ->
        mapView.addOverlay(leanLineToPolyline(line))
    }
    markers.forEach { marker ->
        mapView.addAnnotation(NosMarkerAnnotation(marker))
    }
}
