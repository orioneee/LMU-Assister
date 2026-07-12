package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.config.BuildConfig

/**
 * Tiny tagged logger for the Steam auth flow. Disabled unless explicitly enabled from
 * local.properties with steam.logs=true.
 */
internal object SteamLog {
    private const val TAG = "[SteamAuth]"

    fun d(msg: String) {
        if (BuildConfig.STEAM_LOGS) println("$TAG $msg")
    }

    fun e(msg: String, t: Throwable? = null) {
        if (!BuildConfig.STEAM_LOGS) return
        println("$TAG ERROR $msg" + (t?.let { " :: ${it::class.simpleName}" } ?: ""))
    }

    fun short(s: String?): String = when {
        s == null -> "null"
        s.length <= 12 -> s
        else -> "${s.take(6)}...${s.takeLast(4)}(${s.length})"
    }
}
