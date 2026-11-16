package online.lds.nos.ui.map

import kotlinx.cinterop.ExperimentalForeignApi
import net.lds.online.network.nos.LeanMapMarker
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.UIKit.UIColor
import platform.darwin.NSObject

/**
 * Keeps [onMarkerClick], annotation views and overlay renderers. Retained as [MKMapView.delegate].
 */
@OptIn(ExperimentalForeignApi::class)
internal class NosMapDelegate : NSObject(), MKMapViewDelegateProtocol {

    var onMarkerClick: (LeanMapMarker) -> Unit = {}

    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        val nos = viewForAnnotation as? NosMarkerAnnotation ?: return null
        if (nos.isMft) {
            val reused = mapView.dequeueReusableAnnotationViewWithIdentifier(MftCircleAnnotationView.REUSE_ID)
            val view = reused as? MftCircleAnnotationView
                ?: MftCircleAnnotationView(nos, MftCircleAnnotationView.REUSE_ID)
            view.annotation = nos
            return view
        }
        val reuseId = "nos_marker"
        val reused = mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId)
        val view = reused as? MKMarkerAnnotationView
            ?: MKMarkerAnnotationView(annotation = nos, reuseIdentifier = reuseId)
        view.annotation = nos
        view.canShowCallout = false
        return view
    }

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = didSelectAnnotationView.annotation as? NosMarkerAnnotation ?: return
        onMarkerClick(annotation.marker)
        mapView.deselectAnnotation(annotation, animated = false)
    }

    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        val overlay = rendererForOverlay
        return if (overlay is MKPolyline) {
            MKPolylineRenderer(overlay = overlay).apply {
                // Same blue as Android LinkLineColor 0xCC2F6FEB
                setStrokeColor(UIColor.colorWithRed(47.0 / 255.0, 111.0 / 255.0, 235.0 / 255.0, 0.8))
                setLineWidth(3.0)
            }
        } else {
            MKOverlayRenderer(overlay = overlay)
        }
    }
}
