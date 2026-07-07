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
import com.orioooneee.lmuasister.data.remote.SplitDetailDto
import com.orioooneee.lmuasister.data.remote.TrackDetailResponse
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.data.steam.SignInOutcome
import com.orioooneee.lmuasister.data.steam.SteamAchievements
import com.orioooneee.lmuasister.data.steam.SteamAchievementsClient
import com.orioooneee.lmuasister.data.steam.SteamAuthEnvironment
import com.orioooneee.lmuasister.data.steam.SteamAuthEnvironmentUnavailable
import com.orioooneee.lmuasister.data.steam.SteamGuardKind
import com.orioooneee.lmuasister.data.steam.SteamLog
import com.orioooneee.lmuasister.data.steam.SteamRestoreTimedOut
import com.orioooneee.lmuasister.data.steam.SteamSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.time.Clock

sealed interface BackendState {
    data object Loading : BackendState
    data class Ok(val profile: SteamProfile, val fromCache: Boolean = false) : BackendState
    data class AuthFailed(val message: String) : BackendState
    data class Error(val message: String) : BackendState
}

sealed interface SteamLoginUiState {
    data object Idle : SteamLoginUiState

    data object Restoring : SteamLoginUiState
    data object CheckingAuthEnvironment : SteamLoginUiState
    data class LocalNetworkPermissionRequired(val denied: Boolean = false) : SteamLoginUiState
    data object RequestingLocalNetworkPermission : SteamLoginUiState
    data class MinterUnavailable(val message: String) : SteamLoginUiState
    data object Loading : SteamLoginUiState
    data object QrCodeStarting : SteamLoginUiState

    data class GuardRequired(val kind: SteamGuardKind) : SteamLoginUiState
    data class DeviceConfirmationPending(
        val challengeId: String,
        val expiresIn: Int,
        val continuing: Boolean = false,
    ) : SteamLoginUiState
    data class QrCodePending(
        val flowId: String,
        val challengeUrl: String,
        val displayCode: String?,
        val expiresIn: Int,
        val checking: Boolean = false,
    ) : SteamLoginUiState

