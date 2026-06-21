package com.orioooneee.lmuasister.data.steam

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json

internal object AndroidAppContext {
    @Volatile
    var value: Context? = null
}

/** Call once from the Android entry point (e.g. MainActivity.onCreate). */
fun initSteamStorage(context: Context) {
    AndroidAppContext.value = context.applicationContext
}

private val json = Json { ignoreUnknownKeys = true }
private const val PREFS = "steam_session"
private const val KEY = "tokens"

internal class AndroidSteamSessionStore(private val prefs: SharedPreferences) : SteamSessionStore {
    override fun save(tokens: SteamTokens) {
        prefs.edit().putString(KEY, json.encodeToString(tokens)).apply()
    }

    override fun load(): SteamTokens? =
        prefs.getString(KEY, null)?.let { runCatching { json.decodeFromString<SteamTokens>(it) }.getOrNull() }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }
}

// androidx.security-crypto (MasterKey / EncryptedSharedPreferences) is deprecated by
// Google with no drop-in replacement; it still works and is the right tool here.
@Suppress("DEPRECATION")
actual fun steamSessionStore(): SteamSessionStore {
    val ctx = AndroidAppContext.value ?: return InMemorySteamSessionStore()
    val prefs = runCatching {
        val masterKey = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        // Keystore hiccup — fall back to plain prefs (security isn't critical here).
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    return AndroidSteamSessionStore(prefs)
}
