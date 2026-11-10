package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

expect fun createNosEngineHttpClient(): HttpClient

/**
 * Shared NOS client: cookie jar keeps [PHPSESSID] like the browser after POST /login.
 */
fun createNosHttpClient(
    baseUrl: String = NosEndpoints.BASE_URL,
    cookies: AcceptAllCookiesStorage = AcceptAllCookiesStorage(),
): HttpClient {
    return createNosEngineHttpClient().config {
        expectSuccess = false
        install(HttpCookies) {
            storage = cookies
        }
        defaultRequest {
            url(baseUrl)
            header("X-Requested-With", "XMLHttpRequest")
        }
    }
}
