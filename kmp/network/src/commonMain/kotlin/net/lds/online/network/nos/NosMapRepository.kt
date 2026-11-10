package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import net.lds.online.network.ApiException

/**
 * NOS map data over the same CookieJar as [NosAuthRepository].
 * Loads markgen **per type** (enabled set only — never silent full dump of all web types/MFT).
 */
class NosMapRepository(
    private val client: HttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun markgen(type: String): List<LeanMapMarker> {
        val response = client.submitForm(
            url = NosEndpoints.AJAX_MAP,
            formParameters = Parameters.build {
                append("mode", "markgen")
                append("type", type)
            },
        )
        val body = response.bodyAsText()
        if (isLoginHtml(body)) {
            throw ApiException.clientError(401, "Сессия истекла")
        }
        return parseLeanMarkers(body, type)
    }

    /** Parallel markgen for an explicit enabled set of types. */
    suspend fun markgenMany(types: Collection<String>): List<LeanMapMarker> {
        if (types.isEmpty()) return emptyList()
        return coroutineScope {
            types.distinct().map { type ->
                async { markgen(type) }
            }.awaitAll().flatten()
        }
    }

    private fun parseLeanMarkers(body: String, type: String): List<LeanMapMarker> {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed == "null") return emptyList()
        val element = try {
            json.parseToJsonElement(trimmed)
        } catch (e: Exception) {
            throw ApiException.parsingError(e)
        }
        val array = element as? JsonArray
            ?: throw ApiException.parsingError(
                IllegalStateException("markgen expected JSON array, got ${element::class.simpleName}"),
            )
        return array.mapNotNull { node ->
            val obj = node as? JsonObject ?: return@mapNotNull null
            val lat = obj.coord("lat") ?: return@mapNotNull null
            val lng = obj.coord("lng") ?: return@mapNotNull null
            LeanMapMarker(
                name = obj.string("name").orEmpty(),
                lat = lat,
                lng = lng,
                state = obj.int("state") ?: 0,
                layer = obj.string("layer").orEmpty(),
                icon = obj.string("icon"),
                type = type,
            )
        }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.intOrNull ?: p.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.coord(key: String): Double? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.doubleOrNull ?: p.contentOrNull?.toDoubleOrNull()
    }

    private fun isLoginHtml(body: String): Boolean =
        body.contains("Форма авторизации") ||
            body.contains("id=\"signin\"") ||
            body.contains("id='signin'")

    companion object {
        /** markgen `type` values from web `map_osm.js` (returnSw/Uzel/Pon/Mft/Wifi/Usil + line/mft_line). */
        const val TYPE_SW = "sw"
        const val TYPE_UZEL = "uzel"
        const val TYPE_PON = "pon_new"
        const val TYPE_MFT = "mft"
        const val TYPE_WIFI = "wifi"
        const val TYPE_USIL = "usil"
        const val TYPE_KTV_AMP = "ktv_amp"
        const val TYPE_LINE = "line"
        const val TYPE_MFT_LINE = "mft_line"

        /**
         * Full web markgen set — keep in sync with `/js/map_osm.js` loaders, not a hand-picked subset.
         */
        val DEFAULT_MAP_TYPES: List<String> = listOf(
            TYPE_SW,
            TYPE_UZEL,
            TYPE_PON,
            TYPE_MFT,
            TYPE_WIFI,
            TYPE_USIL,
            TYPE_KTV_AMP,
            TYPE_LINE,
            TYPE_MFT_LINE,
        )
    }
}
