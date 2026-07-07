package com.orioooneee.lmuasister.minter

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.CAuthentication_AllowedConfirmation
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.EAuthSessionGuardType
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSession
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.CredentialsAuthSession
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.SteamAuthTicket
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.Closeable
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import `in`.dragonbra.javasteam.types.SteamID

private const val DEFAULT_APP_ID = 2_399_420
private const val DEFAULT_PORT = 8787
private const val LOGIN_WAIT_MS = 45_000L
private const val POLL_REQUEST_WAIT_MS = 15_000L
private const val STATUS_POLL_REQUEST_WAIT_MS = 2_000L
private const val APPROVAL_WAIT_SECONDS = 120
private const val FLOW_TOKEN_BYTES = 32
private const val STEAM_WEB_API_BASE = "https://api.steampowered.com"
private const val STEAM_ACHIEVEMENT_ICON_BASE = "https://cdn.cloudflare.steamstatic.com/steamcommunity/public/images/apps"

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private val secureRandom = SecureRandom()

fun main(args: Array<String>) {
    ensureBouncyCastleProvider()
    val port = args.firstOrNull()?.toIntOrNull()
        ?: System.getenv("PORT")?.toIntOrNull()
        ?: DEFAULT_PORT
    val service = TicketMinterService()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)

    server.createContext("/") { it.redirect("/ui/") }
    server.createContext("/ui") { it.redirect("/ui/") }
    server.createContext("/ui/") { exchange ->
        exchange.respondHtml(uiHtml(port))
    }
    server.createContext("/health") { exchange ->
        exchange.respondJson(healthResponse())
    }
    server.createContext("/api/health") { exchange ->
        exchange.respondJson(healthResponse())
    }
    server.createContext("/api/login") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<LoginRequest>()
            service.login(req).respond(exchange)
        }
    }
    server.createContext("/api/guard") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<GuardRequest>()
            service.submitGuard(req).respond(exchange)
        }
    }
    server.createContext("/api/approval") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<ApprovalRequest>()
            service.waitForApproval(req).respond(exchange)
        }
    }
    server.createContext("/api/qr/start") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<QrStartRequest>()
            service.startQr(req).respond(exchange)
        }
    }
    server.createContext("/api/qr/status") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<QrStatusRequest>()
            service.waitForQr(req).respond(exchange)
        }
    }
    server.createContext("/api/ticket") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<TicketRequest>()
            service.mintTicket(req).respond(exchange)
        }
    }
    server.createContext("/api/achievements") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<AchievementsRequest>()
            service.achievements(req).respond(exchange)
        }
    }
    server.createContext("/api/status") { exchange ->
        val query = exchange.query()
        service.status(
            flowId = query["flowId"].orEmpty(),
            flowToken = query["flowToken"].orEmpty(),
        ).respond(exchange)
    }
    server.createContext("/api/cancel") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<FlowRequest>()
            service.cancel(req).respond(exchange)
        }
    }

    server.executor = Executors.newCachedThreadPool()
    Runtime.getRuntime().addShutdownHook(Thread {
        service.close()
        server.stop(0)
    })
    server.start()

    println("jvm-minter listening on http://127.0.0.1:$port/ui/")
    println("API: POST /api/login, /api/guard, /api/approval, /api/qr/start, /api/qr/status, /api/ticket, /api/achievements; GET /api/status?flowId=...")
}

private fun healthResponse(): ApiResponse =
    ApiResponse(
        status = "ok",
        versionCode = MinterBuildConfig.VERSION_CODE,
        message = "jvm-minter is running",
    )

private class TicketMinterService {
    private val flows = ConcurrentHashMap<String, AuthFlow>()

    fun login(req: LoginRequest): ApiResponse = runBlocking {
        cleanup()
        if (req.username.isBlank() || req.password.isBlank()) {
            return@runBlocking ApiResponse(status = "error", message = "username and password are required")
        }

        val flow = newFlow(req.appId ?: DEFAULT_APP_ID)
        flows[flow.id] = flow
        flow.start(req.username, req.password, req.guardCode)
            .also { closeCompletedFlow(flow, it) }
    }

    fun startQr(req: QrStartRequest): ApiResponse = runBlocking {
        cleanup()
        val flow = newFlow(req.appId ?: DEFAULT_APP_ID)
        flows[flow.id] = flow
        flow.startQr()
            .also { closeCompletedFlow(flow, it) }
    }

