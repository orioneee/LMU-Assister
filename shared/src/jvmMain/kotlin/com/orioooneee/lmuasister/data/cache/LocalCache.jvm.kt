package com.orioooneee.lmuasister.data.cache

import java.io.File

actual object LocalCache {
    private val dir = File(System.getProperty("user.home") ?: ".", ".lmuassister").apply { runCatching { mkdirs() } }
    private fun file(key: String) = File(dir, "$key.json")

    actual fun read(key: String): String? =
        file(key).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    actual fun write(key: String, value: String) {
        runCatching { file(key).writeText(value) }
    }

    actual fun remove(key: String) {
        runCatching { file(key).delete() }
    }

    actual fun removeByPrefix(prefix: String) {
        runCatching {
            dir.listFiles()
                ?.filter { it.isFile && it.name.removeSuffix(".json").startsWith(prefix) }
                ?.forEach { it.delete() }
        }
    }
}
