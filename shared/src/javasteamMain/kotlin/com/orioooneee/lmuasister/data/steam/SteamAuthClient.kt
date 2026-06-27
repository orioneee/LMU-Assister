package com.orioooneee.lmuasister.data.steam

/**
 * On-device Steam authentication, backed by JavaSteam (Android + desktop JVM).
 *
 * The architecture intent: do the login + ticket generation where the user physically
 * is (residential IP) and send only the short-lived session ticket to our backend.
 *
 * TUNNEL_DISABLED:
 * The previous iOS backend + device tunnel note is intentionally retired.
 */
interface SteamAuthClient {

    /**
     * Logs in with Steam credentials. If Steam Guard is required and no (valid) [guardCode]
     * is supplied, returns [SteamLoginResult.GuardRequired] so the UI can prompt and retry.
     * Pass [guardData] from a previous [SteamTokens.guardData] to skip Guard on a trusted device.
     */
    suspend fun login(
        username: String,
        password: String,
        guardCode: String? = null,
        guardData: String? = null,
    ): SteamLoginResult

    /** Mints a fresh access token from a stored [refreshToken] (no password needed). */
    suspend fun renewAccessToken(steamId: Long, refreshToken: String): Result<SteamTokens>

    /** Generates a game session ticket for [appId]; logs on with [refreshToken] first. */
    suspend fun sessionTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        appId: Int = LMU_APP_ID,
    ): Result<SteamSessionTicket>

    /**
     * Generates a Web API session ticket for [appId] bound to [identity], validated by the
     * game backend via ISteamUserAuth/AuthenticateUserTicket.
     */
    suspend fun webApiTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        identity: String,
        appId: Int = LMU_APP_ID,
    ): Result<SteamSessionTicket>
}
