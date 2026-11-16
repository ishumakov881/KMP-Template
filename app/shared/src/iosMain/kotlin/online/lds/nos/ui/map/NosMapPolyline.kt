package online.lds.nos.ui.map

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import net.lds.online.network.nos.LeanMapLine
import platform.CoreLocation.CLLocationCoordinate2D
import platform.MapKit.MKPolyline

@OptIn(ExperimentalForeignApi::class)
internal fun leanLineToPolyline(line: LeanMapLine): MKPolyline = memScoped {
    val coords = allocArray<CLLocationCoordinate2D>(2)
    coords[0].latitude = line.lat1
    coords[0].longitude = line.lng1
    coords[1].latitude = line.lat2
    coords[1].longitude = line.lng2
    MKPolyline.polylineWithCoordinates(coords, count = 2u)
}
