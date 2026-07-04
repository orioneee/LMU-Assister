package com.orioooneee.lmuasister.minter

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.kSteam
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.account.AuthorizationState
import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import org.bouncycastle.jce.provider.BouncyCastleProvider
import steam.enums.EAuthTokenPlatformType
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.security.Security
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_APP_ID = 2_399_420
private const val DEFAULT_PORT = 8787
private const val LOGIN_WAIT_MS = 45_000L
private const val GUARD_PROMPT_WAIT_MS = 2_000L
private const val APPROVAL_WAIT_SECONDS = 120

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

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
    server.createContext("/api/health") { exchange ->
        exchange.respondJson(ApiResponse(status = "ok", message = "jvm-minter is running"))
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
    server.createContext("/api/status") { exchange ->
        val flowId = exchange.query()["flowId"].orEmpty()
        service.status(flowId).respond(exchange)
    }
    server.createContext("/api/cancel") { exchange ->
        exchange.requirePost {
            val req = exchange.readJson<FlowRequest>()
            service.cancel(req.flowId).respond(exchange)
        }
    }

    server.executor = Executors.newCachedThreadPool()
    Runtime.getRuntime().addShutdownHook(Thread {
        service.close()
        server.stop(0)
    })
    server.start()

    println("jvm-minter listening on http://127.0.0.1:$port/ui/")
    println("API: POST /api/login, /api/guard, /api/approval, GET /api/status?flowId=...")
}

private class TicketMinterService {
    private val flows = ConcurrentHashMap<String, AuthFlow>()

    fun login(req: LoginRequest): ApiResponse = runBlocking {
        cleanup()
        if (req.username.isBlank() || req.password.isBlank()) {
            return@runBlocking ApiResponse(status = "error", message = "username and password are required")
        }

        val flow = AuthFlow(
            id = UUID.randomUUID().toString(),
            appId = req.appId ?: DEFAULT_APP_ID,
            rootDir = minterRoot().resolve(UUID.randomUUID().toString()),
        )
        flows[flow.id] = flow
        flow.start(req.username, req.password, req.guardCode)
    }

