package online.lds.nos.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Web parity for `layer_pon_mft`: Leaflet `L.circleMarker` + `preferCanvas: true`.
 * One overlay, one `draw()` pass — no per-muff [Marker], no icon downloads.
 * Legend keeps `/images/map/pon.png`; only map dots use circles.
 */
internal class MftCirclesOverlay : Overlay() {

    /** Current muff points (already filtered by legend visibility upstream). */
    var items: List<LeanMapMarker> = emptyList()

    var onItemClick: ((LeanMapMarker) -> Unit)? = null

    private val scratch = Point()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Web: fillOpacity 0.5, fillColor usually #000000
        color = Color.argb(128, 0, 0, 0)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 0.5f
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || items.isEmpty()) return
        val projection = mapView.projection
        val bounds = mapView.boundingBox
        val radiusPx = CIRCLE_RADIUS_PX * mapView.context.resources.displayMetrics.density
        for (item in items) {
            // Viewport cull — skip off-screen muffs (osmdroid Marker does not do this).
            if (!bounds.contains(item.lat, item.lng)) continue
            projection.toPixels(GeoPoint(item.lat, item.lng), scratch)
            val x = scratch.x.toFloat()
            val y = scratch.y.toFloat()
            canvas.drawCircle(x, y, radiusPx, fillPaint)
            canvas.drawCircle(x, y, radiusPx, strokePaint)
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val click = onItemClick ?: return false
        if (items.isEmpty()) return false
        val projection = mapView.projection
        val radiusPx = CIRCLE_RADIUS_PX * mapView.context.resources.displayMetrics.density
        val hitSlop = radiusPx * 2.5f
        var best: LeanMapMarker? = null
        var bestDist = Float.MAX_VALUE
        for (item in items) {
            projection.toPixels(GeoPoint(item.lat, item.lng), scratch)
            val d = hypot(e.x - scratch.x, e.y - scratch.y)
            if (d <= hitSlop && d < bestDist) {
                bestDist = d
                best = item
            }
        }
        val hit = best ?: return false
        click(hit)
        return true
    }

    companion object {
        /** Leaflet `circleMarker` radius: 5 (CSS px). */
        private const val CIRCLE_RADIUS_PX = 5f

        fun isMftCircleMarker(marker: LeanMapMarker): Boolean =
            marker.type == NosMapRepository.TYPE_MFT ||
                marker.layer == "layer_pon_mft"
    }
}
