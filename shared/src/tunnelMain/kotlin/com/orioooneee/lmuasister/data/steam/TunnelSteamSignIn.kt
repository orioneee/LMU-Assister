package com.orioooneee.lmuasister.data.steam

/*
 * TUNNEL_DISABLED:
 * This entire old backend-side Steam login implementation is intentionally commented out.
 * It is kept in place for reference, but no active platform module should bind or compile it.
 *
import com.orioooneee.lmuasister.data.remote.BackendAuthFailed
import com.orioooneee.lmuasister.data.remote.BackendTunnelRequired
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import com.orioooneee.lmuasister.data.remote.SteamChallengeExpired
import com.orioooneee.lmuasister.data.remote.SteamCreds
import com.orioooneee.lmuasister.data.remote.SteamGuardNeeded
import com.orioooneee.lmuasister.data.remote.UnsupportedGuardFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

/**
 * JVM + iOS sign-in: credentials go to the backend sidecar, whose Steam login egresses
 * through this device via [SteamTunnel] (residential IP). Returns our backend app token.
 *
 * The device-held Steam refresh ({account, refresh, guard}) is persisted in the secure
 * [SteamSessionStore] (never on the backend) and used for silent reauth.
 */
internal class TunnelSteamSignIn(
    private val backend: SteamBackendApi,
    private val store: SteamSessionStore,
) : SteamSignIn {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private companion object {
        const val LOGIN_RETRIES = 5 // retries of /login on the SAME open WS (~7.5s window)
    }

    override suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome {
        SteamLog.d("signIn: start (user=$username, guard=${guardCode?.let { "yes" } ?: "no"})")
        // The tunnel agent may not be registered yet on the sidecar (cold start / race),
        // so retry once on a tunnel-not-connected result with a fresh tunnel.
        var last: SignInOutcome = SignInOutcome.TunnelRequired
        repeat(2) { attempt ->
            SteamLog.d("signIn: attempt ${attempt + 1}/2")
            last = attemptSignIn(username, password, guardCode)
            SteamLog.d("signIn: attempt ${attempt + 1} → ${last::class.simpleName}")
            if (last !is SignInOutcome.TunnelRequired) return last
            delay(1200)
        }
        return last
    }

    private suspend fun attemptSignIn(username: String, password: String, guardCode: String?): SignInOutcome {
        val guardData = loadCreds()?.guard
        return withTunnel { key -> loginWithRetry(username, password, guardCode, guardData, key) }
            ?: SignInOutcome.Failure("Couldn't reach the backend tunnel.")
    }

    /**
     * Calls /auth/steam/login while the tunnel WS stays open. If the sidecar reports the
     * tunnel not connected (agent not yet visible — registration lag), retry on the SAME
     * open WS a few times before giving up. (Reopening the WS would only re-race.)
     */
    private suspend fun loginWithRetry(
        username: String,
        password: String,
        guardCode: String?,
        guardData: String?,
        key: String,
    ): SignInOutcome {
        repeat(LOGIN_RETRIES) { i ->
            val hasGuardCode = !guardCode.isNullOrBlank()
            val endpoint = if (hasGuardCode) "/auth/steam/login" else "/auth/steam/login/start"
            SteamLog.d("login: POST $endpoint (try ${i + 1}/$LOGIN_RETRIES, tunnelKey=${SteamLog.short(key)})")
            try {
                if (hasGuardCode) {
                    val resp = backend.authSteamLogin(
                        username.trim(),
                        password,
                        guardCode,
                        guardData?.takeIf { it.isNotBlank() },
                        key,
                    )
                    return success(resp.token, resp.uid, resp.steam)
                }

                val resp = backend.authSteamLoginStart(
                    username.trim(),
                    password,
                    guardData?.takeIf { it.isNotBlank() },
                    key,
                )
                if (resp.status == "pending_device_confirmation" && !resp.challengeId.isNullOrBlank()) {
                    SteamLog.d("login: pending device confirmation challenge=${SteamLog.short(resp.challengeId)}")
                    return SignInOutcome.DeviceConfirmationPending(resp.challengeId, resp.expiresIn)
                }
                val token = resp.token
                val uid = resp.uid
                if (token != null && uid != null) return success(token, uid, resp.steam)
                SteamLog.e(
                    "login: unexpected start response " +
                        "status=${resp.status}, challenge=${resp.challengeId != null}, token=${resp.token != null}, uid=${resp.uid != null}",
                )
                return SignInOutcome.Failure("Unexpected Steam login response.")
            } catch (e: SteamGuardNeeded) {
                SteamLog.d("login: Steam Guard required (kind=${e.kind}, mode=${e.mode})")
                return SignInOutcome.GuardRequired(if (e.kind.equals("email", true)) SteamGuardKind.EMAIL else SteamGuardKind.DEVICE)
            } catch (e: BackendTunnelRequired) {
                SteamLog.e("login: tunnel not visible yet (try ${i + 1}), WS stays open, retrying…")
                delay(1500)
            } catch (e: BackendAuthFailed) {
                SteamLog.e("login: auth_failed", e)
                return SignInOutcome.Failure(e.message ?: "auth_failed")
            } catch (e: UnsupportedGuardFlow) {
                SteamLog.e("login: unsupported Guard flow", e)
                return SignInOutcome.Failure(e.message ?: "Steam Guard approval is not supported on this device yet.")
            } catch (e: Throwable) {
                SteamLog.e("login: unexpected error", e)
                return SignInOutcome.Failure(e.message ?: e::class.simpleName ?: "sign-in failed")
            }
        }
        return SignInOutcome.TunnelRequired
    }

    override suspend fun continueDeviceConfirmation(challengeId: String): SignInOutcome {
        SteamLog.d("login: continue device confirmation challenge=${SteamLog.short(challengeId)}")
        var last: SignInOutcome = SignInOutcome.TunnelRequired
        repeat(2) { attempt ->
            SteamLog.d("login: continue attempt ${attempt + 1}/2")
            last = attemptContinueDeviceConfirmation(challengeId)
            SteamLog.d("login: continue attempt ${attempt + 1} → ${last::class.simpleName}")
            if (last !is SignInOutcome.TunnelRequired) return last
            delay(1200)
        }
        return last
    }

    private suspend fun attemptContinueDeviceConfirmation(challengeId: String): SignInOutcome =
        withTunnel { key -> continueWithRetry(challengeId, key) }
            ?: SignInOutcome.Failure("Couldn't reach the backend tunnel.")

    private suspend fun continueWithRetry(challengeId: String, key: String): SignInOutcome {
        repeat(LOGIN_RETRIES) { i ->
            SteamLog.d("login: POST /auth/steam/login/continue (try ${i + 1}/$LOGIN_RETRIES, tunnelKey=${SteamLog.short(key)})")
            try {
                val resp = backend.authSteamLoginContinue(challengeId, key)
                return success(resp.token, resp.uid, resp.steam)
            } catch (e: BackendTunnelRequired) {
                SteamLog.e("login: tunnel not visible yet (continue try ${i + 1}), WS stays open, retrying…")
                delay(1500)
            } catch (e: SteamChallengeExpired) {
                SteamLog.e("login: challenge expired", e)
                return SignInOutcome.Failure(e.message ?: "Steam Guard approval expired. Try again.")
            } catch (e: SteamGuardNeeded) {
                SteamLog.d("login: Steam Guard required during continue (kind=${e.kind}, mode=${e.mode})")
                return SignInOutcome.GuardRequired(if (e.kind.equals("email", true)) SteamGuardKind.EMAIL else SteamGuardKind.DEVICE)
            } catch (e: BackendAuthFailed) {
                SteamLog.e("login: continue auth_failed", e)
                return SignInOutcome.Failure(e.message ?: "auth_failed")
            } catch (e: Throwable) {
                SteamLog.e("login: continue unexpected error", e)
                return SignInOutcome.Failure(e.message ?: e::class.simpleName ?: "sign-in failed")
            }
        }
        return SignInOutcome.TunnelRequired
    }

    private fun success(token: String, uid: String, steam: SteamCreds?): SignInOutcome {
        SteamLog.d("login: success uid=$uid, steamCreds=${steam != null}")
        steam?.let { saveCreds(it) }
        return SignInOutcome.Success(token, uid)
    }

    override suspend fun restore(): String? = loadCreds()?.let { reauthWith(it) }

    override suspend fun reauth(): String? = loadCreds()?.let { reauthWith(it) }

    override fun signOut() {
        store.clear()
    }

    private suspend fun reauthWith(creds: SteamCreds): String? {
        val account = creds.account ?: return null
        val refresh = creds.refresh ?: return null
        SteamLog.d("reauth: silent re-mint for account=$account")
        return withTunnel { key ->
            try {
                val resp = backend.authSteamRefresh(account, refresh, key)
                SteamLog.d("reauth: success uid=${resp.uid}")
                resp.steam?.let { saveCreds(it) } // Steam may rotate the refresh
                resp.token
            } catch (e: BackendAuthFailed) {
                SteamLog.e("reauth: refresh dead — clearing stored creds, full login needed", e)
                store.clear()
                null
            } catch (e: Throwable) {
                SteamLog.e("reauth: failed", e)
                null
            }
        }
    }

    private suspend fun <T> withTunnel(block: suspend (key: String) -> T): T? {
        SteamLog.d("tunnel: GET /tunnel/ticket")
        val ticket = runCatching { backend.tunnelTicket() }
            .onFailure { SteamLog.e("tunnel: /tunnel/ticket failed", it) }
            .getOrNull() ?: return null
        if (ticket.agentUrl.isBlank()) {
            SteamLog.e("tunnel: backend returned empty agent_url (STEAM_AGENT_URL not configured?)")
            return null
        }
        SteamLog.d("tunnel: ticket ok (key=${SteamLog.short(ticket.key)}, agent=${ticket.agentUrl})")
        val tunnel = SteamTunnel(ticket.agentUrl, ticket.token, scope)
        return try {
            tunnel.connect()
            delay(400) // let the sidecar register the agent under this key before /login
            block(ticket.key)
        } catch (t: Throwable) {
            SteamLog.e("tunnel: withTunnel error", t)
            null
        } finally {
            tunnel.close()
        }
    }

    // Steam creds packed into the existing secure SteamSessionStore (device-only).
    private fun saveCreds(c: SteamCreds) {
        store.save(
            SteamTokens(
                steamId = 0L,
                accountName = c.account ?: "",
                accessToken = "",
                refreshToken = c.refresh ?: "",
                guardData = c.guard,
            ),
        )
    }

    private fun loadCreds(): SteamCreds? = store.load()?.let {
        if (it.accountName.isBlank() || it.refreshToken.isBlank()) null
        else SteamCreds(account = it.accountName, refresh = it.refreshToken, guard = it.guardData)
    }
}
*/
