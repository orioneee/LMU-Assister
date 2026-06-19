package com.orioooneee.lmuasister.data.steam

/**
 * Tiny tagged logger for the Steam auth / tunnel flow. Uses [println] so it shows up on
 * every platform: Android Logcat (tag `System.out`, filter `[SteamAuth]`), JVM stdout,
 * iOS Xcode console. Never logs passwords; tokens are truncated.
 */
internal object SteamLog {
    private const val TAG = "[SteamAuth]"

    fun d(msg: String) = println("$TAG $msg")

    fun e(msg: String, t: Throwable? = null) =
        println("$TAG ERROR $msg" + (t?.let { " :: ${it::class.simpleName}: ${it.message}" } ?: ""))

    /** Shorten a token/secret for logs (keep head + tail). */
    fun short(s: String?): String = when {
        s == null -> "null"
        s.length <= 12 -> s
        else -> "${s.take(6)}…${s.takeLast(4)}(${s.length})"
    }
}
