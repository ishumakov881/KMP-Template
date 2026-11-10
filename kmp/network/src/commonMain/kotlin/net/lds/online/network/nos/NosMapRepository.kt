package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.lds.online.network.ApiException

/**
 * NOS map data over the same CookieJar as [NosAuthRepository].
 * Loads markgen **per type** (do not fan-out all layers at once).
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
        const val TYPE_UZEL = "uzel"
        const val TYPE_SW = "sw"
    }
}
