package com.orioooneee.lmuasister.analytics

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/**
 * Adds one Firebase Performance HTTP metric around every Ktor request made by the
 * configured client. Firebase itself is platform-specific; commonMain only owns
 * request boundaries and response metadata.
 */
fun HttpClientConfig<*>.installPerformanceMonitoring() {
    install(FirebasePerformanceHttpMetrics)
}

private val FirebasePerformanceHttpMetrics = createClientPlugin("FirebasePerformanceHttpMetrics") {
    on(Send) { request ->
        val metric = Telemetry.performanceMonitor.startHttpMetric(
            url = request.url.toString(),
            method = request.method.value,
        )
        try {
            val call = proceed(request)
            metric.setHttpResponseCode(call.response.status.value)
            metric.setResponseContentType(call.response.headers.get(HttpHeaders.ContentType))
            call
        } finally {
            metric.stop()
        }
    }
}
