package com.orioooneee.lmuasister.data.cache

import platform.Foundation.NSUserDefaults

actual object LocalCache {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun read(key: String): String? = defaults.stringForKey(key)

    actual fun write(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    actual fun removeByPrefix(prefix: String) {
        defaults.dictionaryRepresentation().keys
            .mapNotNull { it as? String }
            .filter { it.startsWith(prefix) }
            .forEach { defaults.removeObjectForKey(it) }
    }
}
