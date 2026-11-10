package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import net.lds.online.network.ApiException

data class NosLoginResult(
    val phpSessionId: String?,
    val switchCountsJson: String,
)

/**
 * Web-parity session auth: POST /login (login/password/next) → CookieJar [PHPSESSID],
 * then probe [mode=get_count] to confirm the session is usable.
 */
class NosAuthRepository(
    private val cookies: AcceptAllCookiesStorage = AcceptAllCookiesStorage(),
    private val client: HttpClient = createNosHttpClient(cookies = cookies),
) {
    suspend fun login(
        login: String,
        password: String,
        next: String = "/osm",
    ): NosLoginResult {
        val response = client.submitForm(
            url = NosEndpoints.LOGIN,
            formParameters = Parameters.build {
                append("login", login)
                append("password", password)
                append("next", next)
            },
        )
        val body = response.bodyAsText()
        val sessionCookies = client.cookies(Url(NosEndpoints.BASE_URL))
        val phpSessionId = sessionCookies.firstOrNull { it.name == "PHPSESSID" }?.value

        if (phpSessionId.isNullOrBlank()) {
            throw ApiException.authError("Сервер не выдал PHPSESSID")
        }

        // Failed login usually redisplays the form; success may be 200 or redirect.
        if (body.contains("Форма авторизации", ignoreCase = false) &&
            !response.headers.contains("Location")
        ) {
            // Still probe API — some builds keep shell chrome; get_count is the source of truth.
        }

        val counts = fetchSwitchCounts()
        val looksLikeCounts = counts.trimStart().startsWith("{") && counts.contains("sw_ok")
        if (!looksLikeCounts) {
            throw ApiException.clientError(401, "Неверный логин или пароль")
        }

        return NosLoginResult(
            phpSessionId = phpSessionId,
            switchCountsJson = counts,
        )
    }

    suspend fun fetchSwitchCounts(): String {
        val response = client.post(NosEndpoints.AJAX_MAP) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("mode=get_count")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess() && text.contains("Форма авторизации")) {
            throw ApiException.clientError(401, "Сессия недействительна")
        }
        return text
    }

    fun close() {
        client.close()
    }
}

private fun ApiException.Companion.authError(message: String): ApiException =
    clientError(401, message)
