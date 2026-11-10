package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createNosEngineHttpClient(): HttpClient = HttpClient(OkHttp)
