package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createNosEngineHttpClient(): HttpClient = HttpClient(CIO)
