package com.orioooneee.lmuasister.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.analytics.TelemetryError
import com.orioooneee.lmuasister.analytics.UserProperties
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.AppJson
import com.orioooneee.lmuasister.data.remote.AppTokenHolder
import com.orioooneee.lmuasister.data.remote.BackendAuthFailed
import com.orioooneee.lmuasister.data.remote.BackendReauthRequired
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.RacesPageDto
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.data.steam.SignInOutcome
import com.orioooneee.lmuasister.data.steam.SteamGuardKind
import com.orioooneee.lmuasister.data.steam.SteamLog
import com.orioooneee.lmuasister.data.steam.SteamSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

/** State of the backend profile call chain (auth → profile). */
sealed interface BackendState {
    data object Loading : BackendState
    data class Ok(val profile: SteamProfile, val fromCache: Boolean = false) : BackendState
    data class AuthFailed(val message: String) : BackendState
    data class Error(val message: String) : BackendState
}

sealed interface SteamLoginUiState {
    data object Idle : SteamLoginUiState

    /** Startup: silently restoring a saved session — show a loader, not the login form. */
    data object Restoring : SteamLoginUiState
    data object Loading : SteamLoginUiState

    /** Steam asked for a Guard code — the UI should enable the 2FA field. */
    data class GuardRequired(val kind: SteamGuardKind) : SteamLoginUiState

    /** Signed in; [backend] carries the profile (offline-first). */
    data class SignedIn(val backend: BackendState = BackendState.Loading) : SteamLoginUiState

    data class Error(val message: String) : SteamLoginUiState
}

/** Which exit action is currently running — drives the two profile buttons' loaders. */
enum class ExitAction { NONE, SIGNING_OUT, CLEARING }