    fun submitGuard(req: GuardRequest): ApiResponse = runBlocking {
        flow(req.flowId)?.submitGuard(req.code)
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun waitForApproval(req: ApprovalRequest): ApiResponse = runBlocking {
        val waitSeconds = (req.waitSeconds ?: 15).coerceIn(1, APPROVAL_WAIT_SECONDS)
        flow(req.flowId)?.waitForApproval(waitSeconds)
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun status(flowId: String): ApiResponse = runBlocking {
        flow(flowId)?.status()
            ?: ApiResponse(status = "error", message = "flow not found")
    }

    fun cancel(flowId: String): ApiResponse {
        val flow = flows.remove(flowId) ?: return ApiResponse(status = "error", message = "flow not found")
        flow.close()
        return ApiResponse(status = "cancelled", flowId = flowId)
    }

    fun close() {
        flows.values.forEach { it.close() }
        flows.clear()
    }

    private fun flow(flowId: String): AuthFlow? {
        cleanup()
        return flows[flowId]
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
    private val appId: Int,
    private val rootDir: java.nio.file.Path,
) {
    val createdAt: Instant = Instant.now()
    private val mutex = Mutex()
    private var steam: SteamClient? = null
    private var completed: ApiResponse? = null
    private var lastMessage: String = "created"

    suspend fun start(username: String, password: String, guardCode: String?): ApiResponse = mutex.withLock {
        val client = startedClient()
        lastMessage = "signing in"
        val authResult = runCatching {
            client.account.signIn(username.trim(), password, rememberSession = false)
        }.getOrElse {
            closeClient()
            return@withLock ApiResponse(status = "error", flowId = id, message = it.message ?: "Steam sign-in failed")
        }

        when (authResult) {
            Account.AuthorizationResult.InvalidPassword -> {
                closeClient()
                return@withLock ApiResponse(status = "error", flowId = id, message = "Steam rejected the password")
            }
            Account.AuthorizationResult.RpcError -> {
                closeClient()
                return@withLock ApiResponse(status = "error", flowId = id, message = "Steam auth RPC failed")
            }
            Account.AuthorizationResult.ProceedToTfa -> Unit
        }

        val awaiting = awaitGuardPrompt(client)
        if (awaiting == null) return@withLock mintTicket(client)

        val typedCode = guardCode?.trim().orEmpty()
        if (typedCode.isNotBlank()) return@withLock submitGuardCode(client, typedCode)

        guardResponse(awaiting)
    }

    suspend fun submitGuard(code: String): ApiResponse = mutex.withLock {
        val client = steam ?: return@withLock ApiResponse(status = "error", flowId = id, message = "flow is closed")
        if (code.isBlank()) return@withLock ApiResponse(status = "error", flowId = id, message = "code is required")
        submitGuardCode(client, code.trim())
    }

    suspend fun waitForApproval(waitSeconds: Int): ApiResponse = mutex.withLock {
        val client = steam ?: return@withLock completed ?: ApiResponse(status = "error", flowId = id, message = "flow is closed")
        val state = client.account.clientAuthState.value
        if (state !is AuthorizationState.AwaitingTwoFactor) {
            return@withLock mintTicket(client)
        }
        val hasApproval = state.supportedConfirmationMethods.any { it.isApprovalBased() }
        if (!hasApproval) return@withLock guardResponse(state)

        lastMessage = "waiting for Steam Guard approval"
        val signedIn = withTimeoutOrNull(waitSeconds.seconds) {
            client.account.clientAuthState.filterIsInstance<AuthorizationState.Success>().first()
        }
        if (signedIn != null) mintTicket(client) else guardResponse(state)
    }

    suspend fun status(): ApiResponse = mutex.withLock {
        completed?.let { return@withLock it }
        val client = steam ?: return@withLock ApiResponse(status = "closed", flowId = id, message = lastMessage)
        when (val state = client.account.clientAuthState.value) {
            AuthorizationState.Success -> mintTicket(client)
            AuthorizationState.Unauthorized -> ApiResponse(status = "pending", flowId = id, appId = appId, message = lastMessage)
            is AuthorizationState.AwaitingTwoFactor -> guardResponse(state)
        }
    }

    fun close() {
        runCatching { closeClient() }
    }

    private suspend fun startedClient(): SteamClient {
        val existing = steam
        if (existing != null) return existing
        rootDir.createDirectories()
        return kSteam {
            rootFolder = rootDir.toString().toPath(normalize = true)
            persistenceDriver = MemoryPersistenceDriver
            deviceInfo = DeviceInformation(
                osType = EOSType.k_WinUnknown,
                gamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC,
                deviceName = "LMU Assister JVM Minter",
                platformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
            )
        }.also {
            steam = it
            it.start()
        }
    }

    private suspend fun awaitGuardPrompt(client: SteamClient): AuthorizationState.AwaitingTwoFactor? =
        (client.account.clientAuthState.value as? AuthorizationState.AwaitingTwoFactor)
            ?: withTimeoutOrNull(GUARD_PROMPT_WAIT_MS) {
                client.account.clientAuthState.filterIsInstance<AuthorizationState.AwaitingTwoFactor>().first()
            }

    private suspend fun submitGuardCode(client: SteamClient, code: String): ApiResponse {
        val accepted = runCatching { client.account.updateCurrentSessionWithCode(code) }.getOrDefault(false)
        if (!accepted) {
            val state = client.account.clientAuthState.value as? AuthorizationState.AwaitingTwoFactor
            return if (state != null) guardResponse(state)
            else ApiResponse(status = "error", flowId = id, message = "Steam Guard code was rejected")
        }
        return waitForSignedInAndMint(client)
    }

    private suspend fun waitForSignedInAndMint(client: SteamClient): ApiResponse =
        try {
            withTimeout(LOGIN_WAIT_MS) {
                client.account.clientAuthState.filterIsInstance<AuthorizationState.Success>().first()
                mintTicket(client)
            }
        } catch (_: TimeoutCancellationException) {
            ApiResponse(status = "error", flowId = id, message = "Steam sign-in timed out")
        }

    private suspend fun mintTicket(client: SteamClient): ApiResponse {
        completed?.let { return it }
        lastMessage = "minting ticket"
        val ticket = runCatching {
            withTimeout(LOGIN_WAIT_MS) {
                client.authTickets.getAuthSessionTicket(AppId(appId)).ticket
            }
        }.getOrElse {
            return ApiResponse(status = "error", flowId = id, message = it.message ?: "ticket mint failed")
        }
        val response = ApiResponse(
            status = "success",
            flowId = id,
            appId = appId,
            ticketHex = ticket.hex(),
            ticketBytes = ticket.size,
            message = "Steam auth session ticket minted",
        )
        completed = response
        return response
    }

    private fun guardResponse(state: AuthorizationState.AwaitingTwoFactor): ApiResponse {
        val hasApproval = state.supportedConfirmationMethods.any { it.isApprovalBased() }
        val codeKind = state.supportedConfirmationMethods.firstNotNullOfOrNull { it.codeKindOrNull() }
        lastMessage = if (hasApproval) "waiting for Steam Guard approval" else "waiting for Steam Guard code"
        return ApiResponse(
            status = if (hasApproval) "approval_required" else "guard_required",
            flowId = id,
            appId = appId,
            guardKind = codeKind?.name?.lowercase(),
            challengeId = state.steamId.toString(),
            expiresIn = APPROVAL_WAIT_SECONDS.takeIf { hasApproval },
            message = if (hasApproval) {
                "Approve in Steam app or submit a Steam Guard code"
            } else {
                "Submit Steam Guard code"
            },
        )
    }

    private fun closeClient() {
        steam?.let {
            runCatching { it.account.cancelSignInAttempt() }
            runCatching { it.stop() }
        }
        steam = null
    }
}

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
    val code: String = "",
)

@Serializable
private data class ApprovalRequest(
    val flowId: String = "",
    val waitSeconds: Int? = null,
)

@Serializable
private data class FlowRequest(val flowId: String = "")

@Serializable
private data class ApiResponse(
    val status: String,
    val flowId: String? = null,
    val message: String? = null,
    val guardKind: String? = null,
    val challengeId: String? = null,
    val expiresIn: Int? = null,
    val appId: Int? = null,
    val ticketHex: String? = null,
    val ticketBytes: Int? = null,
)

private fun ApiResponse.respond(exchange: HttpExchange) {
    exchange.respondJson(this, if (status == "error") 400 else 200)
}

private inline fun HttpExchange.requirePost(block: () -> Unit) {
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
    responseHeaders.add("Access-Control-Allow-Origin", "*")
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}

private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

private fun AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.isApprovalBased(): Boolean =
    this == AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.DeviceConfirmation ||
        this == AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.EmailConfirmation

private fun AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.codeKindOrNull(): SteamGuardKind? =
    when (this) {
        AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.EmailCode -> SteamGuardKind.EMAIL
        AuthorizationState.AwaitingTwoFactor.ConfirmationMethod.DeviceCode -> SteamGuardKind.DEVICE
        else -> null
    }

private enum class SteamGuardKind { EMAIL, DEVICE }

private fun minterRoot(): java.nio.file.Path {
    val base = System.getProperty("java.io.tmpdir") ?: "."
    return Path(base, "lmu-jvm-minter").also { it.createDirectories() }
}

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
        input { height: 40px; border-radius: 6px; border: 1px solid #343d47; background: #0f1317; color: #f5f7fa; padding: 0 10px; font: inherit; }
        button { height: 42px; border: 0; border-radius: 6px; background: #e7b84e; color: #15100a; font-weight: 800; cursor: pointer; padding: 0 14px; }
        button.secondary { background: #2a333d; color: #edf1f5; }
        button:disabled { opacity: .48; cursor: default; }
        .row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-top: 14px; }
        pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #0b0e11; border: 1px solid #252d35; border-radius: 8px; padding: 12px; min-height: 120px; color: #dbe4ec; }
        .full { grid-column: 1 / -1; }
        @media (max-width: 640px) { .grid { grid-template-columns: 1fr; } }
      </style>
    </head>
    <body>
      <main>
        <h1>LMU JVM Minter</h1>
        <p>Local kSteam login server on 127.0.0.1:$port. Default appId is $DEFAULT_APP_ID.</p>
        <section>
          <div class="grid">
            <label>Steam login <input id="username" autocomplete="username"></label>
            <label>Password <input id="password" type="password" autocomplete="current-password"></label>
            <label>Steam Guard code <input id="code" inputmode="numeric" placeholder="optional"></label>
            <label>App ID <input id="appId" value="$DEFAULT_APP_ID"></label>
          </div>
          <div class="row">
            <button id="loginBtn">Login / mint</button>
            <button id="guardBtn" class="secondary">Submit code</button>
            <button id="approvalBtn" class="secondary">Check approval</button>
            <button id="statusBtn" class="secondary">Status</button>
            <button id="cancelBtn" class="secondary">Cancel</button>
          </div>
        </section>
        <pre id="out">Ready.</pre>
      </main>
      <script>
        let flowId = "";
        const out = document.getElementById("out");
        const val = id => document.getElementById(id).value;
        const print = data => {
          if (data.flowId) flowId = data.flowId;
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
          appId: Number(val("appId") || $DEFAULT_APP_ID)
        });
        document.getElementById("guardBtn").onclick = () => post("/api/guard", { flowId, code: val("code") });
        document.getElementById("approvalBtn").onclick = () => post("/api/approval", { flowId, waitSeconds: 10 });
        document.getElementById("statusBtn").onclick = async () => {
          const r = await fetch("/api/status?flowId=" + encodeURIComponent(flowId));
          print(await r.json());
        };
        document.getElementById("cancelBtn").onclick = () => post("/api/cancel", { flowId });
      </script>
    </body>
    </html>
    """.trimIndent()
