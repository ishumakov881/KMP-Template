package dev.walhalla.lazagne.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads and caches NOS map icons by absolute URL (few unique icons, many markers).
 */
internal class MarkerIconCache(private val context: Context) {
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private val mutex = Mutex()

    suspend fun ensureLoaded(urls: Collection<String>) {
        val missing = urls.filter { it.isNotBlank() && it !in bitmaps }.distinct()
        if (missing.isEmpty()) return
        missing.forEach { url ->
            val bmp = withContext(Dispatchers.IO) { downloadBitmap(url) } ?: return@forEach
            mutex.withLock { bitmaps[url] = bmp }
        }
    }

    fun drawableFor(url: String?): Drawable? {
        if (url.isNullOrBlank()) return null
        val bmp = bitmaps[url] ?: return null
        return BitmapDrawable(context.resources, bmp)
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
