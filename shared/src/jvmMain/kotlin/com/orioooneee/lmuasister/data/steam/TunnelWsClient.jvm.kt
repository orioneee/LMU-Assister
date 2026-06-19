package com.orioooneee.lmuasister.data.steam

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.time.Duration

internal actual fun tunnelWsClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(Duration.ofSeconds(90)) // Render free cold start can take ~30-60s
            readTimeout(Duration.ZERO)             // long-lived WS — no read timeout
            writeTimeout(Duration.ofSeconds(60))
            pingInterval(Duration.ofSeconds(15))   // keepalive
        }
    }
    install(WebSockets)
}
