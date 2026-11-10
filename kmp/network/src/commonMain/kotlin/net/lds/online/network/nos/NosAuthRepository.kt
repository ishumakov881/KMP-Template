package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import net.lds.online.network.ApiException

data class NosLoginResult(
    val phpSessionId: String?,
    /** Optional probe payload after auth (not used as login proof). */
    val authenticatedPageMarker: String,
)

/**
 * Web-parity session auth.
 *
 * Important: `PHPSESSID` is issued even for anonymous/failed login, and
 * `mode=get_count` works **without** auth — do **not** use them as success proof.
 *
 * Real flow (browser):
 * 1. GET `/login?next=...` (establish session cookie)
 * 2. POST `/login` with `login`/`password`/`next`/`submit-login`
 * 3. Success → **302 Location: /osm** (then map page with leaflet)
 * 4. Failure → **200** login form + `Неверное имя пользователя или пароль.`
 *
 * Persistence of PHPSESSID is done outside (russhwolf Settings); use [restoreSession]
 * on cold start and [login] to obtain a fresh id.
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
        // 1) Warm-up session like the browser
        client.get("${NosEndpoints.LOGIN}?next=$next")

        // 2) Submit credentials on the same PHPSESSID
        val response = client.submitForm(
            url = NosEndpoints.LOGIN,
            formParameters = Parameters.build {
                append("login", login)
                append("password", password)
                append("next", next)
                append("submit-login", "Вход")
            },
        ) {
            header(HttpHeaders.Origin, NosEndpoints.BASE_URL)
            header(HttpHeaders.Referrer, "${NosEndpoints.BASE_URL}${NosEndpoints.LOGIN}?next=$next")
        }

        assertLoginSucceeded(response, next)

        val sessionCookies = client.cookies(Url(NosEndpoints.BASE_URL))
        val phpSessionId = sessionCookies.firstOrNull { it.name == "PHPSESSID" }?.value
        if (phpSessionId.isNullOrBlank()) {
            throw ApiException.authError("Сервер не выдал PHPSESSID после успешного входа")
        }

        // 3) Hard check: protected page must open (not bounce to login)
        val osmBody = fetchProtectedPage(next)
        if (isLoginHtml(osmBody)) {
            throw ApiException.clientError(401, "Неверный логин или пароль")
        }

        return NosLoginResult(
            phpSessionId = phpSessionId,
            authenticatedPageMarker = pageMarker(osmBody),
        )
    }

    /**
     * Inject saved PHPSESSID into CookieJar and verify protected page still opens.
     * @return true if session is still valid.
     */
    suspend fun restoreSession(
        phpSessionId: String,
        next: String = "/osm",
    ): Boolean {
        if (phpSessionId.isBlank()) return false
        cookies.addCookie(
            requestUrl = Url(NosEndpoints.BASE_URL),
            cookie = Cookie(
                name = "PHPSESSID",
                value = phpSessionId,
                encoding = CookieEncoding.RAW,
                path = "/",
            ),
        )
        val body = fetchProtectedPage(next)
        return !isLoginHtml(body)
    }

    fun close() {
        client.close()
    }

    private suspend fun fetchProtectedPage(path: String): String =
        client.get(path).bodyAsText()

    private fun pageMarker(body: String): String = when {
        "map_osm" in body -> "map_osm"
        "leaflet" in body.lowercase() -> "leaflet"
        else -> "authenticated"
    }

    private suspend fun assertLoginSucceeded(response: HttpResponse, next: String) {
        val body = response.bodyAsText()
        val location = response.headers[HttpHeaders.Location].orEmpty()

        // Engine may or may not auto-follow 302; handle both.
        if (response.status == HttpStatusCode.Found || response.status == HttpStatusCode.SeeOther) {
            if (location.contains("login", ignoreCase = true)) {
                throw ApiException.clientError(401, extractLoginError(body))
            }
            if (location.isNotBlank() && (location == next || location.endsWith(next) || next in location)) {
                return
            }
        }

        if (body.contains("Неверное имя пользователя или пароль")) {
            throw ApiException.clientError(401, "Неверный логин или пароль")
        }
        if (isLoginHtml(body)) {
            throw ApiException.clientError(401, extractLoginError(body))
        }
        // Followed redirect to map already
        if ("map_osm" in body || "leaflet" in body.lowercase()) {
            return
        }
    }

    private fun isLoginHtml(body: String): Boolean =
        body.contains("Форма авторизации") ||
            body.contains("id=\"signin\"") ||
            body.contains("id='signin'")

    private fun extractLoginError(body: String): String {
        val marker = "id='signin_label'>"
        val start = body.indexOf(marker)
        if (start >= 0) {
            val from = start + marker.length
            val end = body.indexOf('<', from).takeIf { it > from } ?: return "Неверный логин или пароль"
            return body.substring(from, end).trim().ifBlank { "Неверный логин или пароль" }
        }
        return "Неверный логин или пароль"
    }
}

private fun ApiException.Companion.authError(message: String): ApiException =
    clientError(401, message)
