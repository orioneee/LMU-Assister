package com.orioooneee.lmuasister.data.cache

// Web: rely on the browser's own HTTP cache for now (no-op persistent store).
actual object LocalCache {
    actual fun read(key: String): String? = null
    actual fun write(key: String, value: String) {}
}
