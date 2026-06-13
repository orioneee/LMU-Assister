package com.orioooneee.lmuasister.data.cache

import platform.Foundation.NSUserDefaults

actual object LocalCache {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun read(key: String): String? = defaults.stringForKey(key)

    actual fun write(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
