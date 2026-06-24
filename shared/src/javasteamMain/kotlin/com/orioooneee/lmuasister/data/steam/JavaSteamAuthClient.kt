package com.orioooneee.lmuasister.data.steam

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.SteamAuthTicket
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.Closeable
import java.security.Security
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Steam authentication on-device via JavaSteam (the SteamKit2 port) — Android + JVM.
 *
 * Each call spins up its own short-lived CM connection, pumps callbacks on a daemon
 * thread, performs the work, then disconnects — there is no long-lived session here.
 * All blocking JavaSteam calls run on [Dispatchers.IO].
 */
internal class JavaSteamAuthClient : SteamAuthClient {

    override suspend fun login(
        username: String,
        password: String,
        guardCode: String?,
        guardData: String?,
    ): SteamLoginResult = withContext(Dispatchers.IO) {
        ensureBouncyCastle()
        val session = SteamSession()
        try {
            session.open()
            session.awaitConnected()

            val details = AuthSessionDetails().apply {
                this.username = username
                this.password = password
                this.persistentSession = true
                this.guardData = guardData
                this.authenticator = GuardAuthenticator(guardCode)
                // platformType defaults to SteamClient → the refresh token can later be
                // reused to log on and generate game session tickets.
            }

            val authSession = session.client.authentication
                .beginAuthSessionViaCredentials(details).get()
            val poll = authSession.pollingWaitForResult()
                .get(STEAM_GUARD_APPROVAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)

            val steamId = session.logOn(poll.accountName, poll.refreshToken)
            SteamLoginResult.Success(
                SteamTokens(
                    steamId = steamId,
                    accountName = poll.accountName,
                    accessToken = poll.accessToken,
                    refreshToken = poll.refreshToken,
                    guardData = poll.newGuardData ?: guardData,
                ),
            )
        } catch (t: Throwable) {
            guardSignalOf(t)?.let { return@withContext SteamLoginResult.GuardRequired(it.kind) }
            SteamLoginResult.Failure(humanError(t))
        } finally {
            session.close()
        }
    }

    override suspend fun renewAccessToken(
        steamId: Long,
        refreshToken: String,
    ): Result<SteamTokens> = withContext(Dispatchers.IO) {
        ensureBouncyCastle()
        val session = SteamSession()
        try {
            session.open()
            session.awaitConnected()
            val res = session.client.authentication
                .generateAccessTokenForApp(SteamID(steamId), refreshToken, false).get()
            Result.success(
                SteamTokens(
                    steamId = steamId,
                    accountName = "",
                    accessToken = res.accessToken,
                    refreshToken = res.refreshToken.ifEmpty { refreshToken },
                ),
            )
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            session.close()
        }
    }

    override suspend fun sessionTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        appId: Int,
    ): Result<SteamSessionTicket> = withContext(Dispatchers.IO) {
        withLoggedOnSession(accountName, refreshToken) { client ->
            val handler = client.getHandler(SteamAuthTicket::class.java)
                ?: error("SteamAuthTicket handler unavailable")
            SteamSessionTicket(appId, handler.getAuthSessionTicket(appId).get().ticket)
        }
    }

    override suspend fun webApiTicket(
        steamId: Long,
        accountName: String,
        refreshToken: String,
        identity: String,
        appId: Int,
    ): Result<SteamSessionTicket> = withContext(Dispatchers.IO) {
        withLoggedOnSession(accountName, refreshToken) { client ->
            val handler = client.getHandler(SteamAuthTicket::class.java)
                ?: error("SteamAuthTicket handler unavailable")
            SteamSessionTicket(appId, handler.getAuthTicketForWebApi(appId, identity).get().ticket)
        }
    }

    private fun withLoggedOnSession(
        accountName: String,
        refreshToken: String,
        block: (SteamClient) -> SteamSessionTicket,
    ): Result<SteamSessionTicket> {
        ensureBouncyCastle()
        val session = SteamSession()
        return try {
            session.open()
            session.awaitConnected()
            session.logOn(accountName, refreshToken)
            Result.success(block(session.client))
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            session.close()
        }
    }

    private fun humanError(t: Throwable): String {
        val root = t.cause ?: t
        if (root is TimeoutException) {
            return "Steam Guard approval timed out. Approve the request in the Steam app and try again."
        }
        return root.message ?: root::class.simpleName ?: "Steam login failed"
    }

    private fun guardSignalOf(t: Throwable): GuardSignal? {
        var e: Throwable? = t
        while (e != null) {
            if (e is GuardSignal) return e
            e = e.cause
        }
        return null
    }

    private companion object {
        private const val STEAM_GUARD_APPROVAL_TIMEOUT_MS = 120_000L

        @Volatile
        private var bcReady = false

        /**
         * JavaSteam asks for the "BC" JCE provider, but Android ships a stripped-down
         * bouncycastle under that name. Swap in the full bcprov so RSA/PKCS1 etc. work.
         * On desktop JVM this is a harmless no-op / reinforcement.
         */
        private fun ensureBouncyCastle() {
            if (bcReady) return
            synchronized(this) {
                if (bcReady) return
                runCatching {
                    if (Security.getProvider("BC") !is BouncyCastleProvider) {
                        Security.removeProvider("BC")
                        Security.insertProviderAt(BouncyCastleProvider(), 1)
                    }
                }
                bcReady = true
            }
        }
    }
}

