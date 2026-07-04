package com.orioooneee.lmuasister.data.steam

/** Result of a sign-in attempt. The app token is our own backend JWT (for profile calls). */
sealed interface SignInOutcome {
    data class Success(val appToken: String, val uid: String) : SignInOutcome
    data class GuardRequired(val kind: SteamGuardKind) : SignInOutcome
    data class DeviceConfirmationPending(val challengeId: String, val expiresIn: Int) : SignInOutcome
    data class QrCodePending(
        val flowId: String,
        val challengeUrl: String,
        val displayCode: String?,
        val expiresIn: Int,
    ) : SignInOutcome
    data class Failure(val reason: String) : SignInOutcome

    /*
     * TUNNEL_DISABLED:
     * The old device-egress tunnel flow is intentionally kept out of the active sign-in
     * contract. Leave this here as a marker instead of deleting the old path outright.
     *
     * data object TunnelRequired : SignInOutcome
     */
}

sealed interface SteamAuthEnvironment {
    data object Ready : SteamAuthEnvironment

    data class LocalNetworkPermissionRequired(
        val denied: Boolean = false,
    ) : SteamAuthEnvironment

    data class CompanionUnavailable(
        val message: String,
    ) : SteamAuthEnvironment
}

class SteamAuthEnvironmentUnavailable(message: String) : RuntimeException(message)

/**
 * Steam sign-in strategy. The active production implementation is kSteam on-device
 * auth for Android, iOS and desktop: it signs into Steam locally, mints a Steam ticket,
 * and exchanges it at /auth/steam for our app token.
 *
 * It ends with our backend app token, which [SteamBackendApi] uses for profile calls.
 */
interface SteamSignIn {
    val requiresAuthEnvironmentCheck: Boolean
        get() = false

    suspend fun checkAuthEnvironment(): SteamAuthEnvironment =
        SteamAuthEnvironment.Ready

    suspend fun requestAuthEnvironmentAccess(): SteamAuthEnvironment =
        checkAuthEnvironment()

    suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome

    suspend fun continueDeviceConfirmation(challengeId: String): SignInOutcome =
        SignInOutcome.Failure("Steam Guard approval is not available on this platform.")

    suspend fun startQrSignIn(): SignInOutcome =
        SignInOutcome.Failure("Steam QR sign-in is not available on this platform.")

    suspend fun continueQrSignIn(flowId: String): SignInOutcome =
        SignInOutcome.Failure("Steam QR sign-in is not available on this platform.")

    /** Silent session restore (no password) → app token, or null if a full login is needed. */
    suspend fun restore(): String?

    /** Silent reauth after a profile 401 → fresh app token, or null if a full login is needed. */
    suspend fun reauth(): String?

    fun signOut()
}

class SteamRestoreTimedOut : RuntimeException("Steam session restore timed out")
