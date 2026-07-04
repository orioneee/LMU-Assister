package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val WEB_APP_TOKEN_KEY = "web_companion_app_token"
private const val APPROVAL_WAIT_SECONDS = 15

private val CompanionJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Wasm/browser auth cannot talk to Steam directly. It asks the local JVM companion
 * to perform kSteam login and mint a Steam auth session ticket, then exchanges that
 * ticket with the normal backend /auth/steam endpoint.
 */
internal class WebCompanionSteamSignIn(
    private val backend: SteamBackendApi,
    private val client: HttpClient,
) : SteamSignIn {
    private val baseUrl = BuildConfig.COMPANION_URL.trimEnd('/')
    private var activeFlowId: String? = null

    override suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome {
        val code = guardCode?.trim().orEmpty()
        val existingFlow = activeFlowId
        val response = if (existingFlow != null && code.isNotBlank()) {
            companionPost("/api/guard", GuardRequest(existingFlow, code))
        } else {
            companionPost(
                "/api/login",
                LoginRequest(
                    username = username,
                    password = password,
                    guardCode = code.takeIf { it.isNotBlank() },
                    appId = LMU_APP_ID,
                ),
            )
        }
        return response.toOutcome()
    }

    override suspend fun continueDeviceConfirmation(challengeId: String): SignInOutcome {
        val flow = challengeId.ifBlank { activeFlowId.orEmpty() }
        if (flow.isBlank()) return SignInOutcome.Failure("Steam Guard flow expired. Sign in again.")
        return companionPost("/api/approval", ApprovalRequest(flow, APPROVAL_WAIT_SECONDS)).toOutcome()
    }

    override suspend fun startQrSignIn(): SignInOutcome =
        companionPost("/api/qr/start", QrStartRequest(appId = LMU_APP_ID)).toOutcome()

    override suspend fun continueQrSignIn(flowId: String): SignInOutcome {
        val flow = flowId.ifBlank { activeFlowId.orEmpty() }
        if (flow.isBlank()) return SignInOutcome.Failure("Steam QR flow expired. Start again.")
        return companionPost("/api/qr/status", QrStatusRequest(flow, waitSeconds = APPROVAL_WAIT_SECONDS)).toOutcome()
    }

    override suspend fun restore(): String? =
        LocalCache.read(WEB_APP_TOKEN_KEY)?.takeIf { it.isNotBlank() }

    override suspend fun reauth(): String? = null

    override fun signOut() {
        activeFlowId = null
        LocalCache.remove(WEB_APP_TOKEN_KEY)
    }

    private suspend inline fun <reified T> companionPost(path: String, body: T): CompanionResponse =
        try {
            client.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                setBody(CompanionJson.encodeToString(body))
            }.bodyAsText().let(CompanionJson::decodeFromString)
        } catch (t: Throwable) {
            CompanionResponse(
                status = "error",
                message = "Couldn't reach JVM companion at $baseUrl. Start :jvm-minter first.",
            )
        }

    private suspend fun CompanionResponse.toOutcome(): SignInOutcome =
        when (status) {
            "success" -> exchangeTicket()
            "approval_required" -> {
                val flow = flowId.orEmpty()
                activeFlowId = flow
                SignInOutcome.DeviceConfirmationPending(
                    challengeId = flow,
                    expiresIn = expiresIn ?: 120,
                )
            }
            "guard_required" -> {
                activeFlowId = flowId
                SignInOutcome.GuardRequired(guardKind.toSteamGuardKind())
            }
            "qr_required" -> {
                val flow = flowId.orEmpty()
                activeFlowId = flow
                SignInOutcome.QrCodePending(
                    flowId = flow,
                    challengeUrl = qrUrl.orEmpty(),
                    displayCode = qrCode,
                    expiresIn = expiresIn ?: 120,
                )
            }
            "pending" -> {
                activeFlowId = flowId
                if (!qrUrl.isNullOrBlank()) {
                    SignInOutcome.QrCodePending(
                        flowId = flowId.orEmpty(),
                        challengeUrl = qrUrl,
                        displayCode = qrCode,
                        expiresIn = expiresIn ?: 120,
                    )
                } else {
                    SignInOutcome.DeviceConfirmationPending(
                        challengeId = flowId.orEmpty(),
                        expiresIn = expiresIn ?: 120,
                    )
                }
            }
            else -> SignInOutcome.Failure(message ?: "Steam companion auth failed")
        }

    private suspend fun CompanionResponse.exchangeTicket(): SignInOutcome {
        val ticket = ticketHex
            ?: return SignInOutcome.Failure("JVM companion did not return a Steam ticket.")
        return try {
            val auth = backend.authSteam(ticket)
            activeFlowId = null
            LocalCache.write(WEB_APP_TOKEN_KEY, auth.token)
            SignInOutcome.Success(auth.token, auth.uid)
        } catch (t: Throwable) {
            SignInOutcome.Failure(t.message ?: "Steam ticket exchange failed")
        }
    }

    private fun String?.toSteamGuardKind(): SteamGuardKind =
        when (this?.lowercase()) {
            "email" -> SteamGuardKind.EMAIL
            else -> SteamGuardKind.DEVICE
        }
}

@Serializable
private data class LoginRequest(
    val username: String,
    val password: String,
    val guardCode: String? = null,
    val appId: Int,
)

@Serializable
private data class GuardRequest(
    val flowId: String,
    val code: String,
)

@Serializable
private data class ApprovalRequest(
    val flowId: String,
    val waitSeconds: Int,
)

@Serializable
private data class QrStartRequest(
    val appId: Int,
)

@Serializable
private data class QrStatusRequest(
    val flowId: String,
    val waitSeconds: Int,
)

@Serializable
private data class CompanionResponse(
    val status: String,
    val flowId: String? = null,
    val message: String? = null,
    val guardKind: String? = null,
    val challengeId: String? = null,
    val expiresIn: Int? = null,
    val qrUrl: String? = null,
    val qrCode: String? = null,
    val appId: Int? = null,
    val ticketHex: String? = null,
    val ticketBytes: Int? = null,
)