    fun submitGuard(req: GuardRequest): ApiResponse = runBlocking {
        flow(req.flowId, req.flowToken)?.submitGuard(req.code)
            ?.also { closeCompletedFlow(req.flowId, it) }
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun waitForApproval(req: ApprovalRequest): ApiResponse = runBlocking {
        val waitSeconds = (req.waitSeconds ?: 15).coerceIn(1, APPROVAL_WAIT_SECONDS)
        flow(req.flowId, req.flowToken)?.waitForApproval(waitSeconds)
            ?.also { closeCompletedFlow(req.flowId, it) }
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun waitForQr(req: QrStatusRequest): ApiResponse = runBlocking {
        val waitSeconds = (req.waitSeconds ?: 15).coerceIn(1, APPROVAL_WAIT_SECONDS)
        flow(req.flowId, req.flowToken)?.waitForQr(waitSeconds)
            ?.also { closeCompletedFlow(req.flowId, it) }
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun mintTicket(req: TicketRequest): ApiResponse = runBlocking {
        cleanup()
        val session = req.steamSession
            ?: return@runBlocking ApiResponse(status = "session_invalid", message = "steamSession is required")
        val flow = newFlow(req.appId ?: DEFAULT_APP_ID)
        try {
            flow.mintFromSession(session)
        } finally {
            flow.close()
        }
    }

    fun achievements(req: AchievementsRequest): ApiResponse = runBlocking {
        cleanup()
        val session = req.steamSession
            ?: return@runBlocking ApiResponse(status = "session_invalid", message = "steamSession is required")
        val flow = newFlow(DEFAULT_APP_ID)
        try {
            flow.achievementsFromSession(session)
        } finally {
            flow.close()
        }
    }

    fun status(flowId: String, flowToken: String): ApiResponse = runBlocking {
        flow(flowId, flowToken)?.status()
            ?.also { closeCompletedFlow(flowId, it) }
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun cancel(req: FlowRequest): ApiResponse {
        val flow = flow(req.flowId, req.flowToken)
            ?: return ApiResponse(status = "error", message = "flow not found")
        flows.remove(flow.id, flow)
        flow.close()
        return ApiResponse(status = "cancelled", flowId = flow.id, flowToken = flow.flowToken)
    }

    fun close() {
        flows.values.forEach { it.close() }
        flows.clear()
    }

    private fun flow(flowId: String, flowToken: String): AuthFlow? {
        cleanup()
        return flows[flowId]?.takeIf { it.hasFlowToken(flowToken) }
    }

    private fun newFlow(appId: Int): AuthFlow =
        AuthFlow(
            id = UUID.randomUUID().toString(),
            flowToken = newFlowToken(),
            appId = appId,
        )

    private fun closeCompletedFlow(flowId: String, response: ApiResponse) {
        if (response.status != "success") return
        flows.remove(flowId)?.close()
    }

    private fun closeCompletedFlow(flow: AuthFlow, response: ApiResponse) {
        if (response.status != "success") return
        if (flows.remove(flow.id, flow)) flow.close()
    }

    private fun cleanup() {
        val cutoff = Instant.now().minusSeconds(15 * 60)
        flows.entries.removeIf { (_, flow) ->
            val stale = flow.createdAt.isBefore(cutoff)
            if (stale) flow.close()
            stale
        }
    }
}

private class AuthFlow(
    val id: String,
    val flowToken: String,
    private val appId: Int,
) {
    val createdAt: Instant = Instant.now()
    private val mutex = Mutex()
    private var steam: SteamCmSession? = null
    private var authSession: AuthSession? = null
    private var finished = false
    private var lastMessage: String = "created"
    private var qrUrl: String? = null
    private var qrCode: String? = null
    private var steamSession: SteamSessionPayload? = null

    fun hasFlowToken(token: String): Boolean =
        token.isNotBlank() && token.secureEquals(flowToken)

    private fun response(
        status: String,
        message: String? = null,
        guardKind: String? = null,
        challengeId: String? = null,
        expiresIn: Int? = null,
        qrUrl: String? = null,
        qrCode: String? = null,
        ticketHex: String? = null,
        ticketBytes: Int? = null,
        steamSession: SteamSessionPayload? = null,
        achievements: SteamAchievementsPayload? = null,
    ): ApiResponse =
        ApiResponse(
            status = status,
            flowId = id,
            flowToken = flowToken,
            message = message,
            guardKind = guardKind,
            challengeId = challengeId,
            expiresIn = expiresIn,
            qrUrl = qrUrl,
            qrCode = qrCode,
            appId = appId,
            ticketHex = ticketHex,
            ticketBytes = ticketBytes,
            steamSession = steamSession,
            achievements = achievements,
        )

    private fun closedResponse(): ApiResponse =
        response(status = "error", message = "flow is closed")

    suspend fun start(username: String, password: String, guardCode: String?): ApiResponse = mutex.withLock {
        val client = startedClient()
        qrUrl = null
        qrCode = null
        lastMessage = "signing in"

        val credentials = runCatching {
            client.client.authentication.beginAuthSessionViaCredentials(
                AuthSessionDetails().apply {
                    this.username = username.trim()
                    this.password = password
                    this.persistentSession = true
                    this.deviceFriendlyName = "LMU Assister JVM Minter"
                },
            ).awaitSteam(LOGIN_WAIT_MS)
        }.getOrElse {
            closeClient()
            return@withLock authError(it, "Steam sign-in failed")
        }

        authSession = credentials
        val typedCode = guardCode?.trim().orEmpty()
        if (typedCode.isNotBlank()) {
            return@withLock submitGuardCode(credentials, typedCode)
        }

        if (!credentials.requiresConfirmation()) {
            return@withLock waitForAuthAndMint(LOGIN_WAIT_MS / 1_000)
                ?: response(status = "error", message = "Steam sign-in timed out")
        }

        guardResponse(credentials)
    }

    suspend fun startQr(): ApiResponse = mutex.withLock {
        val client = startedClient()
        qrUrl = null
        qrCode = null
        lastMessage = "starting Steam QR sign-in"

        val qr = runCatching {
            client.client.authentication.beginAuthSessionViaQR(
                AuthSessionDetails().apply {
                    this.persistentSession = true
                    this.deviceFriendlyName = "LMU Assister JVM Minter"
                },
            ).awaitSteam(LOGIN_WAIT_MS)
        }.getOrElse {
            closeClient()
            return@withLock authError(it, "Steam QR sign-in failed")
        }

        qr.challengeUrlChanged = IChallengeUrlChanged { changed ->
            changed?.challengeUrl?.let(::setQrUrl)
        }
        authSession = qr
        setQrUrl(qr.challengeUrl)
        qrResponse()
    }

    suspend fun mintFromSession(session: SteamSessionPayload): ApiResponse = mutex.withLock {
        val steamId = session.steamId.trim().toLongOrNull()
            ?: return@withLock response(
                status = "session_invalid",
                message = "Steam session steamId is missing",
            )
        val accountName = session.accountName.trim()
        if (accountName.isBlank()) {
            return@withLock response(
                status = "session_invalid",
                message = "Steam session accountName is missing",
            )
        }
        val refreshToken = session.refreshToken.trim()
        if (refreshToken.isBlank()) {
            return@withLock response(
                status = "session_invalid",
                message = "Steam session refreshToken is missing",
            )
        }

        val client = startedClient()
        qrUrl = null
        qrCode = null
        lastMessage = "restoring Steam session"

        val restoredSteamId = runCatching {
            client.logOn(accountName, refreshToken).takeIf { it != 0L } ?: steamId
        }.getOrElse {
            closeClient()
            return@withLock response(
                status = "session_invalid",
                message = it.rootMessage("Steam refresh session expired. Sign in again."),
            )
        }

        steamSession = session.copy(steamId = restoredSteamId.toString())
        mintTicket()
    }

    suspend fun achievementsFromSession(session: SteamSessionPayload): ApiResponse = mutex.withLock {
        val prepared = prepareSession(session)
            ?: return@withLock response(status = "session_invalid", message = "Steam session accountName and refreshToken are required")
        var working = prepared
        lastMessage = "loading Steam achievements"

        val first = runCatching { fetchAchievements(working, DEFAULT_APP_ID) }
        val achievements = first.getOrElse { firstError ->
            if (firstError !is SteamWebApiAuthException) {
                return@withLock response(
                    status = "error",
                    message = firstError.rootMessage("Steam achievements request failed"),
                    steamSession = working,
                )
            }
            working = refreshAccessToken(working).getOrElse { refreshError ->
                return@withLock response(
                    status = "session_invalid",
                    message = refreshError.rootMessage("Steam refresh session expired. Sign in again."),
                    steamSession = working,
                )
            }
            runCatching { fetchAchievements(working, DEFAULT_APP_ID) }.getOrElse { retryError ->
                return@withLock response(
                    status = if (retryError is SteamWebApiAuthException) "session_invalid" else "error",
                    message = retryError.rootMessage("Steam achievements request failed"),
                    steamSession = working,
                )
            }
        }

        finished = true
        response(
            status = "success",
            message = "Steam achievements loaded",
            steamSession = working,
            achievements = achievements,
        )
    }

    suspend fun submitGuard(code: String): ApiResponse = mutex.withLock {
        val credentials = authSession as? CredentialsAuthSession
            ?: return@withLock closedResponse()
        if (code.isBlank()) return@withLock response(status = "error", message = "code is required")
        submitGuardCode(credentials, code.trim())
    }

    suspend fun waitForApproval(waitSeconds: Int): ApiResponse = mutex.withLock {
        if (finished) return@withLock closedResponse()
        val active = authSession
            ?: return@withLock closedResponse()
        if (!active.hasApproval()) {
            return@withLock authSessionResponse(active)
        }

        waitForAuthAndMint(waitSeconds.toLong()) ?: authSessionResponse(active)
    }

    suspend fun waitForQr(waitSeconds: Int): ApiResponse = mutex.withLock {
        if (finished) return@withLock closedResponse()
        authSession ?: return@withLock closedResponse()
        if (qrUrl == null) {
            return@withLock response(status = "pending", message = lastMessage)
        }

        waitForAuthAndMint(waitSeconds.toLong()) ?: qrResponse()
    }

    suspend fun status(): ApiResponse = mutex.withLock {
        if (finished) return@withLock closedResponse()
        val active = authSession
            ?: return@withLock response(status = "closed", message = lastMessage)

        pollOnceAndMint(STATUS_POLL_REQUEST_WAIT_MS)?.let { return@withLock it }

        if (qrUrl != null || active is QrAuthSession) {
            qrResponse()
        } else {
            authSessionResponse(active)
        }
    }

    fun close() {
        runCatching { closeClient() }
    }

    private fun startedClient(): SteamCmSession {
        val existing = steam
        if (existing != null) return existing
        return SteamCmSession().also {
            steam = it
            it.open()
            it.awaitConnected()
        }
    }

    private fun submitGuardCode(credentials: CredentialsAuthSession, code: String): ApiResponse {
        val codeType = credentials.preferredCodeType()
            ?: return guardResponse(credentials)
        runCatching {
            credentials.sendSteamGuardCode(code, codeType).awaitSteam(POLL_REQUEST_WAIT_MS)
        }.getOrElse {
            return guardResponse(credentials)
        }
        return waitForAuthAndMint(LOGIN_WAIT_MS / 1_000)
            ?: response(status = "error", message = "Steam sign-in timed out")
    }

    private fun waitForAuthAndMint(waitSeconds: Long): ApiResponse? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(waitSeconds)
        lastMessage = if (qrUrl != null) "waiting for Steam QR scan" else "waiting for Steam Guard approval"
        while (System.nanoTime() < deadline) {
            val remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                .coerceAtLeast(1L)
                .coerceAtMost(POLL_REQUEST_WAIT_MS)
            pollOnceAndMint(remainingMs)?.let { return it }
            Thread.sleep(authSession.pollIntervalMillis())
        }
        return null
    }

    private fun pollOnceAndMint(timeoutMs: Long = POLL_REQUEST_WAIT_MS): ApiResponse? {
        val result = runCatching { authSession?.pollAuthSessionStatus()?.awaitSteam(timeoutMs) }
            .getOrElse {
                return authError(it, "Steam auth polling failed")
            }
        return result?.let(::finishAuthAndMint)
    }

    private fun finishAuthAndMint(result: AuthPollResult): ApiResponse {
        val client = steam ?: return closedResponse()
        lastMessage = "logging on to Steam"
        val steamId = runCatching { client.logOn(result.accountName, result.refreshToken) }
            .getOrElse {
                closeClient()
                return authError(it, "Steam logon failed")
            }

        steamSession = SteamSessionPayload(
            steamId = steamId.toString(),
            accountName = result.accountName,
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
        )
        return mintTicket()
    }

    private fun mintTicket(): ApiResponse {
        if (finished) return closedResponse()
        val client = steam ?: return closedResponse()
        lastMessage = "minting ticket"
        val ticket = runCatching {
            client.authSessionTicket(appId)
        }.getOrElse {
            return authError(it, "ticket mint failed")
        }

        val response = response(
            status = "success",
            ticketHex = ticket.hex(),
            ticketBytes = ticket.size,
            steamSession = steamSession,
            message = "Steam auth session ticket minted",
        )
        finished = true
        closeClient()
        return response
    }

    private fun prepareSession(session: SteamSessionPayload): SteamSessionPayload? {
        val accountName = session.accountName.trim()
        val refreshToken = session.refreshToken.trim()
        if (accountName.isBlank() || refreshToken.isBlank()) return null
        return session.copy(
            steamId = session.steamId.trim(),
            accountName = accountName,
            accessToken = session.accessToken.trim(),
            refreshToken = refreshToken,
        )
    }

    private fun refreshAccessToken(session: SteamSessionPayload): Result<SteamSessionPayload> =
        runCatching {
            val client = startedClient()
            val steamId = session.steamId.toLongOrNull()?.takeIf { it > 0L }
                ?: client.logOn(session.accountName, session.refreshToken).takeIf { it > 0L }
                ?: error("Steam account id is missing")
            val renewed = client.renewAccessToken(steamId, session.refreshToken)
            session.copy(
                steamId = steamId.toString(),
                accessToken = renewed.accessToken,
                refreshToken = renewed.refreshToken.ifBlank { session.refreshToken },
            )
        }

    private fun fetchAchievements(session: SteamSessionPayload, appId: Int): SteamAchievementsPayload {
        val accessToken = session.accessToken.takeIf { it.isNotBlank() }
            ?: throw SteamWebApiAuthException("Steam access token is missing")
        val schemaRaw = steamWebApiGet(
            path = "/ISteamUserStats/GetSchemaForGame/v2/",
            params = mapOf(
                "appid" to appId.toString(),
                "l" to "english",
                "access_token" to accessToken,
            ),
        )
        val playerParams = mutableMapOf(
            "appid" to appId.toString(),
            "access_token" to accessToken,
        )
        session.steamId.takeIf { it.isNotBlank() }?.let { playerParams["steamid"] = it }
        val userRaw = steamWebApiGet(
            path = "/ISteamUserStats/GetPlayerAchievements/v1/",
            params = playerParams,
        )
        val schema = json.decodeFromString<SteamSchemaResponse>(schemaRaw)
        val user = json.decodeFromString<SteamPlayerAchievementsResponse>(userRaw)
        val userStats = user.playerstats ?: throw SteamWebApiException("Steam achievements response is empty")
        userStats.error?.takeIf { it.isNotBlank() }?.let { throw steamWebApiError(it) }
        if (userStats.success == false) throw SteamWebApiException("Steam achievements request failed")

        val progressByName = userStats.achievements.associateBy { it.apiname }
        val achievements = schema.game
            ?.availableGameStats
            ?.achievements
            .orEmpty()
            .mapNotNull { item ->
                val apiName = item.name.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val progress = progressByName[apiName]
                val displayName = item.displayName.takeIf { it.isNotBlank() } ?: apiName
                SteamAchievementPayload(
                    name = displayName,
                    description = item.description.takeIf { it.isNotBlank() },
                    achievedImageUrl = item.icon.steamAchievementIconUrl(appId),
                    lockedImageUrl = item.iconGray.steamAchievementIconUrl(appId),
                    achieved = progress?.achieved == 1,
                    unlockTime = progress?.unlockTime ?: 0L,
                )
            }
            .sortedWith(achievementDateComparator())

        return SteamAchievementsPayload(
            appId = appId,
            total = achievements.size,
            unlocked = achievements.count { it.achieved },
            achievements = achievements,
        )
    }

    private fun qrResponse(): ApiResponse {
        val url = qrUrl.orEmpty()
        lastMessage = "waiting for Steam QR scan"
        return response(
            status = "qr_required",
            qrUrl = url,
            qrCode = qrCode,
            expiresIn = APPROVAL_WAIT_SECONDS,
            message = "Scan the QR code in Steam mobile app",
        )
    }

    private fun guardResponse(session: AuthSession): ApiResponse {
        val hasApproval = session.hasApproval()
        val codeKind = session.codeKindOrNull()
        lastMessage = if (hasApproval) "waiting for Steam Guard approval" else "waiting for Steam Guard code"
        return response(
            status = if (hasApproval) "approval_required" else "guard_required",
            guardKind = codeKind?.name?.lowercase(),
            challengeId = id,
            expiresIn = APPROVAL_WAIT_SECONDS.takeIf { hasApproval },
            message = if (hasApproval) {
                "Approve in Steam app or submit a Steam Guard code"
            } else {
                "Submit Steam Guard code"
            },
        )
    }

    private fun authSessionResponse(session: AuthSession): ApiResponse =
        if (session.requiresConfirmation()) {
            guardResponse(session)
        } else {
            response(status = "pending", message = lastMessage)
        }

    private fun authError(error: Throwable, fallback: String): ApiResponse =
        response(
            status = "error",
            message = error.humanSteamMessage(fallback),
        )

    private fun setQrUrl(url: String) {
        qrUrl = url
        qrCode = url.toSteamQrDisplayCode()
    }

    private fun closeClient() {
        steam?.close()
        steam = null
        authSession = null
        qrUrl = null
        qrCode = null
    }
}

private class SteamCmSession {
    val client = SteamClient()
    private val manager = CallbackManager(client)
    private val subscriptions = mutableListOf<Closeable>()
    private val pump = Thread {
        while (running) {
            runCatching { manager.runWaitCallbacks(500L) }
        }
    }.apply {
        isDaemon = true
        name = "lmu-minter-steam-callbacks"
    }

    @Volatile private var running = true
    private val connectedLatch = CountDownLatch(1)
    @Volatile private var connectError: String? = null
    @Volatile private var loggedOnLatch: CountDownLatch? = null
    @Volatile private var logonResult: EResult? = null
    @Volatile private var loggedOnSteamId: Long = 0L

    fun open() {
        subscriptions += manager.subscribe(ConnectedCallback::class.java) {
            connectedLatch.countDown()
        }
        subscriptions += manager.subscribe(DisconnectedCallback::class.java) {
            connectError = "Disconnected from Steam"
            connectedLatch.countDown()
            loggedOnLatch?.countDown()
        }
        subscriptions += manager.subscribe(LoggedOnCallback::class.java) { cb: LoggedOnCallback ->
            logonResult = cb.result
            loggedOnSteamId = cb.clientSteamID?.convertToUInt64() ?: 0L
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
        connectError = null
        logonResult = null
        loggedOnSteamId = 0L
        val latch = CountDownLatch(1).also { loggedOnLatch = it }
        val user = client.getHandler(SteamUser::class.java) ?: error("SteamUser handler unavailable")
        user.logOn(
            LogOnDetails().apply {
                username = accountName
                accessToken = refreshToken
                shouldRememberPassword = true
                loginID = 149
            },
        )
        check(latch.await(30, TimeUnit.SECONDS)) { "Timed out logging on to Steam" }
        connectError?.let { error(it) }
        val result = logonResult
        if (result != EResult.OK) error("Steam logon failed: $result")
        return loggedOnSteamId
    }

    fun authSessionTicket(appId: Int): ByteArray {
        val handler = client.getHandler(SteamAuthTicket::class.java)
            ?: error("SteamAuthTicket handler unavailable")
        return handler.getAuthSessionTicket(appId).awaitSteam(LOGIN_WAIT_MS).ticket
    }

    fun renewAccessToken(steamId: Long, refreshToken: String): RenewedAccessToken {
        val res = client.authentication
            .generateAccessTokenForApp(SteamID(steamId), refreshToken, false)
            .awaitSteam(LOGIN_WAIT_MS)
        return RenewedAccessToken(
            accessToken = res.accessToken,
            refreshToken = res.refreshToken,
        )
    }

    fun close() {
        running = false
        subscriptions.forEach { runCatching { it.close() } }
        subscriptions.clear()
        runCatching { client.disconnect() }
    }
}

private data class RenewedAccessToken(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
private data class LoginRequest(
    val username: String = "",
    val password: String = "",
    val guardCode: String? = null,
    val appId: Int? = null,
)

@Serializable
private data class GuardRequest(
    val flowId: String = "",
    val flowToken: String = "",
    val code: String = "",
)

@Serializable
private data class ApprovalRequest(
    val flowId: String = "",
    val flowToken: String = "",
    val waitSeconds: Int? = null,
)

@Serializable
private data class QrStartRequest(
    val appId: Int? = null,
)

@Serializable
private data class QrStatusRequest(
    val flowId: String = "",
    val flowToken: String = "",
    val waitSeconds: Int? = null,
)

@Serializable
private data class TicketRequest(
    val appId: Int? = null,
    val steamSession: SteamSessionPayload? = null,
)

@Serializable
private data class AchievementsRequest(
    val steamSession: SteamSessionPayload? = null,
)

@Serializable
private data class FlowRequest(
    val flowId: String = "",
    val flowToken: String = "",
)

@Serializable
private data class SteamSessionPayload(
    val steamId: String = "",
    val accountName: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
)

@Serializable
private data class SteamAchievementsPayload(
    val appId: Int = DEFAULT_APP_ID,
    val total: Int = 0,
    val unlocked: Int = 0,
    val achievements: List<SteamAchievementPayload> = emptyList(),
)

@Serializable
private data class SteamAchievementPayload(
    val name: String,
    val description: String? = null,
    @SerialName("achieved_image_url") val achievedImageUrl: String? = null,
    @SerialName("locked_image_url") val lockedImageUrl: String? = null,
    val achieved: Boolean = false,
    @SerialName("unlock_time") val unlockTime: Long = 0L,
)

@Serializable
private data class ApiResponse(
    val status: String,
    val flowId: String? = null,
    val flowToken: String? = null,
    val versionCode: Int? = null,
    val message: String? = null,
    val guardKind: String? = null,
    val challengeId: String? = null,
    val expiresIn: Int? = null,
    val qrUrl: String? = null,
    val qrCode: String? = null,
    val appId: Int? = null,
    val ticketHex: String? = null,
    val ticketBytes: Int? = null,
    val steamSession: SteamSessionPayload? = null,
    val achievements: SteamAchievementsPayload? = null,
)

@Serializable
private data class SteamSchemaResponse(
    val game: SteamSchemaGame? = null,
)

@Serializable
private data class SteamSchemaGame(
    @SerialName("availableGameStats") val availableGameStats: SteamAvailableGameStats? = null,
)

@Serializable
private data class SteamAvailableGameStats(
    val achievements: List<SteamSchemaAchievement> = emptyList(),
)

@Serializable
private data class SteamSchemaAchievement(
    val name: String = "",
    @SerialName("displayName") val displayName: String = "",
    val description: String = "",
    val icon: String = "",
    @SerialName("icongray") val iconGray: String = "",
)

@Serializable
private data class SteamPlayerAchievementsResponse(
    val playerstats: SteamPlayerStats? = null,
)

@Serializable
private data class SteamPlayerStats(
    @SerialName("steamID") val steamId: String = "",
    val success: Boolean? = null,
    val error: String? = null,
    val achievements: List<SteamPlayerAchievement> = emptyList(),
)

@Serializable
private data class SteamPlayerAchievement(
    val apiname: String = "",
    val achieved: Int = 0,
    @SerialName("unlocktime") val unlockTime: Long = 0L,
)

private fun steamWebApiGet(path: String, params: Map<String, String>): String {
    val query = params.entries.joinToString("&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
    val url = "$STEAM_WEB_API_BASE$path?$query"
    val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/json")
    }
    return try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
            throw SteamWebApiAuthException(body.ifBlank { "Steam access token was rejected" })
        }
        if (code !in 200..299) {
            throw steamWebApiError(body.ifBlank { "Steam WebAPI failed with HTTP $code" })
        }
        if (body.isSteamAccessDenied()) throw SteamWebApiAuthException(body)
        body
    } finally {
        connection.disconnect()
    }
}

private fun steamWebApiError(message: String): SteamWebApiException =
    if (message.isSteamAccessDenied()) SteamWebApiAuthException(message) else SteamWebApiException(message)

private fun String.isSteamAccessDenied(): Boolean {
    val text = lowercase()
    return "access is denied" in text ||
        "access denied" in text ||
        "invalid access token" in text ||
        "expired access token" in text ||
        "token" in text && "invalid" in text
}

private fun achievementDateComparator(): Comparator<SteamAchievementPayload> =
    compareBy<SteamAchievementPayload> { if (it.unlockTime > 0L) 0 else 1 }
        .thenByDescending { it.unlockTime }
        .thenBy { it.name.lowercase() }

private fun String.steamAchievementIconUrl(appId: Int): String? {
    val value = trim().takeIf { it.isNotBlank() } ?: return null
    val httpsValue = value.replace("http://", "https://")
    return when {
        httpsValue.startsWith("https://") -> httpsValue
        httpsValue.startsWith("//") -> "https:$httpsValue"
        httpsValue.startsWith("/") -> "https://cdn.cloudflare.steamstatic.com$httpsValue"
        "/" in httpsValue -> "https://cdn.cloudflare.steamstatic.com/${httpsValue.trimStart('/')}"
        else -> {
            val file = if (httpsValue.endsWith(".jpg") || httpsValue.endsWith(".png")) httpsValue else "$httpsValue.jpg"
            "$STEAM_ACHIEVEMENT_ICON_BASE/$appId/$file"
        }
    }
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())

private open class SteamWebApiException(message: String) : RuntimeException(message)

private class SteamWebApiAuthException(message: String) : SteamWebApiException(message)

private fun ApiResponse.respond(exchange: HttpExchange) {
    exchange.respondJson(this, if (status == "error") 400 else 200)
}

private inline fun HttpExchange.requirePost(block: () -> Unit) {
    if (requestMethod == "OPTIONS") {
        respondNoContent(204)
        return
    }
    if (requestMethod != "POST") {
        respondJson(ApiResponse(status = "error", message = "POST required"), 405)
        return
    }
    runCatching(block).onFailure {
        respondJson(ApiResponse(status = "error", message = it.message ?: "request failed"), 400)
    }
}

private inline fun <reified T> HttpExchange.readJson(): T {
    val raw = requestBody.bufferedReader().use { it.readText() }
    if (requestHeaders.getFirst("Content-Type").orEmpty().contains("application/x-www-form-urlencoded")) {
        return json.decodeFromString(json.encodeToString(formAsMap(raw)))
    }
    return json.decodeFromString(raw)
}

private fun HttpExchange.query(): Map<String, String> =
    requestURI.rawQuery?.let(::formAsMap).orEmpty()

private fun formAsMap(raw: String): Map<String, String> =
    raw.split("&")
        .filter { it.isNotBlank() }
        .associate {
            val key = it.substringBefore("=")
            val value = it.substringAfter("=", "")
            decode(key) to decode(value)
        }

private fun decode(value: String): String =
    URLDecoder.decode(value, Charsets.UTF_8)

private fun HttpExchange.redirect(location: String) {
    responseHeaders.add("Location", location)
    addCorsHeaders()
    sendResponseHeaders(302, -1)
    close()
}

private fun HttpExchange.respondJson(value: ApiResponse, status: Int = 200) {
    val body = json.encodeToString(value)
    respond(body, status, "application/json; charset=utf-8")
}

private fun HttpExchange.respondHtml(body: String) {
    respond(body, 200, "text/html; charset=utf-8")
}

private fun HttpExchange.respond(body: String, status: Int, contentType: String) {
    val bytes = body.toByteArray()
    responseHeaders.add("Content-Type", contentType)
    responseHeaders.add("Cache-Control", "no-store")
    addCorsHeaders()
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}

private fun HttpExchange.respondNoContent(status: Int) {
    addCorsHeaders()
    sendResponseHeaders(status, -1)
    close()
}

private fun HttpExchange.addCorsHeaders() {
    responseHeaders.add("Access-Control-Allow-Origin", "*")
    responseHeaders.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
    responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
    responseHeaders.add("Access-Control-Max-Age", "600")
}

private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

private fun newFlowToken(): String {
    val bytes = ByteArray(FLOW_TOKEN_BYTES)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun String.secureEquals(other: String): Boolean =
    MessageDigest.isEqual(toByteArray(), other.toByteArray())

private fun String.toSteamQrDisplayCode(): String? =
    trim()
        .trimEnd('/')
        .substringAfterLast('/')
        .takeIf { it.length in 3..24 && it.all { ch -> ch.isLetterOrDigit() } }
        ?.uppercase()

private fun AuthSession.requiresConfirmation(): Boolean =
    allowedConfirmations.any { it.confirmationType != EAuthSessionGuardType.k_EAuthSessionGuardType_None }

private fun AuthSession.hasApproval(): Boolean =
    allowedConfirmations.any { it.confirmationType.isApprovalBased() }

private fun AuthSession.preferredCodeType(): EAuthSessionGuardType? =
    allowedConfirmations.firstOrNull { it.confirmationType == EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode }
        ?.confirmationType
        ?: allowedConfirmations.firstOrNull { it.confirmationType == EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode }
            ?.confirmationType

private fun AuthSession.codeKindOrNull(): SteamGuardKind? =
    allowedConfirmations.firstNotNullOfOrNull { it.codeKindOrNull() }

private fun CAuthentication_AllowedConfirmation.codeKindOrNull(): SteamGuardKind? =
    when (confirmationType) {
        EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode -> SteamGuardKind.EMAIL
        EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode -> SteamGuardKind.DEVICE
        else -> null
    }

private fun EAuthSessionGuardType.isApprovalBased(): Boolean =
    this == EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation ||
        this == EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation

private fun AuthSession?.pollIntervalMillis(): Long =
    (((this?.pollingInterval ?: 1f) * 1_000).toLong()).coerceIn(500L, 5_000L)

private fun <T> CompletableFuture<T>.awaitSteam(timeoutMs: Long): T =
    try {
        get(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    } catch (e: TimeoutException) {
        throw e
    }

private fun Throwable.humanSteamMessage(fallback: String): String {
    val root = rootCause()
    if (root is TimeoutException) return "$fallback: timed out"
    if (root is AuthenticationException) {
        return when (root.result) {
            EResult.InvalidPassword -> "Steam rejected the password"
            EResult.AccountNotFound -> "Steam account was not found"
            EResult.InvalidLoginAuthCode,
            EResult.ExpiredLoginAuthCode,
            EResult.TwoFactorCodeMismatch,
            -> "Steam Guard code was rejected"
            EResult.RateLimitExceeded,
            EResult.AccountLoginDeniedThrottle,
            -> "Steam rejected the request due to rate limiting"
            else -> root.message ?: fallback
        }
    }
    return root.message ?: fallback
}

private fun Throwable.rootMessage(fallback: String): String =
    rootCause().message ?: fallback

private fun Throwable.rootCause(): Throwable {
    var current = this
    while (current is ExecutionException && current.cause != null) {
        current = current.cause!!
    }
    return current
}

private enum class SteamGuardKind { EMAIL, DEVICE }

@Volatile
private var bouncyCastleReady = false

private fun ensureBouncyCastleProvider() {
    if (bouncyCastleReady) return
    synchronized(BouncyCastleProvider::class.java) {
        if (bouncyCastleReady) return
        runCatching {
            if (Security.getProvider("BC") !is BouncyCastleProvider) {
                Security.removeProvider("BC")
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            }
        }
        bouncyCastleReady = true
    }
}

private fun uiHtml(port: Int): String =
    """
    <!doctype html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width,initial-scale=1">
      <title>LMU JVM Minter</title>
      <style>
        :root { color-scheme: dark; font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
        body { margin: 0; min-height: 100vh; background: #101316; color: #edf1f5; display: grid; place-items: center; }
        main { width: min(760px, calc(100vw - 32px)); }
        h1 { font-size: 24px; margin: 0 0 6px; }
        p { color: #9aa7b3; margin: 0 0 18px; }
        section { border: 1px solid #2b333c; background: #171c21; border-radius: 8px; padding: 16px; }
        .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
        label { display: grid; gap: 6px; color: #b8c3cc; font-size: 13px; }
        input, textarea { border-radius: 6px; border: 1px solid #343d47; background: #0f1317; color: #f5f7fa; padding: 0 10px; font: inherit; }
        input { height: 40px; }
        textarea { min-height: 92px; padding: 10px; resize: vertical; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
        button { height: 42px; border: 0; border-radius: 6px; background: #e7b84e; color: #15100a; font-weight: 800; cursor: pointer; padding: 0 14px; }
        button.secondary { background: #2a333d; color: #edf1f5; }
        button:disabled { opacity: .48; cursor: default; }
        .row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-top: 14px; }
        .qr { display: none; margin-top: 14px; padding: 12px; border: 1px solid #343d47; border-radius: 8px; background: #0f1317; }
        .qr strong { display: block; color: #edf1f5; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 24px; letter-spacing: .08em; margin-bottom: 6px; }
        .qr a { color: #e7b84e; overflow-wrap: anywhere; }
        pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #0b0e11; border: 1px solid #252d35; border-radius: 8px; padding: 12px; min-height: 120px; color: #dbe4ec; }
        .full { grid-column: 1 / -1; }
        @media (max-width: 640px) { .grid { grid-template-columns: 1fr; } }
      </style>
    </head>
    <body>
      <main>
        <h1>LMU JVM Minter</h1>
        <p>Local JavaSteam login server on 127.0.0.1:$port. Default appId is $DEFAULT_APP_ID.</p>
        <section>
          <div class="grid">
            <label>Steam login <input id="username" autocomplete="username"></label>
            <label>Password <input id="password" type="password" autocomplete="current-password"></label>
            <label>Steam Guard code <input id="code" inputmode="numeric" placeholder="optional"></label>
            <label>App ID <input id="appId" value="$DEFAULT_APP_ID"></label>
            <label class="full">Steam session JSON <textarea id="steamSession" placeholder="Filled after successful login; paste it here to mint from refresh session"></textarea></label>
          </div>
          <div class="row">
            <button id="loginBtn">Login / mint</button>
            <button id="guardBtn" class="secondary">Submit code</button>
            <button id="approvalBtn" class="secondary">Check approval</button>
            <button id="qrBtn" class="secondary">Start QR</button>
            <button id="qrStatusBtn" class="secondary">Check QR</button>
            <button id="ticketBtn" class="secondary">Mint from session</button>
            <button id="statusBtn" class="secondary">Status</button>
            <button id="cancelBtn" class="secondary">Cancel</button>
          </div>
          <div id="qrBox" class="qr">
            <strong id="qrCode"></strong>
            <a id="qrUrl" href="#" target="_blank" rel="noreferrer"></a>
          </div>
        </section>
        <pre id="out">Ready.</pre>
      </main>
      <script>
        let flowId = "";
        let flowToken = "";
        const out = document.getElementById("out");
        const qrBox = document.getElementById("qrBox");
        const qrCode = document.getElementById("qrCode");
        const qrUrl = document.getElementById("qrUrl");
        const val = id => document.getElementById(id).value;
        const appId = () => Number(val("appId") || $DEFAULT_APP_ID);
        const parseSteamSession = () => {
          const raw = val("steamSession").trim();
          return raw ? JSON.parse(raw) : null;
        };
        const print = data => {
          if (data.flowId) flowId = data.flowId;
          if (data.flowToken) flowToken = data.flowToken;
          if (data.steamSession) {
            document.getElementById("steamSession").value = JSON.stringify(data.steamSession, null, 2);
          }
          if (data.qrUrl) {
            qrBox.style.display = "block";
            qrCode.textContent = data.qrCode || "QR";
            qrUrl.textContent = data.qrUrl;
            qrUrl.href = data.qrUrl;
          }
          out.textContent = JSON.stringify(data, null, 2);
        };
        async function post(url, body) {
          out.textContent = "Requesting " + url + "...";
          const r = await fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
          print(await r.json());
        }
        document.getElementById("loginBtn").onclick = () => post("/api/login", {
          username: val("username"),
          password: val("password"),
          guardCode: val("code") || null,
          appId: appId()
        });
        document.getElementById("guardBtn").onclick = () => post("/api/guard", { flowId, flowToken, code: val("code") });
        document.getElementById("approvalBtn").onclick = () => post("/api/approval", { flowId, flowToken, waitSeconds: 10 });
        document.getElementById("qrBtn").onclick = () => post("/api/qr/start", { appId: appId() });
        document.getElementById("qrStatusBtn").onclick = () => post("/api/qr/status", { flowId, flowToken, waitSeconds: 10 });
        document.getElementById("ticketBtn").onclick = () => post("/api/ticket", { appId: appId(), steamSession: parseSteamSession() });
        document.getElementById("statusBtn").onclick = async () => {
          const r = await fetch("/api/status?flowId=" + encodeURIComponent(flowId) + "&flowToken=" + encodeURIComponent(flowToken));
          print(await r.json());
        };
        document.getElementById("cancelBtn").onclick = () => post("/api/cancel", { flowId, flowToken });
      </script>
    </body>
    </html>
    """.trimIndent()
