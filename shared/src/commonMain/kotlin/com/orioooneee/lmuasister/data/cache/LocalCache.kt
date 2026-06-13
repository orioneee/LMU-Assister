package com.orioooneee.lmuasister.data.cache

/**
 * Tiny persistent string store, platform-backed, for offline-first caching.
 * Reads/writes are best-effort — failures are swallowed (cache is never critical).
 */
expect object LocalCache {
    fun read(key: String): String?
    fun write(key: String, value: String)
}
