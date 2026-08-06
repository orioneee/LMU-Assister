package com.orioooneee.lmuasister.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

actual fun platformHttpClient(
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Js) {
    block()
}
