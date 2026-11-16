package online.lds.nos.ui.map

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.UIKit.UIColor

/**
 * Web-like `circleMarker` for muffs: small filled circle, no pin / no pon.png.
 */
@OptIn(ExperimentalForeignApi::class)
internal class MftCircleAnnotationView(
    annotation: MKAnnotationProtocol?,
    reuseIdentifier: String?,
) : MKAnnotationView(annotation = annotation, reuseIdentifier = reuseIdentifier) {

    init {
        frame = CGRectMake(0.0, 0.0, 10.0, 10.0)
        backgroundColor = UIColor.colorWithRed(0.0, 0.0, 0.0, alpha = 0.5)
        layer.cornerRadius = 5.0
        clipsToBounds = true
        canShowCallout = false
    }

    companion object {
        const val REUSE_ID = "nos_mft_circle"
    }
}
