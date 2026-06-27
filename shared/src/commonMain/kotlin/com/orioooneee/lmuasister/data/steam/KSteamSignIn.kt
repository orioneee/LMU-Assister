package com.orioooneee.lmuasister.data.steam

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.account.AuthorizationState
import com.orioooneee.lmuasister.data.remote.BackendAuthFailed
import com.orioooneee.lmuasister.data.remote.SteamAuthResponse
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

private const val APPROVAL_WAIT_SECONDS = 120
private const val LOGIN_WAIT_MS = 45_000L
private const val RESTORE_WAIT_MS = 10_000L
private const val APPROVAL_WAIT_MS = APPROVAL_WAIT_SECONDS * 1_000L
private const val PROMPT_STATE_WAIT_MS = 2_000L

/**
 * Steam auth through kSteam. The kSteam client is process-scoped and independent of
 * any screen lifecycle; long waits for Steam Guard approval happen in SteamAuthRunner.
 */
internal class KSteamSignIn(
    private val backend: SteamBackendApi,
    private val sessionStore: SteamSessionStore,
    private val clientFactory: () -> SteamClient = ::createKSteamClient,
) : SteamSignIn {

    private val mutex = Mutex()
    private var client: SteamClient? = null
    private var started = false

    override suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome =
        mutex.withLock {
            val steam = startedClient()
            val code = guardCode?.trim().orEmpty()

            if (code.isNotBlank() && steam.account.clientAuthState.value is AuthorizationState.AwaitingTwoFactor) {
                SteamLog.d("ksteam: login has typed guard code while auth state=${steam.account.clientAuthState.value.describe()}")
                return@withLock submitGuardCode(steam, code)
            }

            if (steam.account.clientAuthState.value is AuthorizationState.AwaitingTwoFactor) {
                SteamLog.d("ksteam: cancelling stale 2FA session before new credential login")
                steam.account.cancelSignInAttempt()
            }

            SteamLog.d("ksteam: credential login start userLen=${username.trim().length} state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}")
            val authResult = try {
                steam.account.signIn(username.trim(), password, rememberSession = true)
            } catch (e: CancellationException) {
                SteamLog.e("ksteam: credential login interrupted state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}", e)
                resetClientAfterCredentialFailure(steam, "interrupted")
                return@withLock SignInOutcome.Failure("Steam connection was interrupted. Try again.")
            }
            when (authResult) {
                Account.AuthorizationResult.InvalidPassword -> {
                    SteamLog.d("ksteam: credential login rejected: invalid password")
                    resetClientAfterCredentialFailure(steam, "invalid_password")
                    return@withLock SignInOutcome.Failure("Steam rejected the password.")
                }
                Account.AuthorizationResult.RpcError -> {
                    SteamLog.d("ksteam: credential login rejected: rpc error")
                    resetClientAfterCredentialFailure(steam, "rpc_error")
                    return@withLock SignInOutcome.Failure("Steam auth request failed. Try again.")
                }
                Account.AuthorizationResult.ProceedToTfa -> Unit
            }

            val awaiting = awaitGuardPrompt(steam) ?: return@withLock finishSignedIn(steam)
            val hasApproval = awaiting.supportedConfirmationMethods.any { it.isApprovalBased() }
            val codeKind = awaiting.supportedConfirmationMethods.firstNotNullOfOrNull { it.codeKindOrNull() }

            when {
                hasApproval -> {
                    SteamLog.d(
                        "ksteam: approval-based Steam Guard pending " +
                            "challenge=${SteamLog.short(awaiting.steamId.toString())} methods=${awaiting.supportedConfirmationMethods}",
                    )
                    SignInOutcome.DeviceConfirmationPending(
                        challengeId = awaiting.steamId.toString(),
                        expiresIn = APPROVAL_WAIT_SECONDS,
                    )
                }
                codeKind != null -> {
                    SteamLog.d("ksteam: typed Steam Guard code required ($codeKind)")
                    SignInOutcome.GuardRequired(codeKind)
                }
                else -> {
                    SteamLog.d("ksteam: unknown guard prompt, waiting for Steam to finish")
                    waitForSignedInAndExchange(steam, LOGIN_WAIT_MS)
                }
            }
        }

    override suspend fun continueDeviceConfirmation(challengeId: String): SignInOutcome =
        mutex.withLock {
            val steam = startedClient()
            val state = steam.account.clientAuthState.value
            SteamLog.d("ksteam: continue approval challenge=${SteamLog.short(challengeId)} state=${state.describe()}")
            if (state is AuthorizationState.AwaitingTwoFactor && state.steamId.toString() != challengeId) {
                SteamLog.d(
                    "ksteam: approval challenge mismatch expected=${SteamLog.short(challengeId)} " +
                        "actual=${SteamLog.short(state.steamId.toString())}",
                )
                return@withLock SignInOutcome.Failure("Steam Guard request expired. Sign in again.")
            }

            SteamLog.d("ksteam: waiting for Steam Guard approval for up to ${APPROVAL_WAIT_MS / 1000}s")
            val result = withTimeoutOrNull(APPROVAL_WAIT_MS) {
                finishSignedIn(steam)
            }
            result ?: run {
                SteamLog.d("ksteam: approval wait timed out state=${steam.account.clientAuthState.value.describe()}")
                SignInOutcome.Failure("Steam Guard approval timed out. Try signing in again or enter the Steam Guard code.")
            }
        }

    override suspend fun restore(): String? =
        mutex.withLock {
            val steam = startedClient()
            val saved = sessionStore.load()
            val secureIds = secureAccountIds(steam)
            SteamLog.d(
                "ksteam: restore begin stored=${saved.summary()} secureIds=${secureIds.summary()} " +
                    "state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}",
            )
            val hasKsteamSavedData = steam.account.hasSavedDataForAtLeastOneAccount()
            val attemptedSavedRestore = saved != null || hasKsteamSavedData
            SteamLog.d("ksteam: restore sources appStore=${saved != null} ksteamStore=$hasKsteamSavedData")

            val restoreStarted = try {
                withTimeout(RESTORE_WAIT_MS) { tryRestoreStoredSession(steam, saved) }
            } catch (e: TimeoutCancellationException) {
                val success = steam.account.clientAuthState.value is AuthorizationState.Success
                SteamLog.d(
                    "ksteam: refresh-token restore timed out after ${RESTORE_WAIT_MS}ms " +
                        "state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value} success=$success",
                )
                if (!success) {
                    resetClientAfterCredentialFailure(steam, "restore_timeout")
                    throw SteamRestoreTimedOut()
                }
                true
            }

            if (!restoreStarted && !hasKsteamSavedData && steam.account.clientAuthState.value !is AuthorizationState.Success) {
                SteamLog.d("ksteam: restore unavailable; clearing stored session")
                clearStoredSession(steam)
                return@withLock null
            }

            SteamLog.d("ksteam: restore waiting for signed-in state up to ${RESTORE_WAIT_MS}ms")
            val restored = runCatching { waitForSignedInAndExchange(steam, RESTORE_WAIT_MS) }
                .onFailure { SteamLog.e("ksteam: restore failed", it) }
                .getOrNull()
                ?.let { (it as? SignInOutcome.Success)?.appToken }
            SteamLog.d(
                "ksteam: restore wait completed success=${restored != null} " +
                    "state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}",
            )
            if (restored == null && attemptedSavedRestore) {
                SteamLog.d("ksteam: restore did not mint app token; keeping stored refresh token")
                throw SteamRestoreTimedOut()
            }
            restored
        }

    override suspend fun reauth(): String? =
        mutex.withLock {
            val steam = startedClient()
            runCatching { exchangeTicket(steam) }
                .onFailure { SteamLog.e("ksteam: reauth failed", it) }
                .getOrNull()
                ?.token
        }

    override fun signOut() {
        runCatching {
            client?.let { steam ->
                steam.configuration.getValidSecureAccountIds().forEach(steam.configuration::deleteSecureAccount)
                steam.account.cancelSignInAttempt()
                steam.stop()
            }
            client = null
            started = false
        }
        sessionStore.clear()
    }

    private fun clearStoredSession(steam: SteamClient) {
        runCatching { steam.account.cancelSignInAttempt() }
        runCatching { steam.configuration.getValidSecureAccountIds().forEach(steam.configuration::deleteSecureAccount) }
        sessionStore.clear()
    }

    private suspend fun startedClient(): SteamClient {
        val steam = client ?: clientFactory().also { client = it }
        if (!started) {
            SteamLog.d("ksteam: starting client root=persistent-app-store state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}")
            val mark = TimeSource.Monotonic.markNow()
            steam.start()
            started = true
            SteamLog.d("ksteam: client started in ${mark.elapsedNow().inWholeMilliseconds}ms state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}")
        }
        return steam
    }

    private fun resetClientAfterCredentialFailure(steam: SteamClient, reason: String) {
        SteamLog.d("ksteam: resetting client after credential failure reason=$reason cm=${steam.connectionStatus.value}")
        runCatching { steam.account.cancelSignInAttempt() }
        runCatching { steam.stop() }
            .onFailure { SteamLog.e("ksteam: failed to stop client after credential failure", it) }
        if (client === steam) {
            client = null
            started = false
        }
    }

    private suspend fun awaitGuardPrompt(steam: SteamClient): AuthorizationState.AwaitingTwoFactor? =
        (steam.account.clientAuthState.value as? AuthorizationState.AwaitingTwoFactor)
            ?: withTimeoutOrNull(PROMPT_STATE_WAIT_MS) {
                steam.account.clientAuthState.filterIsInstance<AuthorizationState.AwaitingTwoFactor>().first()
            }

    private suspend fun submitGuardCode(steam: SteamClient, code: String): SignInOutcome {
        SteamLog.d("ksteam: submitting typed Steam Guard code")
        val accepted = runCatching { steam.account.updateCurrentSessionWithCode(code) }
            .onFailure { SteamLog.e("ksteam: Steam Guard code submit failed", it) }
            .getOrDefault(false)
        if (!accepted) {
            val kind = (steam.account.clientAuthState.value as? AuthorizationState.AwaitingTwoFactor)
                ?.supportedConfirmationMethods
                ?.firstNotNullOfOrNull { it.codeKindOrNull() }
                ?: SteamGuardKind.DEVICE
            return SignInOutcome.GuardRequired(kind)
        }
        return finishSignedIn(steam)
    }

    private suspend fun finishSignedIn(steam: SteamClient): SignInOutcome {
        val mark = TimeSource.Monotonic.markNow()
        awaitSignedInWithLogs(steam)
        val outcome = exchangeSignedIn(steam)
        SteamLog.d("ksteam: finishSignedIn completed in ${mark.elapsedNow().inWholeMilliseconds}ms")
        return outcome
    }

    private suspend fun exchangeSignedIn(steam: SteamClient): SignInOutcome {
        SteamLog.d("ksteam: Steam sign-in success observed, requesting app ticket")
        val auth = exchangeTicket(steam)
        SteamLog.d("ksteam: backend token received uid=${auth.uid}")
        saveCurrentSession(steam)
        return SignInOutcome.Success(auth.token, auth.uid)
    }

    private suspend fun awaitSignedInWithLogs(steam: SteamClient) {
        var lastState = steam.account.clientAuthState.value.describe()
        SteamLog.d("ksteam: awaitSignIn initial state=$lastState")
        steam.account.clientAuthState
            .onEach { state ->
                val next = state.describe()
                if (next != lastState) {
                    SteamLog.d("ksteam: auth state $lastState -> $next")
                    lastState = next
                }
            }
            .filterIsInstance<AuthorizationState.Success>()
            .first()
        SteamLog.d("ksteam: awaitSignIn completed")
    }

    private suspend fun waitForSignedInAndExchange(steam: SteamClient, timeoutMs: Long): SignInOutcome =
        try {
            val mark = TimeSource.Monotonic.markNow()
            withTimeout(timeoutMs) { finishSignedIn(steam) }
                .also { SteamLog.d("ksteam: waitForSignedInAndExchange success in ${mark.elapsedNow().inWholeMilliseconds}ms") }
        } catch (_: TimeoutCancellationException) {
            SteamLog.d("ksteam: waitForSignedInAndExchange timed out after ${timeoutMs}ms state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}")
            SignInOutcome.Failure("Steam sign-in timed out. Try again.")
        }

    private suspend fun exchangeTicket(steam: SteamClient): SteamAuthResponse {
        SteamLog.d("ksteam: requesting Steam auth session ticket app=$LMU_APP_ID")
        val ticketMark = TimeSource.Monotonic.markNow()
        val ticket = withTimeout(LOGIN_WAIT_MS) {
            steam.authTickets.getAuthSessionTicket(AppId(LMU_APP_ID))
        }
        SteamLog.d("ksteam: Steam auth session ticket received bytes=${ticket.ticket.size} in ${ticketMark.elapsedNow().inWholeMilliseconds}ms")
        return try {
            val exchangeMark = TimeSource.Monotonic.markNow()
            SteamLog.d("ksteam: exchanging Steam ticket with backend")
            backend.authSteam(ticket.ticket.hex()).also {
                SteamLog.d("ksteam: backend ticket exchange ok uid=${it.uid} in ${exchangeMark.elapsedNow().inWholeMilliseconds}ms")
            }
        } catch (e: BackendAuthFailed) {
            SteamLog.e("ksteam: backend rejected Steam ticket", e)
            throw e
        } catch (t: Throwable) {
            SteamLog.e("ksteam: backend ticket exchange failed", t)
            throw IllegalStateException(t.message ?: "Steam ticket exchange failed", t)
        }
    }

    private suspend fun tryRestoreStoredSession(steam: SteamClient, saved: SteamTokens?): Boolean {
        if (saved == null) {
            SteamLog.d("ksteam: no app-stored Steam refresh token")
            return false
        }
        if (saved.steamId == 0L || saved.refreshToken.isBlank()) {
            SteamLog.d("ksteam: app-stored Steam refresh token invalid ${saved.summary()}")
            return false
        }
        SteamLog.d("ksteam: trying stored Steam refresh token restore ${saved.summary()}")
        val mark = TimeSource.Monotonic.markNow()
        return try {
            steam.account.signInWithRefreshToken(
                steamId = SteamId(saved.steamId.toULong()),
                refreshToken = saved.refreshToken,
            )
            SteamLog.d(
                "ksteam: signInWithRefreshToken returned in ${mark.elapsedNow().inWholeMilliseconds}ms " +
                    "state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}",
            )
            true
        } catch (e: CancellationException) {
            val success = steam.account.clientAuthState.value is AuthorizationState.Success
            SteamLog.e(
                "ksteam: stored session restore cancelled after ${mark.elapsedNow().inWholeMilliseconds}ms " +
                    "state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value} success=$success",
                e,
            )
            if (success) true else throw e
        } catch (t: Throwable) {
            SteamLog.e(
                "ksteam: stored session restore failed after ${mark.elapsedNow().inWholeMilliseconds}ms " +
                    "state=${steam.account.clientAuthState.value.describe()} cm=${steam.connectionStatus.value}",
                t,
            )
            false
        }
    }

    private fun saveCurrentSession(steam: SteamClient) {
        runCatching {
            val current = steam.account.getCurrentAccount()
            if (current == null) {
                SteamLog.d("ksteam: cannot store Steam refresh token: current account is null secureIds=${secureAccountIds(steam).summary()}")
                return
            }
            val steamId = steam.configuration.getValidSecureAccountIds()
                .firstOrNull()
                ?.toString()
                ?.toULongOrNull()
            if (steamId == null) {
                SteamLog.d("ksteam: cannot store Steam refresh token: steamId is missing secureIds=${secureAccountIds(steam).summary()}")
                return
            }
            if (current.refreshToken.isBlank()) {
                SteamLog.d("ksteam: cannot store Steam refresh token: refresh token is blank account=${current.accountName.isNotBlank()} accessLen=${current.accessToken.length}")
                return
            }
            sessionStore.save(
                SteamTokens(
                    steamId = steamId.toLong(),
                    accountName = current.accountName,
                    accessToken = current.accessToken,
                    refreshToken = current.refreshToken,
                ),
            )
            SteamLog.d(
                "ksteam: stored Steam refresh token uid=${SteamLog.short(steamId.toString())} " +
                    "accountSet=${current.accountName.isNotBlank()} accessLen=${current.accessToken.length} refreshLen=${current.refreshToken.length}",
            )
        }.onFailure {
            SteamLog.e("ksteam: failed to persist Steam refresh token", it)
        }
    }

    private fun secureAccountIds(steam: SteamClient): List<String> =
        runCatching { steam.configuration.getValidSecureAccountIds().map { it.toString() } }
            .getOrDefault(emptyList())

    private fun List<String>.summary(): String =
        if (isEmpty()) "none" else joinToString(prefix = "[", postfix = "]") { SteamLog.short(it) }

    private fun SteamTokens?.summary(): String =
        this?.let {
            "uid=${SteamLog.short(it.steamId.toString())} accountSet=${it.accountName.isNotBlank()} " +
                "accessLen=${it.accessToken.length} refreshLen=${it.refreshToken.length} guard=${it.guardData != null}"
        } ?: "none"

    private fun AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.isApprovalBased(): Boolean =
        this == AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.DeviceConfirmation ||
            this == AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.EmailConfirmation

    private fun AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.codeKindOrNull(): SteamGuardKind? =
        when (this) {
            AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.EmailCode -> SteamGuardKind.EMAIL
            AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.DeviceCode -> SteamGuardKind.DEVICE
            else -> null
        }

    private fun AuthorizationState.describe(): String =
        when (this) {
            AuthorizationState.Unauthorized -> "Unauthorized"
            AuthorizationState.Success -> "Success"
            is AuthorizationState.AwaitingTwoFactor ->
                "AwaitingTwoFactor(steamId=${SteamLog.short(steamId.toString())}, methods=$supportedConfirmationMethods)"
        }
}
