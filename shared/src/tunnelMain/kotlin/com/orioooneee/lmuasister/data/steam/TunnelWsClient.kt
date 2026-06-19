package com.orioooneee.lmuasister.data.steam

import io.ktor.client.HttpClient

/**
 * Platform HTTP client for the device tunnel WebSocket. The engine is chosen per
 * platform (OkHttp on JVM/Android, Darwin on iOS) — notably NOT Ktor CIO, whose
 * WebSocket has quirks talking to Cloudflare-fronted origins (Render). A ping
 * keepalive is installed so the upgraded connection stays live end-to-end.
 */
internal expect fun tunnelWsClient(): HttpClient
