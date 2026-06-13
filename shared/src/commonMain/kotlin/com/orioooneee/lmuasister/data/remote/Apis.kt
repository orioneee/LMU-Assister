package com.orioooneee.lmuasister.data.remote

import com.orioooneee.lmuasister.config.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/** Shared lenient JSON — snake_case payload mapped to camelCase DTO properties. */
@OptIn(ExperimentalSerializationApi::class)
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/**
 * The LmuAssister backend — the client's only data source. It does all the
 * upstream merging (lmuportal schedule, S3 cards, racecontrol covers, weather,
 * time-slot maths) server-side and exposes:
 *   - GET /schedule        → every week + championships, fully merged
 *   - GET /race/<id>       → the race plus its fastest-lap leaderboard
 *   - GET /img/<token>     → proxied image bytes (referenced from image/map urls)
 *
 * Base URL comes from [BuildConfig.BACKEND_URL] (set in local.properties).
 */
class BackendApi(private val client: HttpClient) {

    suspend fun schedule(refresh: Boolean = false): ScheduleResponse {
        val url = "$API_BASE/schedule" + if (refresh) "?refresh=1" else ""
        return AppJson.decodeFromString(client.get(url).bodyAsText())
    }

    suspend fun race(raceId: String): RaceDetailResponse {
        val url = "$API_BASE/race/${raceId.encodeURLPathPart()}"
        return AppJson.decodeFromString(client.get(url).bodyAsText())
    }

    /** Hot-laps for the race's track. May be "pending" (still building) — caller polls. */
    suspend fun hotlaps(raceId: String, wait: Boolean = false): HotlapsResponse {
        val url = "$API_BASE/race/${raceId.encodeURLPathPart()}/hotlaps" + if (wait) "?wait=1" else ""
        return AppJson.decodeFromString(client.get(url).bodyAsText())
    }

    /** Absolute URL for a proxied image path the backend returns ("/api/v1/img/…"). */
    fun imageUrl(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> ORIGIN + path
    }

    private companion object {
        val API_BASE = BuildConfig.BACKEND_URL.trimEnd('/')   // http://host/api/v1
        val ORIGIN = API_BASE.removeSuffix("/api/v1")          // http://host
    }
}
