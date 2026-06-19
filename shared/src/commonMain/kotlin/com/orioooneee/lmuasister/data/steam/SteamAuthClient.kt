package com.orioooneee.lmuasister.data.steam

/**
 * On-device Steam authentication.
 *
 * Backed by JavaSteam on **Android only**. On every other target the bound
 * implementation is [UnsupportedSteamAuthClient], which always fails — there the
 * Steam handshake must run on the backend instead (see project memory: no native
 * Steam library exists for iOS). The architecture intent is: do the login + ticket
 * generation where the user physically is (residential IP) and send only the
 * short-lived session ticket to our backend.
 */
interface SteamAuthClient {

    /**
     * Logs in with Steam credentials.
     *
     * If Steam Guard is required and no (valid) [guardCode] is supplied, returns
     * [SteamLoginResult.GuardRequired] so the UI can prompt for it and call again.
     * Pass [guardData] from a previous [SteamTokens.guardData] to skip Guard on a
     * trusted device.
     *
     * The resulting refresh token is minted for the SteamClient platform, so it can
     * be reused to log on and generate game session tickets.
     */
    suspend fun login(
        username: String,
        password: String,
        guardCode: String? = null,
        guardData: String? = null,
    ): SteamLoginResult

    /** Mints a fresh access token from a stored [refreshToken] (no password needed). */
    suspend fun renewAccessToken(steamId: Long, refreshToken: String): Result<SteamTokens>

    /**
     * Generates a game session ticket for [appId] (ISteamGameServer::BeginAuthSession
     * style). Logs on with [refreshToken] (as [accountName]) first.
     */
    suspend fun sessionTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        appId: Int = LMU_APP_ID,
    ): Result<SteamSessionTicket>

    /**
     * Generates a Web API session ticket for [appId] bound to [identity], to be
     * validated by the game backend via ISteamUserAuth/AuthenticateUserTicket.
     */
    suspend fun webApiTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        identity: String,
        appId: Int = LMU_APP_ID,
    ): Result<SteamSessionTicket>
}

/** Fallback used on platforms without a native Steam library (iOS, web, plain JVM). */
internal class UnsupportedSteamAuthClient(
    private val reason: String = "Steam login is only available on Android in this build.",
) : SteamAuthClient {

    override suspend fun login(
        username: String,
        password: String,
        guardCode: String?,
        guardData: String?,
    ): SteamLoginResult = SteamLoginResult.Failure(reason)

    override suspend fun renewAccessToken(steamId: Long, refreshToken: String): Result<SteamTokens> =
        Result.failure(UnsupportedOperationException(reason))

    override suspend fun sessionTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        appId: Int,
    ): Result<SteamSessionTicket> = Result.failure(UnsupportedOperationException(reason))

    override suspend fun webApiTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        identity: String,
        appId: Int,
    ): Result<SteamSessionTicket> = Result.failure(UnsupportedOperationException(reason))
}
