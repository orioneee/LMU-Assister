package com.orioooneee.lmuasister.data.steam

/**
 * Persists the Steam session so the user doesn't have to log in every launch.
 * Holds only tokens — never the password.
 *
 * Storage is platform-specific: encrypted on Android (EncryptedSharedPreferences),
 * a file in the user's home on the JVM, in-memory elsewhere.
 */
interface SteamSessionStore {
    fun save(tokens: SteamTokens)
    fun load(): SteamTokens?
    fun clear()
}

internal class InMemorySteamSessionStore : SteamSessionStore {
    private var tokens: SteamTokens? = null
    override fun save(tokens: SteamTokens) { this.tokens = tokens }
    override fun load(): SteamTokens? = tokens
    override fun clear() { tokens = null }
}

expect fun steamSessionStore(): SteamSessionStore