/** Raised by [GuardAuthenticator] to signal "Steam Guard code needed" up to the caller. */
private class GuardSignal(val kind: SteamGuardKind) : RuntimeException()

/**
 * Feeds a single user-supplied Guard code into JavaSteam's polling. If no (valid)
 * code is available it accepts Steam mobile app confirmation when offered; if Steam
 * only offers a typed code, it fails with [GuardSignal] so the UI can prompt for one.
 */
private class GuardAuthenticator(private val code: String?) : IAuthenticator {
    private var used = false

    override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> =
        supply(SteamGuardKind.DEVICE, previousCodeWasIncorrect)

    override fun getEmailCode(email: String?, previousCodeWasIncorrect: Boolean): CompletableFuture<String> =
        supply(SteamGuardKind.EMAIL, previousCodeWasIncorrect)

    override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(code.isNullOrBlank())

    private fun supply(kind: SteamGuardKind, incorrect: Boolean): CompletableFuture<String> {
        if (code.isNullOrBlank() || used || incorrect) {
            return CompletableFuture.failedFuture(GuardSignal(kind))
        }
        used = true
        return CompletableFuture.completedFuture(code)
    }
}

private class SteamSession {
    val client = SteamClient()
    private val manager = CallbackManager(client)
    private val subs = mutableListOf<Closeable>()

    @Volatile private var running = true
    private val connectedLatch = CountDownLatch(1)
    @Volatile private var connectError: String? = null

    @Volatile private var loggedOnLatch: CountDownLatch? = null
    @Volatile private var logonResult: EResult? = null
    @Volatile private var steamId: Long = 0

    private val pump = Thread {
        while (running) {
            runCatching { manager.runWaitCallbacks(500L) }
        }
    }.apply { isDaemon = true; name = "steam-cb-pump" }

    fun open() {
        subs += manager.subscribe(ConnectedCallback::class.java) { connectedLatch.countDown() }
        subs += manager.subscribe(DisconnectedCallback::class.java) {
            connectError = "Disconnected from Steam"
            connectedLatch.countDown()
            loggedOnLatch?.countDown()
        }
        subs += manager.subscribe(LoggedOnCallback::class.java) { cb: LoggedOnCallback ->
            logonResult = cb.result
            steamId = cb.clientSteamID?.convertToUInt64() ?: 0L
            loggedOnLatch?.countDown()
        }
        pump.start()
        client.connect()
    }

    fun awaitConnected(timeoutSeconds: Long = 30) {
        check(connectedLatch.await(timeoutSeconds, TimeUnit.SECONDS)) { "Timed out connecting to Steam" }
        connectError?.let { error(it) }
    }

    fun logOn(accountName: String, refreshToken: String): Long {
        val latch = CountDownLatch(1).also { loggedOnLatch = it }
        val user = client.getHandler(SteamUser::class.java) ?: error("SteamUser handler unavailable")
        val details = LogOnDetails().apply {
            username = accountName
            accessToken = refreshToken // misnamed in JavaSteam — it takes the refresh token
            shouldRememberPassword = true
            loginID = 149 // non-zero so we don't clash with the user's real Steam client on the same IP
        }
        user.logOn(details)
        check(latch.await(30, TimeUnit.SECONDS)) { "Timed out logging on to Steam" }
        val result = logonResult
        if (result != EResult.OK) error("Steam logon failed: $result")
        return steamId
    }

    fun close() {
        running = false
        subs.forEach { runCatching { it.close() } }
        runCatching { client.disconnect() }
    }
}
