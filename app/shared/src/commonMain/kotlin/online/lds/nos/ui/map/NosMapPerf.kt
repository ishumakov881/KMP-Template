package online.lds.nos.ui.map

import kotlin.time.TimeSource

/**
 * DEBUG: `adb logcat -s NosMapPerf:I` (Android) / stdout elsewhere.
 * Remove with U25 blink before release.
 */
internal expect fun nosMapPerfPlatform(message: String)

internal fun nosMapPerf(message: String) {
    nosMapPerfPlatform(message)
}

internal inline fun <T> nosMapPerfTimed(label: String, block: () -> T): T {
    val mark = TimeSource.Monotonic.markNow()
    return try {
        block()
    } finally {
        nosMapPerf("$label ${mark.elapsedNow()}")
    }
}
