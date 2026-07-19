package com.orioooneee.lmuasister.data.remote

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.encodedPath

fun appTokenAuthPlugin(
    tokenHolder: AppTokenHolder,
    apiBaseUrlProvider: ApiBaseUrlProvider,
) = createClientPlugin("AppTokenAuth") {
    onRequest { request, _ ->
        val token = tokenHolder.token.value?.takeIf { it.isNotBlank() } ?: return@onRequest
        if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest
        val backend = apiBaseUrlProvider.currentBaseUrl()?.let { Url(it) } ?: return@onRequest
        val backendPath = backend.encodedPath.trimEnd('/')
        if (!request.url.isBackendApiRequest(backend, backendPath)) return@onRequest

        request.header(HttpHeaders.Authorization, "Bearer $token")
    }
}

private fun io.ktor.http.URLBuilder.isBackendApiRequest(backend: Url, backendPath: String): Boolean {
    if (protocol.name != backend.protocol.name || host != backend.host || port != backend.port) return false

    val path = encodedPath
    if (path != backendPath && !path.startsWith("$backendPath/")) return false

    val relative = path.removePrefix(backendPath).ifBlank { "/" }
    return !relative.isPublicAuthExcluded()
}

private fun String.isPublicAuthExcluded(): Boolean {
    val path = if (startsWith('/')) this else "/$this"
    return path == "/privacy" ||
        path == "/docs" ||
        path.startsWith("/docs/") ||
        path == "/internal" ||
        path.startsWith("/internal/") ||
        path == "/schedule/notifications/devicepush" ||
        path == "/schedule/updated-subscribers" ||
        path.startsWith("/schedule/updated-subscribers/") ||
        path == "/auth" ||
        path.startsWith("/auth/")
}
