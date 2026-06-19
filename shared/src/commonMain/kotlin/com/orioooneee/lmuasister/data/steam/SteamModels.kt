package com.orioooneee.lmuasister.data.steam

import kotlinx.serialization.Serializable

/** Le Mans Ultimate Steam app id — used when requesting game session tickets. */
const val LMU_APP_ID: Int = 2399420

/** Steam tokens obtained from a successful credential login. */
@Serializable
data class SteamTokens(
    val steamId: Long,
    val accountName: String,
    /** Short-lived JWT (~24h). Used for Steam WebAPI calls and re-logon. */
    val accessToken: String,
    /** Long-lived. Re-logon without a password and mint fresh access tokens. */
    val refreshToken: String,
    /** Opaque machine token — pass back as `guardData` on next login to skip Steam Guard. */
    val guardData: String? = null,
)

/** A game session ticket to hand to the game backend. */
class SteamSessionTicket(val appId: Int, val bytes: ByteArray) {
    /** Hex-encoded ticket — the form game web backends (AuthenticateUserTicket) expect. */
    val hex: String by lazy(LazyThreadSafetyMode.NONE) {
        buildString(bytes.size * 2) {
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                append(HEX[v ushr 4]); append(HEX[v and 0x0F])
            }
        }
    }

    private companion object {
        private const val HEX = "0123456789abcdef"
    }
}

/** Which Steam Guard channel Steam is asking the user to confirm through. */
enum class SteamGuardKind { EMAIL, DEVICE }

/** Outcome of a credential login attempt. */
sealed interface SteamLoginResult {
    data class Success(val tokens: SteamTokens) : SteamLoginResult

    /** A Steam Guard code is required — prompt the user, then retry login with it. */
    data class GuardRequired(val kind: SteamGuardKind) : SteamLoginResult

    /** Login failed (bad credentials / wrong code / network). */
    data class Failure(val reason: String) : SteamLoginResult
}
