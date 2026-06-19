package com.orioooneee.lmuasister.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.AppJson
import com.orioooneee.lmuasister.data.remote.AppTokenHolder
import com.orioooneee.lmuasister.data.remote.BackendAuthFailed
import com.orioooneee.lmuasister.data.remote.BackendReauthRequired
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.RacesPageDto
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

private const val PROFILE_CACHE_KEY = "steam_profile_v2"

/**
 * Drives the Steam login form via the platform [SteamSignIn] (device tunnel on
 * Android/JVM/iOS), then loads the profile with the resulting app token.
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
                if (cached == null) _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                loadProfile()
            } else {
                SteamLog.d("vm: no saved session")
                // Keep the cached profile if we have one; otherwise fall to the login form.
                if (cached == null) _state.value = SteamLoginUiState.Idle
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
        viewModelScope.launch {
            when (val r = signIn.signIn(username, password, guardCode)) {
                is SignInOutcome.Success -> {
                    appToken = r.appToken
                    _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                    loadProfile()
                }
                is SignInOutcome.GuardRequired -> _state.value = SteamLoginUiState.GuardRequired(r.kind)
                SignInOutcome.TunnelRequired ->
                    _state.value = SteamLoginUiState.Error("Couldn't open the device tunnel — try again.")
                is SignInOutcome.Failure -> _state.value = SteamLoginUiState.Error(r.reason)
            }
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

    fun signOut() {
        signIn.signOut()
        appToken = null
        runCatching { LocalCache.write(PROFILE_CACHE_KEY, "") }
        _state.value = SteamLoginUiState.Idle
    }

    private suspend fun loadProfile() {
        // Offline-first: paint cache instantly.
        val cached = loadCachedProfile()
        if (cached != null) _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(cached, fromCache = true))

        val fresh = runCatching { fetchProfileWithReauth() }
        fresh.onSuccess { p ->
            SteamLog.d("vm: profile loaded (uid=${p.uid}, races=${p.recentRaces.size})")
            saveCachedProfile(p)
            _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(p, fromCache = false))
        }.onFailure { e ->
            SteamLog.e("vm: profile load failed (cached=${cached != null})", e)
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
        val token = appToken ?: throw IllegalStateException("not signed in")
        return try {
            block(token)
        } catch (e: BackendReauthRequired) {
            val fresh = signIn.reauth() ?: throw e
            appToken = fresh
            block(fresh)
        }
    }

    private fun loadCachedProfile(): SteamProfile? =
        LocalCache.read(PROFILE_CACHE_KEY)?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AppJson.decodeFromString<SteamProfile>(it) }.getOrNull() }

    private fun saveCachedProfile(p: SteamProfile) {
        runCatching { LocalCache.write(PROFILE_CACHE_KEY, AppJson.encodeToString(p)) }
    }
}
