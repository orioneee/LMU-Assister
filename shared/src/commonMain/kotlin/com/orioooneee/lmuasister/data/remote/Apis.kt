package com.orioooneee.lmuasister.data.remote

import com.orioooneee.lmuasister.config.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    // Explicit JSON null on a non-null field → fall back to its default (e.g. position: null
    // on a no-result race would otherwise break the whole decode).
    coerceInputValues = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/**
 * Profile endpoints decode with this instead of [AppJson]. The /profile* surface mixes
 * snake_case (most fields) and camelCase (the `stats.total` counters), and a global
 * naming strategy rewrites @SerialName too — so it can't express that mix. Here there is
 * NO strategy: every wire key is stated explicitly via @SerialName (snake) or matches the
 * Kotlin name verbatim (the camelCase stat keys). Same leniency/coercion as [AppJson].
 */
val ProfileJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/**
 * The LmuAssister backend — the client's only data source. It does all the
 * upstream merging (lmuportal schedule, S3 cards, racecontrol covers, weather,
 * time-slot maths) server-side and exposes:
 *   - GET /schedule        → every week + championships, fully merged
 *   - GET /race/<id>       → the race plus its fastest-lap leaderboard
 *
 * Images are absolute CDN URLs (R2 / S3) in the payloads — loaded directly, no proxy.
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

    /** The full car roster (v2 reference data — static, cache aggressively). */
    suspend fun cars(): CarsResponse =
        AppJson.decodeFromString(client.get("$API_BASE/cars").bodyAsText())

    /** The full track roster (v2 reference data — static, cache aggressively; public). */
    suspend fun tracks(): TracksResponse =
        AppJson.decodeFromString(client.get("$API_BASE/tracks").bodyAsText())

    /** Privacy policy as plain text — rendered in-app (no auth required). */
    suspend fun privacy(): String = client.get("$API_BASE/privacy").bodyAsText()

    /** Public driver directory summary: rating distribution + top safety drivers. */
    suspend fun usersSummary(): UsersSummaryResponse =
        ProfileJson.decodeFromString(client.get("$API_BASE/users/summary").bodyAsText())

    /** Public user search. Empty query intentionally returns an empty page. */
    suspend fun usersSearch(query: String, page: Int = 1): UsersSearchResponse {
        val url = "$API_BASE/users/search?q=${query.encodeURLQueryComponent()}&page=$page"
        return ProfileJson.decodeFromString(client.get(url).bodyAsText())
    }

    /** Public user profile saved in our DB. Does not hit Nakama live. */
    suspend fun publicUser(uid: String): SteamProfile {
        val resp = client.get("$API_BASE/users/${uid.encodeURLPathPart()}")
        val text = resp.bodyAsText()
        if (resp.status.value == 404) throw Exception("user_not_found")
        if (resp.status.value !in 200..299) throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        return ProfileJson.decodeFromString(text)
    }

    /** Public saved track history for a user. Same payload shape as /profile/track/<track_id>. */
    suspend fun publicUserTrack(uid: String, trackId: String): TrackDetailResponse {
        val resp = client.get("$API_BASE/users/${uid.encodeURLPathPart()}/track/${trackId.encodeURLPathPart()}")
        val text = resp.bodyAsText()
        if (resp.status.value == 404) throw Exception("track_not_found")
        if (resp.status.value !in 200..299) throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        return AppJson.decodeFromString(text)
    }

    /** One page of the full official leaderboard (cursor-paginated). When [token] is
     *  supplied, the response also carries the caller's own row + rank (`me`). */
    suspend fun leaderboardPage(
        leaderboardId: String,
        cursor: String? = null,
        limit: Int = 50,
        token: String? = null,
    ): LeaderboardPageResponse {
        val url = buildString {
            append("$API_BASE/leaderboard/${leaderboardId.encodeURLPathPart()}?limit=$limit")
            if (!cursor.isNullOrBlank()) append("&cursor=${cursor.encodeURLQueryComponent()}")
        }
        val body = client.get(url) {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }.bodyAsText()
        return AppJson.decodeFromString(body)
    }

    private companion object {
        val API_BASE = BuildConfig.BACKEND_URL.trimEnd('/')   // http://host/api/v2
    }
}
