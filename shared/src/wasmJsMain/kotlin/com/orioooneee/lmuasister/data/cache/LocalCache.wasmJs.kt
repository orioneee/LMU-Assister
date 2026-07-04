package com.orioooneee.lmuasister.data.cache

import kotlinx.browser.localStorage

actual object LocalCache {
    actual fun read(key: String): String? =
        runCatching { localStorage.getItem(key) }.getOrNull()

    actual fun write(key: String, value: String) {
        runCatching { localStorage.setItem(key, value) }
    }

    actual fun remove(key: String) {
        runCatching { localStorage.removeItem(key) }
    }

    actual fun removeByPrefix(prefix: String) {
        runCatching {
            val keys = buildList {
                for (i in 0 until localStorage.length) {
                    localStorage.key(i)?.takeIf { it.startsWith(prefix) }?.let(::add)
                }
            }
            keys.forEach { localStorage.removeItem(it) }
        }
    }
}
