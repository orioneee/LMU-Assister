package com.orioooneee.lmuasister.featureflags

import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.AppJson
import kotlin.time.Clock
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private const val CACHE_KEY = "feature_flags_v1"

enum class FeatureFlagKey(
    val remoteName: String,
    val defaultValue: Boolean,
) {
    ShowTimerInScheduleCard(
        remoteName = "SHOW_TIMER_IN_SCHEDULE_CARD",
        defaultValue = false,
    ),
}

data class FeatureFlags(
    val showTimerInScheduleCard: Boolean = FeatureFlagKey.ShowTimerInScheduleCard.defaultValue,
) {
    fun toMap(): Map<FeatureFlagKey, Boolean> = mapOf(
        FeatureFlagKey.ShowTimerInScheduleCard to showTimerInScheduleCard,
    )

    companion object {
        val Defaults = FeatureFlags()

        fun from(values: Map<FeatureFlagKey, Boolean>): FeatureFlags =
            FeatureFlags(
                showTimerInScheduleCard = values[FeatureFlagKey.ShowTimerInScheduleCard]
                    ?: FeatureFlagKey.ShowTimerInScheduleCard.defaultValue,
            )
    }
}

interface FeatureFlagRemoteSource {
    fun fetch(keys: List<FeatureFlagKey>, onComplete: (Map<String, Boolean>) -> Unit)
}

class NoopFeatureFlagRemoteSource : FeatureFlagRemoteSource {
    override fun fetch(keys: List<FeatureFlagKey>, onComplete: (Map<String, Boolean>) -> Unit) {
        onComplete(emptyMap())
    }

}

expect fun platformFeatureFlagRemoteSource(): FeatureFlagRemoteSource

class FeatureFlagsRepository(
    private val remoteSource: FeatureFlagRemoteSource,
) {
    private val _flags = MutableStateFlow(readCached() ?: FeatureFlags.Defaults)
    val flags: StateFlow<FeatureFlags> = _flags.asStateFlow()

    suspend fun refresh() {
        val fetched = remoteSource.fetchOnce(FeatureFlagKey.entries)
            .mapKeys { (remoteName, _) -> FeatureFlagKey.entries.firstOrNull { it.remoteName == remoteName } }
            .mapNotNullKeys()
        if (fetched.isEmpty()) return

        val merged = _flags.value.toMap().toMutableMap()
        fetched.forEach { (key, value) -> merged[key] = value }
        val next = FeatureFlags.from(merged)
        _flags.value = next
        writeCached(next)
    }

    private fun readCached(): FeatureFlags? =
        runCatching {
            LocalCache.read(CACHE_KEY)
                ?.let { AppJson.decodeFromString<CachedFeatureFlags>(it) }
                ?.values
                ?.mapKeys { (remoteName, _) -> FeatureFlagKey.entries.firstOrNull { it.remoteName == remoteName } }
                ?.mapNotNullKeys()
                ?.let { FeatureFlags.from(it) }
        }.getOrNull()

    private fun writeCached(flags: FeatureFlags) {
        runCatching {
            val cached = CachedFeatureFlags(
                ts = Clock.System.now().toEpochMilliseconds(),
                values = flags.toMap().mapKeys { (key, _) -> key.remoteName },
            )
            LocalCache.write(CACHE_KEY, AppJson.encodeToString(cached))
        }
    }
}

@Serializable
private data class CachedFeatureFlags(
    val ts: Long = 0,
    val values: Map<String, Boolean> = emptyMap(),
)

private suspend fun FeatureFlagRemoteSource.fetchOnce(keys: List<FeatureFlagKey>): Map<String, Boolean> =
    suspendCancellableCoroutine { continuation ->
        runCatching {
            fetch(keys) { values ->
                if (continuation.isActive) continuation.resume(values)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(emptyMap())
        }
    }

private fun Map<FeatureFlagKey?, Boolean>.mapNotNullKeys(): Map<FeatureFlagKey, Boolean> =
    entries.mapNotNull { (key, value) -> key?.let { it to value } }.toMap()
