@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.orioooneee.lmuasister.data.steam

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private val json = Json { ignoreUnknownKeys = true }
private const val SERVICE = "com.orioooneee.lmuasister.steam"
private const val ACCOUNT = "tokens"

// Steam login still runs through the backend tunnel on iOS, but the device-held refresh
// token must survive relaunches for silent restore/reauth — so persist it in the Keychain
// (encrypted at rest by iOS, AfterFirstUnlock + ThisDeviceOnly: no iCloud/backup sync).
internal class IosKeychainSessionStore : SteamSessionStore {

    override fun save(tokens: SteamTokens) {
        clear()
        val data = json.encodeToString(tokens).toNSData()
        val value = CFBridgingRetain(data)
        val query = baseQuery {
            CFDictionaryAddValue(it, kSecValueData, value)
            CFDictionaryAddValue(it, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }
        SecItemAdd(query, null)
        CFRelease(query)
        CFBridgingRelease(value)
    }

    override fun load(): SteamTokens? = memScoped {
        val out = alloc<CFTypeRefVar>()
        val query = baseQuery {
            CFDictionaryAddValue(it, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(it, kSecMatchLimit, kSecMatchLimitOne)
        }
        val status = SecItemCopyMatching(query, out.ptr)
        CFRelease(query)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(out.value) as? NSData ?: return@memScoped null
        runCatching { json.decodeFromString<SteamTokens>(data.toKString()) }.getOrNull()
    }

    override fun clear() {
        val query = baseQuery { }
        SecItemDelete(query)
        CFRelease(query)
    }
}

/** A generic-password query for our single item; [extra] adds operation-specific keys. */
private inline fun baseQuery(extra: (CFMutableDictionaryRef?) -> Unit): CFDictionaryRef {
    val dict = CFDictionaryCreateMutable(null, 0, null, null)
    CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
    CFDictionaryAddValue(dict, kSecAttrService, serviceRef)
    CFDictionaryAddValue(dict, kSecAttrAccount, accountRef)
    extra(dict)
    return dict!!
}

// Bridged once; these CFStrings live for the whole process (never released).
private val serviceRef: CFTypeRef? = CFBridgingRetain(SERVICE)
private val accountRef: CFTypeRef? = CFBridgingRetain(ACCOUNT)

private fun String.toNSData(): NSData {
    val bytes = encodeToByteArray()
    if (bytes.isEmpty()) return NSData()
    return bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }
}

private fun NSData.toKString(): String {
    val len = length.toInt()
    if (len == 0) return ""
    val out = ByteArray(len)
    out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out.decodeToString()
}

actual fun steamSessionStore(): SteamSessionStore = IosKeychainSessionStore()
