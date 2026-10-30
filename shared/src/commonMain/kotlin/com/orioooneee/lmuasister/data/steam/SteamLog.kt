package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.config.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.TimeSource

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
        println("$TAG ERROR $msg" + (t?.let { " :: ${causes(it)}" } ?: ""))
    }

    /** Never print exception messages: HTTP/library exceptions may contain credentials or bodies. */
    private fun causes(t: Throwable): String = buildString {
        var cause: Throwable? = t
        repeat(6) {
            val current = cause ?: return@buildString
            if (isNotEmpty()) append(" <- ")
            append(current::class.simpleName ?: "Throwable")
            cause = current.cause.takeUnless { it === current }
        }
    }

    /** Each slow stage reports progress until completion; cancellation and timeouts stay distinct. */
    suspend fun <T> step(
        stage: String,
        state: () -> String = { "" },
        block: suspend () -> T,
    ): T {
        if (!BuildConfig.STEAM_LOGS) return block()
        val id = Random.nextInt().toUInt().toString(16)
        val mark = TimeSource.Monotonic.markNow()
        fun detail() = "op=$id stage=$stage elapsedMs=${mark.elapsedNow().inWholeMilliseconds} ${state()}"
        d("START ${detail()}")
        return coroutineScope {
            val progress = launch {
                while (true) {
                    delay(5_000)
                    d("WAIT ${detail()}")
                }
            }
            try {
                block().also { d("DONE ${detail()}") }
            } catch (t: Throwable) {
                val result = when (t) {
                    is TimeoutCancellationException -> "TIMEOUT"
                    is CancellationException -> "CANCELLED"
                    else -> "FAILED"
                }
                e("$result ${detail()}", t)
                throw t
            } finally {
                progress.cancel()
            }
        }
    }

    fun short(s: String?): String = when {
        s == null -> "null"
        s.length <= 12 -> s
        else -> "${s.take(6)}...${s.takeLast(4)}(${s.length})"
    }
}
