package com.orioooneee.lmuasister.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO

actual fun platformHttpClient(
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(CIO) {
    block()
}
