package com.orioooneee.lmuasister.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.analytics.TelemetryError
import com.orioooneee.lmuasister.analytics.UserProperties
import com.orioooneee.lmuasister.config.BuildConfig
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

sealed interface BackendState {
    data object Loading : BackendState
    data class Ok(val profile: SteamProfile, val fromCache: Boolean = false) : BackendState
    data class AuthFailed(val message: String) : BackendState
    data class Error(val message: String) : BackendState
}

sealed interface SteamLoginUiState {
    data object Idle : SteamLoginUiState

    data object Restoring : SteamLoginUiState
    data object Loading : SteamLoginUiState

    data class GuardRequired(val kind: SteamGuardKind) : SteamLoginUiState

    data class SignedIn(val backend: BackendState = BackendState.Loading) : SteamLoginUiState

    data class Error(val message: String) : SteamLoginUiState
}

enum class ExitAction { NONE, SIGNING_OUT, CLEARING }

data class AllRacesUi(
    val races: List<RecentRaceDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

private const val PROFILE_CACHE_KEY = "steam_profile_v2"

// App-store-review login: the reviewer types these into the normal form; we route them to
// /auth/demo (a service-account session) instead of a real Steam sign-in. Configured via
// local.properties (demo.username / demo.password); defaults match the backend's.
private val DEMO_USERNAME = BuildConfig.DEMO_USERNAME
private val DEMO_PASSWORD = BuildConfig.DEMO_PASSWORD
// Marks the active session as a demo one so we silently re-mint it on restart / 401.
private const val DEMO_FLAG_KEY = "auth_demo_session"

private const val AUTH_WAIT_MS = 60_000L

class SteamLoginViewModel(
    private val signIn: SteamSignIn,
    private val backend: SteamBackendApi,
    private val tokenHolder: AppTokenHolder,
) : ViewModel() {

    private val _state = MutableStateFlow<SteamLoginUiState>(SteamLoginUiState.Restoring)
    val state: StateFlow<SteamLoginUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _exiting = MutableStateFlow(ExitAction.NONE)
    val exiting: StateFlow<ExitAction> = _exiting.asStateFlow()

    private val _allRaces = MutableStateFlow(AllRacesUi())
    val allRacesState: StateFlow<AllRacesUi> = _allRaces.asStateFlow()

    val guardRequired: Boolean get() = _state.value is SteamLoginUiState.GuardRequired

    private var appToken: String? = null
        set(value) {
            field = value
            tokenHolder.set(value)
        }

    init {
        val cached = loadCachedProfile()
        if (cached != null) {
            _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(cached, fromCache = true))
        }
        viewModelScope.launch {
            if (LocalCache.read(DEMO_FLAG_KEY) == "1") {
                SteamLog.d("vm: restoring demo session…")
                val r = runCatching { backend.authDemo(DEMO_USERNAME, DEMO_PASSWORD) }.getOrNull()
                if (r != null) {
                    appToken = r.token
                    Telemetry.log(AnalyticsEvent.LoginSuccess(restored = true))
                    Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                    if (cached == null) _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                    loadProfile()
                    return@launch
                }
                SteamLog.d("vm: demo restore failed, falling back")
                runCatching { LocalCache.write(DEMO_FLAG_KEY, "") }
            }
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
        if (username == DEMO_USERNAME && password == DEMO_PASSWORD) {
            loginDemo()
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

    /** App-store-review path: no Steam, just exchange the demo creds for a service-account token. */
    private fun loginDemo() {
        _state.value = SteamLoginUiState.Loading
        Telemetry.log(AnalyticsEvent.LoginSubmitted(has2fa = false))
        viewModelScope.launch {
            runCatching { backend.authDemo(DEMO_USERNAME, DEMO_PASSWORD) }
                .onSuccess { r ->
                    appToken = r.token
                    runCatching { LocalCache.write(DEMO_FLAG_KEY, "1") }
                    Telemetry.log(AnalyticsEvent.LoginSuccess(restored = false))
                    Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                    _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                    loadProfile()
                }
                .onFailure { e ->
                    SteamLog.e("vm: demo login failed", e)
                    Telemetry.log(AnalyticsEvent.LoginFailed(loginFailReason(e.message ?: "")))
                    Telemetry.recordError(e, "stage" to "login_demo")
                    _state.value = SteamLoginUiState.Error(e.message ?: "Login failed")
                }
        }
    }

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

    fun signOut() = exit(ExitAction.SIGNING_OUT, AnalyticsEvent.ProfileSignedOut)

    fun clearMyData() = exit(ExitAction.CLEARING, AnalyticsEvent.ProfileDataCleared)

    private fun exit(action: ExitAction, event: AnalyticsEvent) {
        if (_exiting.value != ExitAction.NONE) return
        _exiting.value = action
        Telemetry.log(event)
        viewModelScope.launch {
            appToken?.let { token ->
                runCatching { backend.signOut(token) }
                    .onFailure { SteamLog.e("vm: backend sign-out failed", it) }
            }
            Telemetry.setUserId(null)
            Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "false")
            signIn.signOut()
            appToken = null
            runCatching { LocalCache.write(PROFILE_CACHE_KEY, "") }
            runCatching { LocalCache.write(DEMO_FLAG_KEY, "") }
            _allRaces.value = AllRacesUi()
            _exiting.value = ExitAction.NONE
            _state.value = SteamLoginUiState.Idle
        }
    }

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
            }
        }
    }

    private suspend fun fetchProfileWithReauth(): SteamProfile = withReauth { backend.profile(it) }

    suspend fun racesPage(page: Int): RacesPageDto = withReauth { backend.racesPage(it, page) }

    suspend fun raceDetail(eventId: String, split: Int?, page: Int?): RaceDetailDto =
        withReauth { backend.raceDetail(it, eventId, split, page) }

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
            val fresh = reauthToken() ?: throw e
            appToken = fresh
            block(fresh)
        }
    }

    /** Silent reauth → fresh app token: re-mint the demo session, else a real Steam reauth. */
    private suspend fun reauthToken(): String? =
        if (LocalCache.read(DEMO_FLAG_KEY) == "1")
            runCatching { backend.authDemo(DEMO_USERNAME, DEMO_PASSWORD).token }.getOrNull()
        else signIn.reauth()

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