/** Paginated full race history for the "See all races" screen (kept in the VM). */
data class AllRacesUi(
    val races: List<RecentRaceDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

private const val PROFILE_CACHE_KEY = "steam_profile_v2"

// How long a token-gated call waits for the session to finish restoring before giving up.
private const val AUTH_WAIT_MS = 60_000L

/**
 * Drives the Steam login form via the platform [SteamSignIn] (on-device JavaSteam on
 * Android/JVM, device tunnel on iOS), then loads the profile with the resulting app token.
 */
class SteamLoginViewModel(
    private val signIn: SteamSignIn,
    private val backend: SteamBackendApi,
    private val tokenHolder: AppTokenHolder,
) : ViewModel() {

    private val _state = MutableStateFlow<SteamLoginUiState>(SteamLoginUiState.Restoring)
    val state: StateFlow<SteamLoginUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    // Which of the two exit buttons is mid-flight (so each can show its own loader and both
    // can be disabled while one runs).
    private val _exiting = MutableStateFlow(ExitAction.NONE)
    val exiting: StateFlow<ExitAction> = _exiting.asStateFlow()

    // Full race-history pagination, held here (not in the screen) so it survives navigating
    // into a race and back — the screen's own state would be lost when it leaves the back stack.
    private val _allRaces = MutableStateFlow(AllRacesUi())
    val allRacesState: StateFlow<AllRacesUi> = _allRaces.asStateFlow()

    val guardRequired: Boolean get() = _state.value is SteamLoginUiState.GuardRequired

    // Mirrors every change into the shared holder so token-optional calls (leaderboard
    // "your position") can pick it up.
    private var appToken: String? = null
        set(value) {
            field = value
            tokenHolder.set(value)
        }

    init {
        // Optimistic UI: if we cached a profile last session, paint it instantly (no loader)
        // while the token is silently refreshed in the background.
        val cached = loadCachedProfile()
        if (cached != null) {
            _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(cached, fromCache = true))
        }
        viewModelScope.launch {
            SteamLog.d("vm: trying silent session restore…")
            val token = runCatching { signIn.restore() }.getOrNull()
            if (token != null) {
                SteamLog.d("vm: session restored, loading profile")
                appToken = token
                Telemetry.log(AnalyticsEvent.LoginSuccess(restored = true))
                Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                if (cached == null) _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                loadProfile()
            } else {
                SteamLog.d("vm: no saved session")
                // Keep the cached profile if we have one; otherwise fall to the login form.
                if (cached == null) {
                    _state.value = SteamLoginUiState.Idle
                    Telemetry.log(AnalyticsEvent.LoginFormShown)
                }
            }
        }
    }

    fun login(username: String, password: String, guardCode: String?) {
        if (_state.value is SteamLoginUiState.Loading) return
        if (username.isBlank() || password.isBlank()) {
            _state.value = SteamLoginUiState.Error("Enter your login and password.")
            return
        }
        _state.value = SteamLoginUiState.Loading
        Telemetry.log(AnalyticsEvent.LoginSubmitted(has2fa = !guardCode.isNullOrBlank()))
        viewModelScope.launch {
            when (val r = signIn.signIn(username, password, guardCode)) {
                is SignInOutcome.Success -> {
                    appToken = r.appToken
                    Telemetry.log(AnalyticsEvent.LoginSuccess(restored = false))
                    Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                    _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                    loadProfile()
                }
                is SignInOutcome.GuardRequired -> {
                    Telemetry.log(AnalyticsEvent.Login2faRequired(r.kind.name.lowercase()))
                    _state.value = SteamLoginUiState.GuardRequired(r.kind)
                }
                SignInOutcome.TunnelRequired -> {
                    Telemetry.log(AnalyticsEvent.LoginTunnelRequired)
                    Telemetry.recordError(TelemetryError("login_tunnel_required"), "stage" to "login")
                    _state.value = SteamLoginUiState.Error("Couldn't open the device tunnel — try again.")
                }
                is SignInOutcome.Failure -> {
                    Telemetry.log(AnalyticsEvent.LoginFailed(loginFailReason(r.reason)))
                    Telemetry.recordError(TelemetryError("login_failed: ${loginFailReason(r.reason)}"), "stage" to "login")
                    _state.value = SteamLoginUiState.Error(r.reason)
                }
            }
        }
    }

    /** Bucket the freeform Steam error into a low-cardinality reason for analytics. */
    private fun loginFailReason(raw: String): String {
        val r = raw.lowercase()
        return when {
            "password" in r || "credential" in r || "invalid" in r -> "bad_credentials"
            "guard" in r || "code" in r || "2fa" in r -> "guard_invalid"
            "rate" in r || "too many" in r -> "rate_limited"
            "network" in r || "timeout" in r || "connect" in r -> "network"
            else -> "other"
        }
    }

    /** Pull-to-refresh on the profile: re-fetch (with one silent reauth on 401). */
    fun refresh() {
        if (appToken == null || _refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { fetchProfileWithReauth() }
                .onSuccess { p ->
                    saveCachedProfile(p)
                    _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(p, fromCache = false))
                }
                .onFailure { SteamLog.e("vm: refresh failed", it) }
            _refreshing.value = false
        }
    }

    /** Plain sign out: tells the backend to drop our session/data, then clears the device. */
    fun signOut() = exit(ExitAction.SIGNING_OUT, AnalyticsEvent.ProfileSignedOut)

    /** "Clear my data" (App Review 5.1.1(v)): same backend call, shown behind a confirm popup. */
    fun clearMyData() = exit(ExitAction.CLEARING, AnalyticsEvent.ProfileDataCleared)

    private fun exit(action: ExitAction, event: AnalyticsEvent) {
        if (_exiting.value != ExitAction.NONE) return
        _exiting.value = action
        Telemetry.log(event)
        viewModelScope.launch {
            // Best-effort: the endpoint is idempotent and wipes all server-side data we hold.
            // Whatever happens, the device still clears its own state below.
            appToken?.let { token ->
                runCatching { backend.signOut(token) }
                    .onFailure { SteamLog.e("vm: backend sign-out failed", it) }
            }
            Telemetry.setUserId(null)
            Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "false")
            signIn.signOut()
            appToken = null
            runCatching { LocalCache.write(PROFILE_CACHE_KEY, "") }
            _allRaces.value = AllRacesUi()
            _exiting.value = ExitAction.NONE
            _state.value = SteamLoginUiState.Idle
        }
    }

    /** Loads the next page of the full race history (no-op while loading / at the end). */
    fun loadMoreAllRaces() {
        val cur = _allRaces.value
        if (cur.loading || !cur.hasMore) return
        viewModelScope.launch {
            _allRaces.value = cur.copy(loading = true, error = null)
            runCatching { racesPage(cur.page + 1) }
                .onSuccess { p ->
                    _allRaces.value = _allRaces.value.copy(
                        races = _allRaces.value.races + p.races,
                        page = cur.page + 1,
                        hasMore = p.hasMore && p.races.isNotEmpty(),
                        loading = false,
                    )
                }
                .onFailure { e ->
                    _allRaces.value = _allRaces.value.copy(error = e.message ?: "Couldn't load races", loading = false)
                }
        }
    }

    fun retryAllRaces() {
        _allRaces.value = _allRaces.value.copy(error = null)
        loadMoreAllRaces()
    }

    private suspend fun loadProfile() {
        // Offline-first: paint cache instantly.
        val cached = loadCachedProfile()
        if (cached != null) _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(cached, fromCache = true))

        val fresh = runCatching { fetchProfileWithReauth() }
        fresh.onSuccess { p ->
            SteamLog.d("vm: profile loaded (uid=${p.uid}, races=${p.recentRaces.size})")
            saveCachedProfile(p)
            applyProfileTelemetry(p)
            Telemetry.log(AnalyticsEvent.ProfileLoaded(fromCache = false))
            _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(p, fromCache = false))
        }.onFailure { e ->
            SteamLog.e("vm: profile load failed (cached=${cached != null})", e)
            Telemetry.recordError(e, "stage" to "profile_load", "had_cache" to (cached != null))
            if (cached == null) {
                _state.value = SteamLoginUiState.SignedIn(
                    if (e is BackendAuthFailed) BackendState.AuthFailed(e.message ?: "auth_failed")
                    else BackendState.Error(e.message ?: e::class.simpleName ?: "error"),
                )
            } // else keep showing the cached profile
        }
    }

    /** Fetches the profile; on a 401 does one silent reauth + retry. */
    private suspend fun fetchProfileWithReauth(): SteamProfile = withReauth { backend.profile(it) }

    /** One page of the full race history (for the "See all" screen). */
    suspend fun racesPage(page: Int): RacesPageDto = withReauth { backend.racesPage(it, page) }

    /** Full race-page detail by eventId. */
    suspend fun raceDetail(eventId: String, split: Int?, page: Int?): RaceDetailDto =
        withReauth { backend.raceDetail(it, eventId, split, page) }

    /** Runs [block] with the app token; on a 401 does one silent reauth + retry. */
    private suspend fun <T> withReauth(block: suspend (token: String) -> T): T {
        // Auth may still be restoring at launch (Render cold start). Wait for the token
        // instead of failing instantly with "not signed in" — callers paint a loader
        // meanwhile. Only give up if it genuinely never arrives.
        val token = appToken ?: tokenHolder.await(AUTH_WAIT_MS)
            ?: throw IllegalStateException("not signed in")
        return try {
            block(token)
        } catch (e: BackendReauthRequired) {
            Telemetry.log(AnalyticsEvent.ProfileReauthTriggered)
            val fresh = signIn.reauth() ?: throw e
            appToken = fresh
            block(fresh)
        }
    }

    /** Ties the anonymous session id + skill segment to this signed-in user. */
    private fun applyProfileTelemetry(p: SteamProfile) {
        Telemetry.setUserId(p.uid.takeIf { it.isNotBlank() })
        Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
        Telemetry.userProperty(UserProperties.DRIVER_RANK, p.driverRating?.rank?.takeIf { it.isNotBlank() })
        Telemetry.userProperty(UserProperties.SAFETY_RANK, p.safetyRating?.rank?.takeIf { it.isNotBlank() })
    }

    private fun loadCachedProfile(): SteamProfile? =
        LocalCache.read(PROFILE_CACHE_KEY)?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AppJson.decodeFromString<SteamProfile>(it) }.getOrNull() }

    private fun saveCachedProfile(p: SteamProfile) {
        runCatching { LocalCache.write(PROFILE_CACHE_KEY, AppJson.encodeToString(p)) }
    }
}
