package com.orioooneee.lmuasister.data.remote

import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.steam.SteamLog
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Response of /auth/steam: exchanges an on-device Steam Web API ticket for our app token. */
@Serializable
data class SteamAuthResponse(val token: String, val uid: String)

/** Steam refresh creds handed back to the device by /auth/steam/login — device-only secret. */
@Serializable
data class SteamCreds(val account: String? = null, val refresh: String? = null, val guard: String? = null)

/** Response of /auth/steam/login & /auth/steam/refresh: our app token (+ device-held steam creds). */
@Serializable
data class AuthLoginResponse(val token: String, val uid: String, val steam: SteamCreds? = null)

/** Short-lived tunnel ticket from /tunnel/ticket — device opens wss://agentUrl?token=. */
@Serializable
data class TunnelTicket(val key: String, val token: String, val agentUrl: String = "", val expiresIn: Int = 0)

@Serializable
private data class ErrBody(val error: String? = null, val kind: String? = null, val detail: String? = null)

// Request bodies. The backend reads camelCase keys (username/password/guardCode/tunnelKey),
// but AppJson is snake_case — so these are encoded with [RequestJson] (no naming strategy).
@Serializable
private data class LoginBody(val username: String, val password: String, val guardCode: String?, val tunnelKey: String)

@Serializable
private data class RefreshBody(val account: String, val refresh: String, val tunnelKey: String)

@Serializable
private data class DemoBody(val username: String, val password: String)

/** Plain JSON for request bodies — keeps property names verbatim (camelCase). */
private val RequestJson = Json { encodeDefaults = true }

/** Backend rejected the ticket / no account (403) — do NOT retry, surface to the user. */
class BackendAuthFailed(message: String) : Exception(message)

/** Session + refresh dead (401 reauth) — re-mint a Steam ticket and retry once. */
class BackendReauthRequired : Exception("reauth")

/** Upstream Nakama / sidecar unavailable (502). */
class BackendUnavailable : Exception("nakama_unavailable")

/** Device egress tunnel required / not connected (428 / 409) — (re)open the tunnel and retry. */
class BackendTunnelRequired : Exception("tunnel_required")

/** Steam Guard code needed (409 need_guard) — prompt and resend with the code. */
class SteamGuardNeeded(val kind: String) : Exception("need_guard:$kind")

/**
 * Our stable REST surface (under {BACKEND_URL} = …/api/v2). The backend hides all the
 * Nakama/session/refresh machinery; we send a Steam Web API ticket once, get our own
 * token, then call profile endpoints with it as a Bearer.
 */
class SteamBackendApi(private val client: HttpClient) {

    /** Exchanges an on-device Steam Web API ticket (hex) for our app token (Android/JVM). */
    suspend fun authSteam(ticketHex: String): SteamAuthResponse {
        val resp = client.post("$API_BASE/auth/steam") {
            contentType(ContentType.Application.Json)
            setBody("""{"ticket":"$ticketHex"}""") // hex is [0-9a-f], no escaping needed
        }
        return when (resp.status.value) {
            in 200..299 -> AppJson.decodeFromString(resp.bodyAsText())
            else -> throw resp.toError()
        }
    }

    /**
     * App-store-review login under the service account (no Steam, no tunnel). Hands back the
     * same {token, uid} shape as [authSteam]; the reviewer then reads /profile* like a real user.
     */
    suspend fun authDemo(username: String, password: String): SteamAuthResponse {
        val resp = client.post("$API_BASE/auth/demo") {
            timeout { requestTimeoutMillis = TICKET_TIMEOUT } // Render cold start
            contentType(ContentType.Application.Json)
            setBody(RequestJson.encodeToString(DemoBody(username, password)))
        }
        return when (resp.status.value) {
            in 200..299 -> AppJson.decodeFromString(resp.bodyAsText())
            else -> throw resp.toError()
        }
    }

    suspend fun profile(token: String): SteamProfile =
        ProfileJson.decodeFromString(getAuthed("$API_BASE/profile", token))

    suspend fun stats(token: String): String = getAuthed("$API_BASE/profile/stats", token)

    /** Track reference block + the caller's personal record on it. trackId = id / code / base.
     *  All-snake-case payload (no camelCase outlier), so it decodes with [AppJson]. */
    suspend fun trackDetail(token: String, trackId: String): TrackDetailResponse =
        AppJson.decodeFromString(getAuthed("$API_BASE/profile/track/${trackId.encodeURLPathPart()}", token))

    /**
     * Drops the server-side session and every piece of data we hold for the user.
     * Idempotent on the backend; treated as best-effort by callers (the device clears its
     * own state regardless). Backs both "Sign out" and "Clear my data".
     */
    suspend fun signOut(token: String) {
        val resp = client.post("$API_BASE/auth/sign-out") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        SteamLog.d("backend: POST /auth/sign-out → ${resp.status.value}")
        if (resp.status.value !in 200..299) throw resp.toError()
    }

    /** One paginated page (5/page) of the player's full race history. */
    suspend fun racesPage(token: String, page: Int): RacesPageDto =
        ProfileJson.decodeFromString(getAuthed("$API_BASE/profile/races?page=$page", token))

