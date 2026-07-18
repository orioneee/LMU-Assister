package com.orioooneee.lmuasister.data.remote

import com.orioooneee.lmuasister.data.model.ScheduleCategory
import com.orioooneee.lmuasister.data.model.SchedulePeriod
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
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
 * Base URL is resolved at runtime and cached locally.
 */
class BackendApi(
    private val client: HttpClient,
    private val apiBaseUrlProvider: ApiBaseUrlProvider,
) {

    suspend fun schedule(refresh: Boolean = false): ScheduleResponse {
        val path = "/schedule" + if (refresh) "?refresh=1" else ""
        return getSchedule(path)
    }

    suspend fun schedule(
        period: SchedulePeriod,
        category: ScheduleCategory,
        refresh: Boolean = false,
    ): ScheduleResponse {
        val path = buildString {
            append("/schedule/")
            if (period == SchedulePeriod.NEXT) append("nextweek/")
            append(category.pathSegment)
            if (refresh) append("?refresh=1")
        }
        return getSchedule(path)
    }

    suspend fun race(raceId: String): RaceDetailResponse {
        val path = "/race/${raceId.encodeURLPathPart()}"
        return AppJson.decodeFromString(getText(path))
    }

    /** Top car models for an event class. Default reads only the backend cache; [fetch] may build it. */
    suspend fun topCars(eventId: String, carClass: String? = null, fetch: Boolean = false): TopCarsResponse {
        val query = buildList {
            carClass?.takeIf { it.isNotBlank() }?.let { add("class=${it.encodeURLQueryComponent()}") }
            if (fetch) add("fetch=1")
        }
        val path = buildString {
            append("/schedule/${eventId.encodeURLPathPart()}/topcars")
            if (query.isNotEmpty()) append("?${query.joinToString("&")}")
        }
        val resp = getResponse(path)
        val text = resp.bodyAsText()
        when (resp.status.value) {
            in 200..299 -> return AppJson.decodeFromString(text)
            403 -> throw Exception("app_check_required")
            404 -> {
                val error = runCatching { AppJson.decodeFromString<TopCarsResponse>(text) }.getOrNull()
                val isClassMiss = error?.reason.orEmpty().contains("class", ignoreCase = true) ||
                    error?.message.orEmpty().contains("class leaderboard", ignoreCase = true)
                throw Exception(if (isClassMiss) "class_leaderboard_not_found" else "event_not_found")
            }
            502 -> throw Exception("topcars_build_error")
            503 -> throw Exception("leaderboard_unavailable")
            else -> throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        }
    }

    /** Hot-laps for the race's track. May be "pending" (still building) — caller polls. */
    suspend fun hotlaps(raceId: String, wait: Boolean = false): HotlapsResponse {
        val path = "/race/${raceId.encodeURLPathPart()}/hotlaps" + if (wait) "?wait=1" else ""
        return AppJson.decodeFromString(getText(path))
    }

    /** The full car roster (v3 reference data — static, cache aggressively). */
    suspend fun cars(): CarsResponse =
        AppJson.decodeFromString(getText("/cars"))

    /** Full car catalogue with artwork, specs and liveries (public/static). */
    suspend fun carsDetailed(): CarsDetailedResponse =
        AppJson.decodeFromString(getText("/cars/detailed"))

    /** The full track roster (v3 reference data — static, cache aggressively; public). */
    suspend fun tracks(): TracksResponse =
        AppJson.decodeFromString(getText("/tracks"))

    /** Privacy policy as plain text — rendered in-app (no auth required). */
    suspend fun privacy(): String = getText("/privacy")

    /** Public driver directory summary: rating distribution + top safety drivers. */
    suspend fun usersSummary(): UsersSummaryResponse =
        ProfileJson.decodeFromString(getText("/users/summary"))

    /** Public user search. Empty query intentionally returns an empty page. */
    suspend fun usersSearch(query: String, page: Int = 1): UsersSearchResponse {
        val path = "/users/search?q=${query.encodeURLQueryComponent()}&page=$page"
        return ProfileJson.decodeFromString(getText(path))
    }

    /** Public user profile saved in our DB. Does not hit Nakama live. */
    suspend fun publicUser(uid: String): SteamProfile {
        val resp = getResponse("/users/${uid.encodeURLPathPart()}")
        val text = resp.bodyAsText()
        if (resp.status.value == 404) throw Exception("user_not_found")
        if (resp.status.value !in 200..299) throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        return ProfileJson.decodeFromString(text)
    }

    /** Public saved race history for a user. Reads our DB only; no Nakama sync/RPC. */
    suspend fun publicUserRaces(uid: String, page: Int = 1): RacesPageDto {
        val resp = getResponse("/users/${uid.encodeURLPathPart()}/races?page=$page")
        val text = resp.bodyAsText()
        if (resp.status.value == 404) throw Exception("user_not_found")
        if (resp.status.value !in 200..299) throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        return ProfileJson.decodeFromString(text)
    }

    /** Public saved race-history category for a user. Same shape as /profile/races/<category>. */
    suspend fun publicUserCategoryRaces(uid: String, category: String, page: Int = 1): RacesPageDto {
        val resp = getResponse("/users/${uid.encodeURLPathPart()}/races/${category.encodeURLPathPart()}?page=$page")
        val text = resp.bodyAsText()
        if (resp.status.value == 404) throw Exception("user_not_found")
        if (resp.status.value !in 200..299) throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        return ProfileJson.decodeFromString(text)
    }

    /** Public race detail for both local saved users and external RaceCenter users. */
    suspend fun publicUserRaceDetail(uid: String, eventId: String, split: Int? = null): RaceDetailDto {
        val path = if (uid.startsWith("racecenter:")) {
            "/users/${uid.encodeURLPathPart()}/external/race/${eventId.encodeURLPathPart()}"
        } else {
            "/users/${uid.encodeURLPathPart()}/race/${eventId.encodeURLPathPart()}"
        }
        val resp = getResponse(path + split?.let { "?split=$it" }.orEmpty())
        val text = resp.bodyAsText()
        when (resp.status.value) {
            404 -> throw Exception(text.publicRaceErrorCode() ?: "race_not_found")
            502 -> throw Exception("nakama_unavailable")
        }
        if (resp.status.value !in 200..299) throw Exception("HTTP ${resp.status.value}: ${text.take(200)}")
        return ProfileJson.decodeFromString(text)
    }

    /** Public saved track history for a user. Same payload shape as /profile/track/<track_id>. */
    suspend fun publicUserTrack(uid: String, trackId: String, patch: String? = null): TrackDetailResponse {
        val qs = patch?.takeIf { it.isNotBlank() }?.let { "?patch=${it.encodeURLQueryComponent()}" }.orEmpty()
        val resp = getResponse("/users/${uid.encodeURLPathPart()}/track/${trackId.encodeURLPathPart()}$qs")
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
        val path = buildString {
            append("/leaderboard/${leaderboardId.encodeURLPathPart()}?limit=$limit")
            if (!cursor.isNullOrBlank()) append("&cursor=${cursor.encodeURLQueryComponent()}")
        }
        val body = getText(path) {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }
        return AppJson.decodeFromString(body)
    }

    private suspend fun getSchedule(pathAndQuery: String): ScheduleResponse {
        val resp = getResponse(pathAndQuery)
        val text = resp.bodyAsText()
        if (resp.status.value !in 200..299) {
            val upstreamError = runCatching {
                AppJson.decodeFromString<BackendErrorResponse>(text).error
            }.getOrNull()
            throw Exception(upstreamError?.takeIf { it.isNotBlank() } ?: "HTTP ${resp.status.value}: ${text.take(200)}")
        }
        return AppJson.decodeFromString(text)
    }

    private suspend fun getText(pathAndQuery: String): String =
        getResponse(pathAndQuery).bodyAsText()

    private suspend fun getText(
        pathAndQuery: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): String =
        getResponse(pathAndQuery, block).bodyAsText()

    private suspend fun getResponse(
        pathAndQuery: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse =
        apiBaseUrlProvider.withBaseUrlRetry { baseUrl ->
            client.get(baseUrl + pathAndQuery.withLeadingSlash()) {
                block()
            }
        }
}

@kotlinx.serialization.Serializable
private data class BackendErrorResponse(val error: String? = null)

private val ScheduleCategory.pathSegment: String
    get() = when (this) {
        ScheduleCategory.RACES -> "daily-weekly"
        ScheduleCategory.SPECIAL -> "special"
        ScheduleCategory.CHAMPIONSHIP -> "championship"
    }

private fun String.publicRaceErrorCode(): String? =
    listOf(
        "user_not_found",
        "race_not_found",
        "external_user_unsupported",
        "local_user_unsupported",
        "nakama_unavailable",
    ).firstOrNull { contains(it) }

private fun String.withLeadingSlash(): String =
    if (startsWith('/')) this else "/$this"
