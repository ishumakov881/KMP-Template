package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createNosEngineHttpClient(): HttpClient = HttpClient(Darwin)