    data class SignedIn(
        val backend: BackendState = BackendState.Loading,
        val restored: Boolean = false,
    ) : SteamLoginUiState

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

/** Paginated drill-down for one stat category (wins/podiums/poles/fastest_laps/top5). */
data class CategoryRacesUi(
    val category: String? = null,
    val races: List<RecentRaceDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

data class AchievementsUi(
    val data: SteamAchievements? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val fromCache: Boolean = false,
    val error: String? = null,
)

private const val PROFILE_CACHE_KEY = "steam_profile_v3"
private const val ACHIEVEMENTS_CACHE_KEY = "steam_achievements_v2"
private const val ACHIEVEMENTS_PROFILE_REFRESH_COOLDOWN_MS = 60_000L

// App-store-review login: the reviewer types these into the normal form; we route them to
// /auth/demo (a service-account session) instead of a real Steam sign-in. Configured via
// local.properties (demo.username / demo.password); defaults match the backend's.
private val DEMO_USERNAME = BuildConfig.DEMO_USERNAME
private val DEMO_PASSWORD = BuildConfig.DEMO_PASSWORD
// Marks the active session as a demo one so we silently re-mint it on restart / 401.
private const val DEMO_FLAG_KEY = "auth_demo_session"

private const val AUTH_WAIT_MS = 10_000L

private sealed interface AutoContinuation {
    data class DeviceApproval(val challengeId: String) : AutoContinuation
    data class QrCode(val flowId: String) : AutoContinuation
}

private class ReauthLoginRequired(message: String) : RuntimeException(message)

class SteamLoginViewModel(
    private val signIn: SteamSignIn,
    private val authRunner: SteamAuthRunner,
    private val backend: SteamBackendApi,
    private val tokenHolder: AppTokenHolder,
    private val achievementsClient: SteamAchievementsClient,
) : ViewModel() {

    private val _state = MutableStateFlow<SteamLoginUiState>(SteamLoginUiState.Restoring)
    val state: StateFlow<SteamLoginUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _updatingProfile = MutableStateFlow(false)
    val updatingProfile: StateFlow<Boolean> = _updatingProfile.asStateFlow()

    private val _exiting = MutableStateFlow(ExitAction.NONE)
    val exiting: StateFlow<ExitAction> = _exiting.asStateFlow()

    private val _allRaces = MutableStateFlow(AllRacesUi())
    val allRacesState: StateFlow<AllRacesUi> = _allRaces.asStateFlow()

    private val _categoryRaces = MutableStateFlow(CategoryRacesUi())
    val categoryRacesState: StateFlow<CategoryRacesUi> = _categoryRaces.asStateFlow()

    private val _achievements = MutableStateFlow(
        loadCachedAchievements()?.let { AchievementsUi(data = it, fromCache = true) } ?: AchievementsUi(),
    )
    val achievementsState: StateFlow<AchievementsUi> = _achievements.asStateFlow()
    private var lastAchievementsRefreshMs = 0L

    val guardRequired: Boolean get() = _state.value is SteamLoginUiState.GuardRequired

    private var appToken: String? = null
        set(value) {
            field = value
            tokenHolder.set(value)
        }
    private var restoreFinished = false
    private var handledAuthResultId = 0L

    init {
        val cached = loadCachedProfile()
        SteamLog.d("vm: init cachedProfile=${cached != null} cachedUid=${SteamLog.short(cached?.uid)}")
        if (cached != null) {
            _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(cached, fromCache = true))
        }
        observeAuthRunner()
        viewModelScope.launch {
            if (authRunner.state.value !is SteamAuthRunnerState.Idle) {
                SteamLog.d("vm: auth runner already active, skipping restore")
                restoreFinished = true
                return@launch
            }
            try {
                if (LocalCache.read(DEMO_FLAG_KEY) == "1") {
                    SteamLog.d("vm: restoring demo session...")
                    val r = runCatching { backend.authDemo(DEMO_USERNAME, DEMO_PASSWORD) }.getOrNull()
                    if (r != null) {
                        appToken = r.token
                        Telemetry.log(AnalyticsEvent.LoginSuccess(restored = true))
                        Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                        if (cached == null) _state.value = SteamLoginUiState.SignedIn(
                            BackendState.Loading,
                            restored = true,
                        )
                        loadProfile()
                        return@launch
                    }
                    SteamLog.d("vm: demo restore failed, falling back")
                    runCatching { LocalCache.write(DEMO_FLAG_KEY, "") }
                }
                SteamLog.d("vm: trying silent session restore...")
                val restored = runCatching { signIn.restore() }
                val token = restored.getOrNull()
                if (token != null) {
                    SteamLog.d("vm: session restored, loading profile tokenLen=${token.length}")
                    appToken = token
                    Telemetry.log(AnalyticsEvent.LoginSuccess(restored = true))
                    Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                    if (cached == null) _state.value = SteamLoginUiState.SignedIn(
                        BackendState.Loading,
                        restored = true,
                    )
                    loadProfile()
                } else {
                    val restoreError = restored.exceptionOrNull()
                    if (restoreError is SteamRestoreTimedOut) {
                        SteamLog.d("vm: saved session restore timed out")
                        forceLoginAfterMissingSession(clearSteamSession = false)
                    } else {
                        restoreError?.let { SteamLog.e("vm: silent session restore failed", it) }
                        SteamLog.d("vm: no saved session")
                        forceLoginAfterMissingSession(clearSteamSession = true)
                    }
                }
            } finally {
                restoreFinished = true
            }
        }
    }

    fun grantLocalNetworkPermission() {
        if (_state.value is SteamLoginUiState.RequestingLocalNetworkPermission) return
        _state.value = SteamLoginUiState.RequestingLocalNetworkPermission
        viewModelScope.launch {
            applyAuthEnvironment(
                runCatching { signIn.requestAuthEnvironmentAccess() }
                    .getOrElse { SteamAuthEnvironment.CompanionUnavailable(it.message ?: "Couldn't check JVM minter.") },
            )
        }
    }

    fun retryAuthEnvironmentCheck() {
        if (_state.value is SteamLoginUiState.CheckingAuthEnvironment ||
            _state.value is SteamLoginUiState.RequestingLocalNetworkPermission
        ) return
        viewModelScope.launch {
            showLoginAfterPreflight()
        }
    }

    fun login(username: String, password: String, guardCode: String?) {
        val currentState = _state.value
        val code = guardCode?.trim().orEmpty()
        if (currentState is SteamLoginUiState.DeviceConfirmationPending && code.isNotBlank()) {
            SteamLog.d("vm: typed guard code submitted while approval pending")
            _state.value = SteamLoginUiState.Loading
            authRunner.cancelRunning("typed_guard_code_while_approval_pending")
            authRunner.startSignIn(username, password, code)
            return
        }
        if (currentState is SteamLoginUiState.Loading ||
            currentState is SteamLoginUiState.QrCodeStarting ||
            authRunner.state.value is SteamAuthRunnerState.Running
        ) return
        if (username.isBlank() || password.isBlank()) {
            _state.value = SteamLoginUiState.Error("Enter your login and password.")
            return
        }
        if (username == DEMO_USERNAME && password == DEMO_PASSWORD) {
            loginDemo()
            return
        }
        SteamLog.d("vm: login submitted has2fa=${code.isNotBlank()}")
        _state.value = SteamLoginUiState.Loading
        Telemetry.log(AnalyticsEvent.LoginSubmitted(has2fa = code.isNotBlank()))
        authRunner.startSignIn(username, password, code)
    }

    fun loginWithQr() {
        val currentState = _state.value
        if (currentState is SteamLoginUiState.Loading ||
            currentState is SteamLoginUiState.QrCodeStarting ||
            authRunner.state.value is SteamAuthRunnerState.Running
        ) return
        SteamLog.d("vm: QR login submitted")
        _state.value = SteamLoginUiState.QrCodeStarting
        Telemetry.log(AnalyticsEvent.LoginSubmitted(has2fa = false))
        val started = authRunner.startQrSignIn()
        if (!started && authRunner.state.value !is SteamAuthRunnerState.Running) {
            _state.value = SteamLoginUiState.Error("Steam QR sign-in could not be started. Try again.")
        }
    }

    fun expireDeviceConfirmation() {
        val pending = _state.value as? SteamLoginUiState.DeviceConfirmationPending ?: return
        SteamLog.d("vm: approval timer expired challenge=${SteamLog.short(pending.challengeId)}")
        authRunner.cancelRunning("approval_timer_expired")
        _state.value = SteamLoginUiState.Error("Steam Guard approval timed out. Try again or enter the Steam Guard code.")
    }

    fun continueDeviceConfirmation() {
        val pending = _state.value as? SteamLoginUiState.DeviceConfirmationPending ?: return
        if (pending.continuing) {
            SteamLog.d("vm: continue approval ignored, already continuing challenge=${SteamLog.short(pending.challengeId)}")
            return
        }
        SteamLog.d("vm: continue approval requested challenge=${SteamLog.short(pending.challengeId)}")
        _state.value = pending.copy(continuing = true)
        val started = authRunner.continueDeviceConfirmation(pending.challengeId)
        SteamLog.d("vm: continue approval start result=$started")
        if (!started && authRunner.state.value !is SteamAuthRunnerState.Running) {
            val latest = _state.value as? SteamLoginUiState.DeviceConfirmationPending
            if (latest?.challengeId == pending.challengeId) {
                SteamLog.d("vm: Steam Guard continuation did not start, keeping approval prompt retryable")
                _state.value = latest.copy(continuing = false)
            }
        }
    }

    fun cancelAuthFlow() {
        SteamLog.d("vm: auth flow cancelled by user")
        authRunner.cancelRunning("user_cancelled_auth")
        _state.value = SteamLoginUiState.Idle
    }

    fun expireQrSignIn() {
        val pending = _state.value as? SteamLoginUiState.QrCodePending ?: return
        SteamLog.d("vm: QR timer expired flow=${SteamLog.short(pending.flowId)}")
        authRunner.cancelRunning("qr_timer_expired")
        _state.value = SteamLoginUiState.Error("Steam QR sign-in timed out. Generate a new code and try again.")
    }

    fun continueQrSignIn() {
        val pending = _state.value as? SteamLoginUiState.QrCodePending ?: return
        if (pending.checking) {
            SteamLog.d("vm: QR continue ignored, already checking flow=${SteamLog.short(pending.flowId)}")
            return
        }
        SteamLog.d("vm: QR continue requested flow=${SteamLog.short(pending.flowId)}")
        _state.value = pending.copy(checking = true)
        val started = authRunner.continueQrSignIn(pending.flowId)
        SteamLog.d("vm: QR continue start result=$started")
        if (!started && authRunner.state.value !is SteamAuthRunnerState.Running) {
            val latest = _state.value as? SteamLoginUiState.QrCodePending
            if (latest?.flowId == pending.flowId) {
                _state.value = latest.copy(checking = false)
            }
        }
    }

    private fun observeAuthRunner() {
        viewModelScope.launch {
            authRunner.state.collect { auth ->
                when (auth) {
                    SteamAuthRunnerState.Idle -> {
                        val pending = _state.value as? SteamLoginUiState.DeviceConfirmationPending
                        if (pending?.continuing == true) {
                            SteamLog.d("vm: runner idle while approval continuing, making prompt retryable")
                            _state.value = pending.copy(continuing = false)
                        }
                        val qrPending = _state.value as? SteamLoginUiState.QrCodePending
                        if (qrPending?.checking == true) {
                            SteamLog.d("vm: runner idle while QR checking, making prompt retryable")
                            _state.value = qrPending.copy(checking = false)
                        }
                    }
                    is SteamAuthRunnerState.Running -> {
                        SteamLog.d("vm: runner running stage=${auth.stage}")
                        when (auth.stage) {
                            "login_continue" -> {
                                val pending = _state.value as? SteamLoginUiState.DeviceConfirmationPending
                                _state.value = pending?.copy(continuing = true) ?: SteamLoginUiState.Loading
                            }
                            "login_qr" -> {
                                if (_state.value !is SteamLoginUiState.SignedIn) {
                                    _state.value = SteamLoginUiState.QrCodeStarting
                                }
                            }
                            "login_qr_continue" -> {
                                val pending = _state.value as? SteamLoginUiState.QrCodePending
                                _state.value = pending?.copy(checking = true) ?: SteamLoginUiState.QrCodeStarting
                            }
                            else -> if (_state.value !is SteamLoginUiState.SignedIn) {
                                _state.value = SteamLoginUiState.Loading
                            }
                        }
                    }
                    is SteamAuthRunnerState.Finished -> {
                        if (handledAuthResultId == auth.id) return@collect
                        handledAuthResultId = auth.id
                        SteamLog.d("vm: runner finished id=${auth.id} stage=${auth.stage}")
                        if (_state.value is SteamLoginUiState.Idle) {
                            SteamLog.d("vm: ignoring finished auth result after user cancel stage=${auth.stage}")
                            authRunner.resetFinished(auth.id)
                            return@collect
                        }
                        val autoContinue = handleSignInOutcome(auth.outcome, auth.stage)
                        authRunner.resetFinished(auth.id)
                        when (autoContinue) {
                            is AutoContinuation.DeviceApproval -> {
                                SteamLog.d("vm: auto-continuing approval challenge=${SteamLog.short(autoContinue.challengeId)}")
                                continueDeviceConfirmation()
                            }
                            is AutoContinuation.QrCode -> {
                                SteamLog.d("vm: auto-continuing QR flow=${SteamLog.short(autoContinue.flowId)}")
                                continueQrSignIn()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleSignInOutcome(r: SignInOutcome, stage: String): AutoContinuation? {
        when (r) {
            is SignInOutcome.Success -> {
                SteamLog.d("vm: login success uid=${r.uid}")
                appToken = r.appToken
                Telemetry.log(AnalyticsEvent.LoginSuccess(restored = false))
                Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "true")
                _state.value = SteamLoginUiState.SignedIn(BackendState.Loading)
                loadProfile()
                return null
            }
            is SignInOutcome.GuardRequired -> {
                SteamLog.d("vm: guard code required kind=${r.kind}")
                Telemetry.log(AnalyticsEvent.Login2faRequired(r.kind.name.lowercase()))
                _state.value = SteamLoginUiState.GuardRequired(r.kind)
                return null
            }
            is SignInOutcome.DeviceConfirmationPending -> {
                SteamLog.d(
                    "vm: approval pending stage=$stage challenge=${SteamLog.short(r.challengeId)} expiresIn=${r.expiresIn}",
                )
                val pending = SteamLoginUiState.DeviceConfirmationPending(
                    challengeId = r.challengeId,
                    expiresIn = r.expiresIn,
                    continuing = false,
                )
                _state.value = pending
                return r.challengeId.takeIf { stage == "login" }?.let(AutoContinuation::DeviceApproval)
            }
            is SignInOutcome.QrCodePending -> {
                SteamLog.d(
                    "vm: QR pending stage=$stage flow=${SteamLog.short(r.flowId)} " +
                        "code=${r.displayCode.orEmpty()} expiresIn=${r.expiresIn}",
                )
                val pending = SteamLoginUiState.QrCodePending(
                    flowId = r.flowId,
                    challengeUrl = r.challengeUrl,
                    displayCode = r.displayCode,
                    expiresIn = r.expiresIn,
                    checking = false,
                )
                _state.value = pending
                return r.flowId.takeIf { stage == "login_qr" || stage == "login_qr_continue" }
                    ?.let(AutoContinuation::QrCode)
            }
            /*
             * TUNNEL_DISABLED:
             * The old backend/device tunnel flow is no longer reachable from the active
             * kSteam sign-in path. Keep the former UI mapping here commented out so the
             * behavior can be restored deliberately if the tunnel ever comes back.
             *
             * SignInOutcome.TunnelRequired -> {
             *     SteamLog.d("vm: tunnel required stage=$stage")
             *     Telemetry.log(AnalyticsEvent.LoginTunnelRequired)
             *     Telemetry.recordError(TelemetryError("login_tunnel_required"), "stage" to stage)
             *     _state.value = SteamLoginUiState.Error("Couldn't open the device tunnel — try again.")
             *     return null
             * }
             */
            is SignInOutcome.Failure -> {
                SteamLog.d("vm: login failed stage=$stage reason=${r.reason}")
                Telemetry.log(AnalyticsEvent.LoginFailed(loginFailReason(r.reason)))
                Telemetry.recordError(TelemetryError("login_failed: ${loginFailReason(r.reason)}"), "stage" to stage)
                _state.value = SteamLoginUiState.Error(r.reason)
                return null
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
        if (!canStartProfileUpdate()) return
        _refreshing.value = true
        viewModelScope.launch {
            try {
                updateProfileFromBackend()
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun updateProfile() {
        if (!canStartProfileUpdate()) return
        _updatingProfile.value = true
        viewModelScope.launch {
            try {
                updateProfileFromBackend()
            } finally {
                _updatingProfile.value = false
            }
        }
    }

    private fun canStartProfileUpdate(): Boolean =
        appToken != null && !_refreshing.value && !_updatingProfile.value

    private suspend fun updateProfileFromBackend() {
        runCatching { fetchProfileWithReauth() }
            .onSuccess { p ->
                saveCachedProfile(p)
                _state.value = SteamLoginUiState.SignedIn(BackendState.Ok(p, fromCache = false))
            }
            .onFailure { SteamLog.e("vm: profile update failed", it) }
    }

    fun signOut() = exit(ExitAction.SIGNING_OUT, AnalyticsEvent.ProfileSignedOut)

    fun clearMyData() = exit(ExitAction.CLEARING, AnalyticsEvent.ProfileDataCleared)

    private fun exit(action: ExitAction, event: AnalyticsEvent) {
        if (_exiting.value != ExitAction.NONE) return
        _exiting.value = action
        Telemetry.log(event)
        viewModelScope.launch {
            appToken?.let { token ->
                runCatching {
                    when (action) {
                        ExitAction.SIGNING_OUT -> backend.signOut(token)
                        ExitAction.CLEARING -> backend.clearMyData(token)
                        ExitAction.NONE -> Unit
                    }
                }.onFailure {
                    val label = if (action == ExitAction.CLEARING) "clear-my-data" else "sign-out"
                    SteamLog.e("vm: backend $label failed", it)
                }
            }
            Telemetry.setUserId(null)
            Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "false")
            authRunner.cancelRunning("exit_$action")
            signIn.signOut()
            appToken = null
            clearLocalAuthAndProfileData()
            _exiting.value = ExitAction.NONE
            showLoginAfterPreflight()
        }
    }

    private suspend fun forceLoginAfterMissingSession(clearSteamSession: Boolean) {
        SteamLog.d("vm: clearing cached signed-in profile because auth session is missing clearSteamSession=$clearSteamSession")
        Telemetry.setUserId(null)
        Telemetry.userProperty(UserProperties.IS_LOGGED_IN, "false")
        appToken = null
        if (clearSteamSession) signIn.signOut()
        clearLocalAuthAndProfileData()
        showLoginAfterPreflight()
    }

    private suspend fun showLoginAfterPreflight() {
        if (!signIn.requiresAuthEnvironmentCheck) {
            _state.value = SteamLoginUiState.Idle
            Telemetry.log(AnalyticsEvent.LoginFormShown)
            return
        }
        _state.value = SteamLoginUiState.CheckingAuthEnvironment
        applyAuthEnvironment(
            runCatching { signIn.checkAuthEnvironment() }
                .getOrElse { SteamAuthEnvironment.CompanionUnavailable(it.message ?: "Couldn't check JVM minter.") },
        )
    }

    private fun applyAuthEnvironment(environment: SteamAuthEnvironment) {
        when (environment) {
            SteamAuthEnvironment.Ready -> {
                _state.value = SteamLoginUiState.Idle
                Telemetry.log(AnalyticsEvent.LoginFormShown)
            }
            is SteamAuthEnvironment.LocalNetworkPermissionRequired -> {
                _state.value = SteamLoginUiState.LocalNetworkPermissionRequired(environment.denied)
            }
            is SteamAuthEnvironment.CompanionUnavailable -> {
                _state.value = SteamLoginUiState.MinterUnavailable(environment.message)
            }
        }
    }

    private fun clearLocalAuthAndProfileData() {
        SteamLog.d("vm: clearing local auth/profile caches")
        runCatching { LocalCache.remove(PROFILE_CACHE_KEY) }
        runCatching { LocalCache.remove(DEMO_FLAG_KEY) }
        runCatching { LocalCache.removeByPrefix("race_detail_") }
        runCatching { LocalCache.removeByPrefix("race_split_") }
        runCatching { LocalCache.removeByPrefix("track_detail_") }
        runCatching { LocalCache.remove(ACHIEVEMENTS_CACHE_KEY) }
        _allRaces.value = AllRacesUi()
        _categoryRaces.value = CategoryRacesUi()
        _achievements.value = AchievementsUi()
        lastAchievementsRefreshMs = 0L
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

    /** Open a category list: reset + load the first page when switching to a new category,
     *  otherwise keep what's already paginated (back-navigation re-enters the same screen). */
    fun openCategory(category: String) {
        val cur = _categoryRaces.value
        if (cur.category == category && cur.races.isNotEmpty()) return
        _categoryRaces.value = CategoryRacesUi(category = category)
        loadMoreCategory()
    }

    fun loadMoreCategory() {
        val cur = _categoryRaces.value
        val category = cur.category ?: return
        if (cur.loading || !cur.hasMore) return
        viewModelScope.launch {
            _categoryRaces.value = cur.copy(loading = true, error = null)
            runCatching { categoryRacesPage(category, cur.page + 1) }
                .onSuccess { p ->
                    _categoryRaces.value = _categoryRaces.value.copy(
                        races = _categoryRaces.value.races + p.races,
                        total = p.total ?: _categoryRaces.value.total,
                        page = cur.page + 1,
                        hasMore = p.hasMore && p.races.isNotEmpty(),
                        loading = false,
                    )
                }
                .onFailure { e ->
                    _categoryRaces.value = _categoryRaces.value.copy(error = e.message ?: "Couldn't load races", loading = false)
                }
        }
    }

    fun retryCategory() {
        _categoryRaces.value = _categoryRaces.value.copy(error = null)
        loadMoreCategory()
    }

    fun loadAchievements(force: Boolean = false) {
        val cur = _achievements.value
        if (cur.loading || cur.refreshing) return
        if (!force && cur.data != null && cur.error == null) return
        lastAchievementsRefreshMs = Clock.System.now().toEpochMilliseconds()
        _achievements.value = cur.copy(
            loading = cur.data == null,
            refreshing = cur.data != null,
            error = null,
        )
        viewModelScope.launch {
            runCatching { achievementsClient.achievements() }
                .onSuccess { data ->
                    saveCachedAchievements(data)
                    _achievements.value = AchievementsUi(data = data, fromCache = false)
                }
                .onFailure { e ->
                    SteamLog.e("vm: achievements load failed", e)
                    val cached = loadCachedAchievements()
                    _achievements.value = AchievementsUi(
                        data = cached ?: cur.data,
                        fromCache = cached != null || cur.fromCache,
                        error = e.message ?: "Couldn't load Steam achievements",
                    )
                }
        }
    }

    fun refreshAchievementsIfStale() {
        val now = Clock.System.now().toEpochMilliseconds()
        if (lastAchievementsRefreshMs > 0L && now - lastAchievementsRefreshMs < ACHIEVEMENTS_PROFILE_REFRESH_COOLDOWN_MS) return
        loadAchievements(force = true)
    }

    fun refreshAchievements() {
        loadAchievements(force = true)
    }

    private suspend fun categoryRacesPage(category: String, page: Int): RacesPageDto =
        withReauth { backend.categoryRacesPage(it, category, page) }

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
            if (e is ReauthLoginRequired) return
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

    /** Race-detail with offline-first caching: finished races are immutable, so a fresh fetch
     *  always overwrites the cache and reads can fall back to it when offline. */
    suspend fun raceDetail(eventId: String, split: Int?, page: Int?): RaceDetailDto =
        withReauth { backend.raceDetail(it, eventId, split, page) }
            .also { runCatching { LocalCache.write(raceDetailKey(eventId), AppJson.encodeToString(it)) } }

    fun cachedRaceDetail(eventId: String): RaceDetailDto? =
        LocalCache.read(raceDetailKey(eventId))?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AppJson.decodeFromString<RaceDetailDto>(it) }.getOrNull() }

    /** One foreign split's tables (lazy, per tab) — same offline-first caching as [raceDetail]. */
    suspend fun raceSplit(eventId: String, splitNo: Int, seriesId: String?): SplitDetailDto =
        withReauth { backend.raceSplit(it, eventId, splitNo, seriesId) }
            .also { runCatching { LocalCache.write(splitKey(eventId, splitNo), AppJson.encodeToString(it)) } }

    fun cachedSplit(eventId: String, splitNo: Int): SplitDetailDto? =
        LocalCache.read(splitKey(eventId, splitNo))?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AppJson.decodeFromString<SplitDetailDto>(it) }.getOrNull() }

    /** Track detail + personal record. Offline-first: cache the last good payload per track. */
    suspend fun trackDetail(trackId: String, patch: String? = null): TrackDetailResponse =
        withReauth { backend.trackDetail(it, trackId, patch) }
            .also { runCatching { LocalCache.write(trackDetailKey(trackId, patch), AppJson.encodeToString(it)) } }

    fun cachedTrackDetail(trackId: String, patch: String? = null): TrackDetailResponse? =
        LocalCache.read(trackDetailKey(trackId, patch))?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AppJson.decodeFromString<TrackDetailResponse>(it) }.getOrNull() }

    private fun raceDetailKey(eventId: String) = "race_detail_$eventId"
    private fun splitKey(eventId: String, splitNo: Int) = "race_split_${eventId}_$splitNo"
    private fun trackDetailKey(trackId: String, patch: String? = null) =
        "track_detail_${trackId}_${patch?.takeIf { it.isNotBlank() } ?: "all"}"

    private suspend fun <T> withReauth(block: suspend (token: String) -> T): T {
        // Auth may still be restoring at launch (Render cold start). Wait for the token
        // instead of failing instantly with "not signed in" — callers paint a loader
        // meanwhile. Only give up if it genuinely never arrives.
        val token = appToken ?: (if (restoreFinished) null else tokenHolder.await(AUTH_WAIT_MS))
            ?: throw IllegalStateException("Session expired. Sign in again.")
        return try {
            block(token)
        } catch (e: BackendReauthRequired) {
            Telemetry.log(AnalyticsEvent.ProfileReauthTriggered)
            val fresh = try {
                reauthToken()
            } catch (reauthError: SteamAuthEnvironmentUnavailable) {
                SteamLog.e("vm: reauth environment unavailable, forcing full login", reauthError)
                forceLoginAfterMissingSession(clearSteamSession = true)
                throw ReauthLoginRequired(reauthError.message ?: "Session expired. Sign in again.")
            }
            if (fresh == null) {
                SteamLog.d("vm: reauth returned no token, forcing full login")
                forceLoginAfterMissingSession(clearSteamSession = true)
                throw ReauthLoginRequired("Session expired. Sign in again.")
            }
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

    private fun loadCachedAchievements(): SteamAchievements? =
        LocalCache.read(ACHIEVEMENTS_CACHE_KEY)?.takeIf { it.isNotBlank() }
            ?.let { runCatching { AppJson.decodeFromString<SteamAchievements>(it) }.getOrNull() }

    private fun saveCachedAchievements(achievements: SteamAchievements) {
        runCatching { LocalCache.write(ACHIEVEMENTS_CACHE_KEY, AppJson.encodeToString(achievements)) }
    }
}
