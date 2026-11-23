package online.lds.nos.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import net.lds.online.network.nos.LeanMapLine
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.resolvedIconUrl
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Single canvas overlay for lines + mft circles + icon markers (web preferCanvas spirit).
 * Apply = assign lists + [MapView.invalidate] — no N× Marker/Polyline on Main (U28).
 * Hit = nearest among icons+mft in slop; miss → false (U30). Lines never steal taps.
 */
internal class NosMapFeaturesOverlay(
    private val iconCache: MarkerIconCache,
) : Overlay() {

    var lines: List<LeanMapLine> = emptyList()
    var mft: List<LeanMapMarker> = emptyList()
    var icons: List<LeanMapMarker> = emptyList()
    var onItemClick: ((LeanMapMarker) -> Unit)? = null

    private val scratch = Point()
    private val scratch2 = Point()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = LINK_LINE_COLOR
        strokeWidth = LINK_LINE_STROKE_WIDTH
    }
    private val mftFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(128, 0, 0, 0)
    }
    private val mftStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 0.5f
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        val bounds = mapView.boundingBox
        val density = mapView.context.resources.displayMetrics.density

        // 1) lines (under points)
        for (line in lines) {
            val aIn = bounds.contains(line.lat1, line.lng1)
            val bIn = bounds.contains(line.lat2, line.lng2)
            if (!aIn && !bIn) continue
            projection.toPixels(GeoPoint(line.lat1, line.lng1), scratch)
            projection.toPixels(GeoPoint(line.lat2, line.lng2), scratch2)
            canvas.drawLine(
                scratch.x.toFloat(),
                scratch.y.toFloat(),
                scratch2.x.toFloat(),
                scratch2.y.toFloat(),
                linePaint,
            )
        }

        // 2) mft circles
        val mftRadius = MFT_RADIUS_PX * density
        for (item in mft) {
            if (!bounds.contains(item.lat, item.lng)) continue
            projection.toPixels(GeoPoint(item.lat, item.lng), scratch)
            val x = scratch.x.toFloat()
            val y = scratch.y.toFloat()
            canvas.drawCircle(x, y, mftRadius, mftFill)
            canvas.drawCircle(x, y, mftRadius, mftStroke)
        }

        // 3) icon bitmaps (on top)
        for (item in icons) {
            if (!bounds.contains(item.lat, item.lng)) continue
            val bmp = iconCache.bitmapFor(item.resolvedIconUrl()) ?: continue
            projection.toPixels(GeoPoint(item.lat, item.lng), scratch)
            canvas.drawBitmap(
                bmp,
                scratch.x - bmp.width / 2f,
                scratch.y - bmp.height / 2f,
                null,
            )
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val click = onItemClick ?: return false
        if (icons.isEmpty() && mft.isEmpty()) return false

        val projection = mapView.projection
        val density = mapView.context.resources.displayMetrics.density
        val mftSlop = MFT_RADIUS_PX * density * 2.5f

        var best: LeanMapMarker? = null
        var bestDist = Float.MAX_VALUE
        var bestIsIcon = false

        for (item in icons) {
            val bmp = iconCache.bitmapFor(item.resolvedIconUrl())
            val half = if (bmp != null) {
                maxOf(bmp.width, bmp.height) / 2f
            } else {
                12f * density
            }
            val slop = half * 1.35f
            projection.toPixels(GeoPoint(item.lat, item.lng), scratch)
            val d = hypot(e.x - scratch.x, e.y - scratch.y)
            if (d > slop) continue
            if (d < bestDist || (d == bestDist && !bestIsIcon)) {
                bestDist = d
                best = item
                bestIsIcon = true
            }
        }

        for (item in mft) {
            projection.toPixels(GeoPoint(item.lat, item.lng), scratch)
            val d = hypot(e.x - scratch.x, e.y - scratch.y)
            if (d > mftSlop) continue
            if (d < bestDist) {
                bestDist = d
                best = item
                bestIsIcon = false
            }
        }

        val hit = best ?: return false
        click(hit)
        return true
    }

    companion object {
        private const val MFT_RADIUS_PX = 5f
        private const val LINK_LINE_COLOR: Int = 0xCC2F6FEB.toInt()
        private const val LINK_LINE_STROKE_WIDTH: Float = 3f

        fun isMftCircleMarker(marker: LeanMapMarker): Boolean =
            marker.type == NosMapRepository.TYPE_MFT ||
                marker.layer == "layer_pon_mft"
    }
}
