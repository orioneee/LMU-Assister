package com.orioooneee.lmuasister.data.steam

import kotlinx.serialization.Serializable

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

/** Which Steam Guard channel Steam is asking the user to confirm through. */
enum class SteamGuardKind { EMAIL, DEVICE }
