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
// TUNNEL_DISABLED: used only by the old AuthLoginStartResponse DTO.
// import kotlinx.serialization.ExperimentalSerializationApi
// import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.Json

/** Response of /auth/steam: exchanges an on-device Steam Web API ticket for our app token. */
@Serializable
data class SteamAuthResponse(val token: String, val uid: String)

/*
 * TUNNEL_DISABLED:
 * The backend-side Steam login through a device egress tunnel is intentionally not part
 * of the active mobile auth path anymore. Keep the DTOs commented for restoration/debugging.
 *
 * /** Steam refresh creds handed back to the device by /auth/steam/login — device-only secret. */
 * @Serializable
 * data class SteamCreds(val account: String? = null, val refresh: String? = null, val guard: String? = null)
 *
 * /** Response of /auth/steam/login & /auth/steam/refresh: our app token (+ device-held steam creds). */
 * @Serializable
 * data class AuthLoginResponse(val token: String, val uid: String, val steam: SteamCreds? = null)
 *
 * /** Start response may be immediate success or a pending Steam mobile confirmation challenge. */
 * @OptIn(ExperimentalSerializationApi::class)
 * @Serializable
 * data class AuthLoginStartResponse(
 *     val token: String? = null,
 *     val uid: String? = null,
 *     val steam: SteamCreds? = null,
 *     val status: String? = null,
 *     @JsonNames("challenge_id")
 *     val challengeId: String? = null,
 *     @JsonNames("expires_in")
 *     val expiresIn: Int = 0,
 * )
 *
 * /** Short-lived tunnel ticket from /tunnel/ticket — device opens wss://agentUrl?token=. */
 * @Serializable
 * data class TunnelTicket(val key: String, val token: String, val agentUrl: String = "", val expiresIn: Int = 0)
 */

/*
 * TUNNEL_DISABLED:
 * Error body used by the old tunnel login decoder.
 *
 * @Serializable
 * private data class ErrBody(
 *     val error: String? = null,
 *     val kind: String? = null,
 *     val mode: String? = null,
 *     val detail: String? = null,
 * )
 */

/*
 * TUNNEL_DISABLED:
 * Request bodies for the old backend-side Steam login endpoints. The active flow uses
 * a local kSteam auth ticket and [authSteam] instead.
 *
 * // Request bodies. The backend reads camelCase keys (username/password/guardCode/tunnelKey),
 * // but AppJson is snake_case — so these are encoded with [RequestJson] (no naming strategy).
 * @Serializable
 * private data class LoginBody(
 *     val username: String,
 *     val password: String,
 *     val guardCode: String?,
 *     val guardData: String?,
 *     val tunnelKey: String,
 * )
 *
 * @Serializable
 * private data class LoginStartBody(
 *     val username: String,
 *     val password: String,
 *     val guardData: String?,
 *     val tunnelKey: String,
 * )
 *
 * @Serializable
 * private data class LoginContinueBody(val challengeId: String, val tunnelKey: String)
 *
 * @Serializable
 * private data class RefreshBody(val account: String, val refresh: String, val tunnelKey: String)
 */

@Serializable
private data class DemoBody(val username: String, val password: String)

/** Plain JSON for request bodies — keeps property names verbatim (camelCase). */
private val RequestJson = Json { encodeDefaults = true }

/*
 * TUNNEL_DISABLED:
 * JSON decoder for the old tunnel login endpoints.
 *
 * /** Auth endpoints use camelCase in the mobile contract; accept snake_case aliases too. */
 * private val AuthJson = Json {
 *     ignoreUnknownKeys = true
 *     isLenient = true
 *     coerceInputValues = true
 * }
 */

/** Backend rejected the ticket / no account (403) — do NOT retry, surface to the user. */
class BackendAuthFailed(message: String) : Exception(message)

/** Session + refresh dead (401 reauth) — re-mint a Steam ticket and retry once. */
class BackendReauthRequired : Exception("reauth")

/** Upstream Nakama / sidecar unavailable (502). */
class BackendUnavailable : Exception("nakama_unavailable")

