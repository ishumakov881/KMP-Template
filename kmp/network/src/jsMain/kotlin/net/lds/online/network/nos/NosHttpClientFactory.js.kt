package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createNosEngineHttpClient(): HttpClient = HttpClient(Js)
