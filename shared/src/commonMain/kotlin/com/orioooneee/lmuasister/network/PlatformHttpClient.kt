package com.orioooneee.lmuasister.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Creates an HTTP client with the engine selected explicitly for the current platform.
 *
 * This must not fall back to Ktor's dependency-order based engine discovery: kSteam brings
 * CIO transitively, and CIO cannot establish TLS sessions on Kotlin/Native.
 */
expect fun platformHttpClient(
    block: HttpClientConfig<*>.() -> Unit = {},
): HttpClient