    /** Full race-page detail by eventId (split disambiguates which split you raced). */
    suspend fun raceDetail(token: String, eventId: String, split: Int?, page: Int?): RaceDetailDto {
        val qs = buildList {
            split?.let { add("split=$it") }
            page?.let { add("page=$it") }
        }.joinToString("&")
        val url = "$API_BASE/profile/race/${eventId.encodeURLPathPart()}" + if (qs.isEmpty()) "" else "?$qs"
        return ProfileJson.decodeFromString(getAuthed(url, token))
    }

    /** One foreign split's classification (lazy, per-tab). `seriesId` lets the backend skip a history re-read. */
    suspend fun raceSplit(token: String, eventId: String, splitNo: Int, seriesId: String?): SplitDetailDto {
        val qs = seriesId?.takeIf { it.isNotBlank() }?.let { "?series_id=${it.encodeURLQueryComponent()}" } ?: ""
        val url = "$API_BASE/profile/race/${eventId.encodeURLPathPart()}/split/$splitNo$qs"
        return ProfileJson.decodeFromString(getAuthed(url, token))
    }


    /** A short-lived token + agent URL for the device to open its egress tunnel. */
    suspend fun tunnelTicket(): TunnelTicket {
        val resp = client.get("$API_BASE/tunnel/ticket") {
            timeout { requestTimeoutMillis = TICKET_TIMEOUT } // Render cold start
        }
        SteamLog.d("backend: GET /tunnel/ticket → ${resp.status.value}")
        return when (resp.status.value) {
            in 200..299 -> AppJson.decodeFromString(resp.bodyAsText())
            else -> throw resp.toError()
        }
    }

    /** Credentials → app token. The sidecar logs in to Steam through the device tunnel. */
    suspend fun authSteamLogin(
        username: String,
        password: String,
        guardCode: String?,
        tunnelKey: String,
    ): AuthLoginResponse {
        val resp = client.post("$API_BASE/auth/steam/login") {
            // Steam login runs through the device tunnel (slow); must outlast the sidecar's
            // budget so we don't time out and tear the tunnel down mid-login.
            timeout { requestTimeoutMillis = STEAM_TIMEOUT }
            contentType(ContentType.Application.Json)
            setBody(RequestJson.encodeToString(LoginBody(username, password, guardCode, tunnelKey)))
        }
        return decodeAuth(resp)
    }

    /** Silent reauth: device-held Steam refresh → fresh app token. */
    suspend fun authSteamRefresh(account: String, refresh: String, tunnelKey: String): AuthLoginResponse {
        val resp = client.post("$API_BASE/auth/steam/refresh") {
            timeout { requestTimeoutMillis = STEAM_TIMEOUT }
            contentType(ContentType.Application.Json)
            setBody(RequestJson.encodeToString(RefreshBody(account, refresh, tunnelKey)))
        }
        return decodeAuth(resp)
    }

    private suspend fun decodeAuth(resp: HttpResponse): AuthLoginResponse {
        SteamLog.d("backend: auth response → ${resp.status.value}")
        if (resp.status.value in 200..299) return AppJson.decodeFromString(resp.bodyAsText())
        val body = runCatching { AppJson.decodeFromString<ErrBody>(resp.bodyAsText()) }.getOrNull()
        throw when {
            resp.status.value == 409 && body?.error == "need_guard" -> SteamGuardNeeded(body.kind ?: "device")
            resp.status.value == 428 -> BackendTunnelRequired()
            resp.status.value == 409 -> BackendTunnelRequired() // tunnel_not_connected
            resp.status.value == 403 -> BackendAuthFailed(body?.detail ?: body?.error ?: "auth_failed")
            resp.status.value == 401 -> BackendReauthRequired()
            resp.status.value == 502 || resp.status.value == 503 -> BackendUnavailable()
            else -> Exception("HTTP ${resp.status.value}: ${resp.bodyAsText().take(200)}")
        }
    }

    private suspend fun getAuthed(url: String, token: String): String {
        val resp = client.get(url) { header(HttpHeaders.Authorization, "Bearer $token") }
        SteamLog.d("backend: GET ${url.substringAfterLast("/api/v2")} → ${resp.status.value}")
        return when (resp.status.value) {
            in 200..299 -> resp.bodyAsText()
            else -> throw resp.toError()
        }
    }

    private suspend fun HttpResponse.toError(): Exception = when (status.value) {
        401 -> BackendReauthRequired()
        403 -> BackendAuthFailed(bodyAsText().ifBlank { "auth_failed" })
        502 -> BackendUnavailable()
        else -> Exception("HTTP ${status.value}: ${bodyAsText().take(200)}")
    }

    private companion object {
        val API_BASE = BuildConfig.BACKEND_URL.trimEnd('/') // http://host/api/v2
        const val TICKET_TIMEOUT = 60_000L // /tunnel/ticket — tolerate Render cold start
        const val STEAM_TIMEOUT = 120_000L // login/refresh — Steam login via tunnel is slow (>60s)
    }
}
