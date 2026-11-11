package online.lds.nos.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads and caches NOS map icons by absolute URL (few unique icons, many markers).
 * Memory + disk so cold start after first visit does not flash default pins.
 * Display size is [DISPLAY_SCALE] × PNG pixels (osmdroid uses drawable intrinsic size).
 */
internal class MarkerIconCache(private val context: Context) {
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private val mutex = Mutex()
    private val diskDir = File(context.cacheDir, "map_icons").also { it.mkdirs() }

    companion object {
        /** Slightly larger than web asset pixels for phone map readability. */
        const val DISPLAY_SCALE = 1.5f
    }

    suspend fun ensureLoaded(urls: Collection<String>) {
        val missing = urls.filter { it.isNotBlank() && it !in bitmaps }.distinct()
        if (missing.isEmpty()) return
        coroutineScope {
            missing.map { url ->
                async(Dispatchers.IO) {
                    val bmp = loadFromDisk(url) ?: downloadBitmap(url)?.also { saveToDisk(url, it) }
                    url to bmp
                }
            }.awaitAll().forEach { (url, bmp) ->
                if (bmp != null) mutex.withLock { bitmaps[url] = bmp }
            }
        }
    }

    fun drawableFor(url: String?): Drawable? {
        if (url.isNullOrBlank()) return null
        val bmp = bitmaps[url] ?: return null
        val w = (bmp.width * DISPLAY_SCALE).toInt().coerceAtLeast(1)
        val h = (bmp.height * DISPLAY_SCALE).toInt().coerceAtLeast(1)
        val scaled = if (w == bmp.width && h == bmp.height) {
            bmp
        } else {
            Bitmap.createScaledBitmap(bmp, w, h, true)
        }
        return BitmapDrawable(context.resources, scaled)
    }

    private fun diskFile(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { b -> "%02x".format(b) }
        return File(diskDir, digest)
    }

    private fun loadFromDisk(url: String): Bitmap? =
        runCatching {
            val file = diskFile(url)
            if (!file.isFile) return null
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()

    private fun saveToDisk(url: String, bmp: Bitmap) {
        runCatching {
            diskFile(url).outputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", context.packageName)
            }
            conn.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}
