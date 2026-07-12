package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.data.remote.BackendAuthFailed
import com.orioooneee.lmuasister.data.remote.SteamBackendApi

private const val WEBAPI_IDENTITY = ""

/**
 * Android + desktop JVM sign-in: on-device JavaSteam mints a Steam Web API ticket
 * (residential IP, no tunnel needed), which the backend exchanges at /auth/steam for
 * our app token. Steam tokens are persisted in the secure store for silent reauth.
 */
internal class JavaSteamSignIn(
    private val steam: SteamAuthClient,
    private val backend: SteamBackendApi,
    private val store: SteamSessionStore,
) : SteamSignIn {

    override suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome {
        SteamLog.d("javasteam: login start (userLen=${username.trim().length})")
        return when (val r = steam.login(username.trim(), password, guardCode?.takeIf { it.isNotBlank() })) {
            is SteamLoginResult.GuardRequired -> { SteamLog.d("javasteam: Steam Guard required (${r.kind})"); SignInOutcome.GuardRequired(r.kind) }
            is SteamLoginResult.Failure -> { SteamLog.e("javasteam: login failed: ${r.reason}"); SignInOutcome.Failure(r.reason) }
            is SteamLoginResult.Success -> {
                SteamLog.d("javasteam: login ok (steamId=${r.tokens.steamId}), minting ticket + exchange")
                store.save(r.tokens)
                exchange(r.tokens) ?: SignInOutcome.Failure("Steam ticket rejected.")
            }
        }
    }

    override suspend fun restore(): String? = store.load()?.let { silentToken(it) }

    override suspend fun reauth(): String? = store.load()?.let { silentToken(it) }

    override fun signOut() {
        store.clear()
    }

    private suspend fun silentToken(tokens: SteamTokens): String? =
        (exchange(tokens) as? SignInOutcome.Success)?.appToken

    private suspend fun exchange(tokens: SteamTokens): SignInOutcome? {
        val ticket = steam.webApiTicket(tokens.steamId, tokens.accountName, tokens.refreshToken, WEBAPI_IDENTITY)
            .getOrNull() ?: return null
        return try {
            val resp = backend.authSteam(ticket.hex)
            SignInOutcome.Success(resp.token, resp.uid)
        } catch (e: BackendAuthFailed) {
            SignInOutcome.Failure(e.message ?: "auth_failed")
        } catch (e: Throwable) {
            SignInOutcome.Failure(e.message ?: "backend error")
        }
    }
}
