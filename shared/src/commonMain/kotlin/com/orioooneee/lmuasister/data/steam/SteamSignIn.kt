package com.orioooneee.lmuasister.data.steam

/** Result of a sign-in attempt. The app token is our own backend JWT (for profile calls). */
sealed interface SignInOutcome {
    data class Success(val appToken: String, val uid: String) : SignInOutcome
    data class GuardRequired(val kind: SteamGuardKind) : SignInOutcome
    data class Failure(val reason: String) : SignInOutcome

    /** The device egress tunnel is required / not connected — the UI should retry. */
    data object TunnelRequired : SignInOutcome
}

/**
 * Platform sign-in strategy. Two implementations:
 *  - **JavaSteamSignIn** (Android + JVM): on-device JavaSteam mints a Steam Web API ticket,
 *    exchanged at /auth/steam for our app token (residential IP, no tunnel).
 *  - **TunnelSteamSignIn** (iOS): credentials go to the backend sidecar, whose Steam login
 *    egresses through the device via a SOCKS-over-WebSocket tunnel (no native Steam lib on iOS).
 *
 * Both end with our backend app token, which [SteamBackendApi] uses for profile calls.
 */
interface SteamSignIn {
    suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome

    /** Silent session restore (no password) → app token, or null if a full login is needed. */
    suspend fun restore(): String?

    /** Silent reauth after a profile 401 → fresh app token, or null if a full login is needed. */
    suspend fun reauth(): String?

    fun signOut()
}
