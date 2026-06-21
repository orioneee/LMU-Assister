package com.orioooneee.lmuasister.data.steam

const val LMU_APP_ID: Int = 2399420

class SteamSessionTicket(val appId: Int, val bytes: ByteArray) {
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

sealed interface SteamLoginResult {
    data class Success(val tokens: SteamTokens) : SteamLoginResult

    data class GuardRequired(val kind: SteamGuardKind) : SteamLoginResult

    data class Failure(val reason: String) : SteamLoginResult
}
