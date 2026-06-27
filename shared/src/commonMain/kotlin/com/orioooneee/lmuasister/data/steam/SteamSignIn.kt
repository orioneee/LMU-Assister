package com.orioooneee.lmuasister.data.steam

/** Result of a sign-in attempt. The app token is our own backend JWT (for profile calls). */
sealed interface SignInOutcome {
    data class Success(val appToken: String, val uid: String) : SignInOutcome
    data class GuardRequired(val kind: SteamGuardKind) : SignInOutcome
    data class DeviceConfirmationPending(val challengeId: String, val expiresIn: Int) : SignInOutcome
    data class Failure(val reason: String) : SignInOutcome

    /** The device egress tunnel is required / not connected — the UI should retry. */
    data object TunnelRequired : SignInOutcome
}

/**
 * Steam sign-in strategy. The active production implementation is kSteam on-device
 * auth for Android, iOS and desktop: it signs into Steam locally, mints a Steam ticket,
 * and exchanges it at /auth/steam for our app token.
 *
 * It ends with our backend app token, which [SteamBackendApi] uses for profile calls.
 */
interface SteamSignIn {
    suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome

    suspend fun continueDeviceConfirmation(challengeId: String): SignInOutcome =
        SignInOutcome.Failure("Steam Guard approval is not available on this platform.")

    /** Silent session restore (no password) → app token, or null if a full login is needed. */
    suspend fun restore(): String?

    /** Silent reauth after a profile 401 → fresh app token, or null if a full login is needed. */
    suspend fun reauth(): String?

    fun signOut()
}

class SteamRestoreTimedOut : RuntimeException("Steam session restore timed out")
