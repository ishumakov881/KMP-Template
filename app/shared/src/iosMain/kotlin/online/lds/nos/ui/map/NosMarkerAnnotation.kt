package online.lds.nos.ui.map

import kotlinx.cinterop.ExperimentalForeignApi
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.NosMapRepository
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
internal class NosMarkerAnnotation(
    val marker: LeanMapMarker,
) : MKPointAnnotation() {

    val isMft: Boolean =
        marker.type == NosMapRepository.TYPE_MFT || marker.layer == "layer_pon_mft"

    init {
        setCoordinate(CLLocationCoordinate2DMake(marker.lat, marker.lng))
        setTitle(marker.name.ifBlank { marker.layer.ifBlank { marker.type } })
    }
}