/*
 * TUNNEL_DISABLED:
 * Errors emitted by the old tunnel login endpoints.
 *
 * /** Device egress tunnel required / not connected (428 / 409) — (re)open the tunnel and retry. */
 * class BackendTunnelRequired : Exception("tunnel_required")
 *
 * /** Steam Guard code needed (409 need_guard) — prompt and resend with the code. */
 * class SteamGuardNeeded(val kind: String, val mode: String? = null) : Exception("need_guard:$kind")
 *
 * /** Steam only offered a Guard flow the current client cannot complete. */
 * class UnsupportedGuardFlow(detail: String) : Exception(detail)
 *
 * /** Pending device confirmation no longer exists on the backend. */
 * class SteamChallengeExpired : Exception("Steam Guard approval expired. Try again.")
 */

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
        SteamLog.d("backend: POST /auth/steam → ${resp.status.value}")
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

    /** Drops the server-side session. */
    suspend fun signOut(token: String) {
        val resp = client.post("$API_BASE/auth/sign-out") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        SteamLog.d("backend: POST /auth/sign-out → ${resp.status.value}")
        if (resp.status.value !in 200..299) throw resp.toError()
    }

    /**
     * Clears every piece of server-side data we hold for the user.
     * The backend expects only the Bearer token and no request body.
     */
    suspend fun clearMyData(token: String) {
        val resp = client.post("$API_BASE/auth/clear-my-data") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        SteamLog.d("backend: POST /auth/clear-my-data → ${resp.status.value}")
        if (resp.status.value !in 200..299) throw resp.toError()
    }

    /** One paginated page (5/page) of the player's full race history. */
    suspend fun racesPage(token: String, page: Int): RacesPageDto =
        ProfileJson.decodeFromString(getAuthed("$API_BASE/profile/races?page=$page", token))

    /** One page (30/page) of a single stat category: wins | podiums | poles | fastest_laps | top5. */
    suspend fun categoryRacesPage(token: String, category: String, page: Int): RacesPageDto =
        ProfileJson.decodeFromString(
            getAuthed("$API_BASE/profile/races/${category.encodeURLPathPart()}?page=$page", token),
        )

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


    /*
     * TUNNEL_DISABLED:
     * Old backend-side Steam credential endpoints. The active app signs into Steam with
     * kSteam on-device, then calls [authSteam] with a Steam auth session ticket.
     *
     * /** A short-lived token + agent URL for the device to open its egress tunnel. */
     * suspend fun tunnelTicket(): TunnelTicket {
     *     val resp = client.get("$API_BASE/tunnel/ticket") {
     *         timeout { requestTimeoutMillis = TICKET_TIMEOUT } // Render cold start
     *     }
     *     SteamLog.d("backend: GET /tunnel/ticket → ${resp.status.value}")
     *     return when (resp.status.value) {
     *         in 200..299 -> AppJson.decodeFromString(resp.bodyAsText())
     *         else -> throw resp.toError()
     *     }
     * }
     *
     * /** Credentials → app token. The sidecar logs in to Steam through the device tunnel. */
     * suspend fun authSteamLogin(
     *     username: String,
     *     password: String,
     *     guardCode: String?,
     *     guardData: String?,
     *     tunnelKey: String,
     * ): AuthLoginResponse {
     *     val resp = client.post("$API_BASE/auth/steam/login") {
     *         // Steam login runs through the device tunnel (slow); must outlast the sidecar's
     *         // budget so we don't time out and tear the tunnel down mid-login.
     *         timeout { requestTimeoutMillis = STEAM_TIMEOUT }
     *         contentType(ContentType.Application.Json)
     *         setBody(RequestJson.encodeToString(LoginBody(username, password, guardCode, guardData, tunnelKey)))
     *     }
     *     return decodeAuth(resp)
     * }
     *
     * /** Credentials → immediate app token or pending Steam mobile approval challenge. */
     * suspend fun authSteamLoginStart(
     *     username: String,
     *     password: String,
     *     guardData: String?,
     *     tunnelKey: String,
     * ): AuthLoginStartResponse {
     *     val resp = client.post("$API_BASE/auth/steam/login/start") {
     *         timeout { requestTimeoutMillis = STEAM_TIMEOUT }
     *         contentType(ContentType.Application.Json)
     *         setBody(RequestJson.encodeToString(LoginStartBody(username, password, guardData, tunnelKey)))
     *     }
     *     SteamLog.d("backend: login/start response → ${resp.status.value}")
     *     val text = resp.bodyAsText()
     *     if (resp.status.value in 200..299) return AuthJson.decodeFromString(text)
     *     throw decodeAuthError(resp.status.value, text)
     * }
     *
     * /** Continue a pending Steam mobile approval challenge using a fresh device tunnel. */
     * suspend fun authSteamLoginContinue(challengeId: String, tunnelKey: String): AuthLoginResponse {
     *     val resp = client.post("$API_BASE/auth/steam/login/continue") {
     *         timeout { requestTimeoutMillis = STEAM_TIMEOUT }
     *         contentType(ContentType.Application.Json)
     *         setBody(RequestJson.encodeToString(LoginContinueBody(challengeId, tunnelKey)))
     *     }
     *     return decodeAuth(resp)
     * }
     *
     * /** Silent reauth: device-held Steam refresh → fresh app token. */
     * suspend fun authSteamRefresh(account: String, refresh: String, tunnelKey: String): AuthLoginResponse {
     *     val resp = client.post("$API_BASE/auth/steam/refresh") {
     *         timeout { requestTimeoutMillis = STEAM_TIMEOUT }
     *         contentType(ContentType.Application.Json)
     *         setBody(RequestJson.encodeToString(RefreshBody(account, refresh, tunnelKey)))
     *     }
     *     return decodeAuth(resp)
     * }
     *
     * private suspend fun decodeAuth(resp: HttpResponse): AuthLoginResponse {
     *     SteamLog.d("backend: auth response → ${resp.status.value}")
     *     val text = resp.bodyAsText()
     *     if (resp.status.value in 200..299) return AppJson.decodeFromString(text)
     *     throw decodeAuthError(resp.status.value, text)
     * }
     *
     * private fun decodeAuthError(status: Int, text: String): Exception {
     *     val body = runCatching { AppJson.decodeFromString<ErrBody>(text) }.getOrNull()
     *     return when {
     *         body?.error == "need_guard" -> SteamGuardNeeded(body.kind ?: "device", body.mode)
     *         body?.error == "tunnel_not_connected" -> BackendTunnelRequired()
     *         body?.error == "challenge_not_found" -> SteamChallengeExpired()
     *         body?.error == "unsupported_guard_flow" ->
     *             UnsupportedGuardFlow(unsupportedGuardMessage(body.detail))
     *         body?.error == "approval_timeout" -> BackendAuthFailed("Steam Guard approval expired. Try again.")
     *         body?.error == "auth_failed" -> BackendAuthFailed(body.detail ?: "auth_failed")
     *         status == 404 -> SteamChallengeExpired()
     *         status == 428 -> BackendTunnelRequired()
     *         status == 409 -> BackendTunnelRequired() // Backward-compatible tunnel_not_connected.
     *         status == 403 -> BackendAuthFailed(body?.detail ?: body?.error ?: "auth_failed")
     *         status == 401 -> BackendReauthRequired()
     *         status == 502 || status == 503 -> BackendUnavailable()
     *         else -> Exception("HTTP $status: ${text.take(200)}")
     *     }
     * }
     */

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

    /*
     * TUNNEL_DISABLED:
     * Tunnel-login-only Guard messaging.
     *
     * private fun unsupportedGuardMessage(detail: String?): String =
     *     when (detail) {
     *         "device_confirmation_only" ->
     *             "Steam requires mobile app approval for this account. This iOS version supports Steam Guard codes only."
     *         else -> "Steam Guard mobile approval is not supported on this device yet."
     *     }
     */

    private companion object {
        val API_BASE = BuildConfig.BACKEND_URL.trimEnd('/') // http://host/api/v2
        const val TICKET_TIMEOUT = 60_000L // /tunnel/ticket — tolerate Render cold start
        /*
         * TUNNEL_DISABLED:
         * const val STEAM_TIMEOUT = 120_000L // login/refresh — Steam login via tunnel is slow (>60s)
         */
    }
}
