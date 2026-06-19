package com.orioooneee.lmuasister.data.steam

import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

/** Plain-JSON file under the user's home — keeps the session across runs. */
internal class FileSteamSessionStore(private val file: File) : SteamSessionStore {
    override fun save(tokens: SteamTokens) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(tokens))
        }
    }

    override fun load(): SteamTokens? =
        runCatching { json.decodeFromString<SteamTokens>(file.readText()) }.getOrNull()

    override fun clear() {
        runCatching { file.delete() }
    }
}

actual fun steamSessionStore(): SteamSessionStore {
    val home = System.getProperty("user.home") ?: "."
    return FileSteamSessionStore(File(home, ".lmuassister/steam_session.json"))
}
