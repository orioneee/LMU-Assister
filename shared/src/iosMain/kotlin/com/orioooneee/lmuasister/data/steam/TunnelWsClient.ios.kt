package com.orioooneee.lmuasister.data.steam

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

internal actual fun tunnelWsClient(): HttpClient = HttpClient(Darwin) {
    engine {
        configureSession {
            setTimeoutIntervalForRequest(90.0)   // Render free cold start
            setTimeoutIntervalForResource(180.0)
        }
    }
    install(WebSockets) { pingIntervalMillis = 15_000 }
}
