package com.orioooneee.lmuasister.data.remote

import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.cache.LocalCache
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ApiBaseUrlProvider(
    private val appSite: String = BuildConfig.APP_SITE,
    private val appCheckTokenProvider: suspend () -> String? = { null },
    private val awaitNetworkAllowed: suspend () -> Unit = {},
    fixedBaseUrl: String? = null,
) {
    private val cacheKey = "bootstrap.api_base_url"
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val bootstrapClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
        }
    }

    private var memoryBaseUrl: String? = fixedBaseUrl?.normalizedApiUrl()

    fun currentBaseUrl(): String? = memoryBaseUrl

    fun warmUp() {
        if (memoryBaseUrl != null) return
        scope.launch {
            runCatching { getBaseUrl() }
        }
    }

    suspend fun getBaseUrl(): String {
        memoryBaseUrl?.let { return it }

        readCachedBaseUrl()?.let { cached ->
            memoryBaseUrl = cached
            refreshInBackground()
            return cached
        }

        return refreshBaseUrl(force = false)
    }

    suspend fun url(pathAndQuery: String): String =
        getBaseUrl() + pathAndQuery.withLeadingSlash()

    suspend fun <T> withBaseUrlRetry(block: suspend (baseUrl: String) -> T): T {
        val initial = getBaseUrl()
        return try {
            block(initial)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val refreshed = runCatching { refreshBaseUrl(force = true) }.getOrElse { throw error }
            if (refreshed == initial) throw error
            block(refreshed)
        }
    }

    private fun refreshInBackground() {
        scope.launch {
            runCatching { refreshBaseUrl(force = true) }
        }
    }

    private suspend fun refreshBaseUrl(force: Boolean): String = mutex.withLock {
        if (!force) {
            memoryBaseUrl?.let { return@withLock it }
            readCachedBaseUrl()?.let { cached ->
                memoryBaseUrl = cached
                return@withLock cached
            }
        }

        val fetched = fetchBootstrapBaseUrl()
        memoryBaseUrl = fetched
        LocalCache.write(cacheKey, fetched)
        fetched
    }

    private suspend fun fetchBootstrapBaseUrl(): String {
        awaitNetworkAllowed()
        val site = appSite.trim().trimEnd('/')
        val text = bootstrapClient.get("$site/bootstrap") {
            appCheckTokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                header("X-Token", token)
            }
        }.bodyAsText()
        val response = AppJson.decodeFromString<BootstrapResponse>(text)
        return response.apiBaseUrl.normalizedApiUrl()
    }

    private fun readCachedBaseUrl(): String? =
        LocalCache.read(cacheKey)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { it.normalizedApiUrl() }.getOrNull() }

    private fun String.normalizedApiUrl(): String {
        val base = trim().trimEnd('/')
        require(base.startsWith("https://") || base.startsWith("http://")) {
            "Resolved API URL must be absolute."
        }
        val versionless = base.replace(API_VERSION_SUFFIX, "")
        return "$versionless$API_V3_PATH"
    }

    private fun String.withLeadingSlash(): String =
        if (startsWith('/')) this else "/$this"
}

@Serializable
private data class BootstrapResponse(
    @SerialName("api_base_url")
    val apiBaseUrl: String,
)

private const val API_V3_PATH = "/api/v3"
private val API_VERSION_SUFFIX = Regex("/api/v[0-9]+$")
