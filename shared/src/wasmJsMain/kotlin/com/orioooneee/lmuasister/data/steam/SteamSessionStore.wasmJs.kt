package com.orioooneee.lmuasister.data.steam

actual fun steamSessionStore(): SteamSessionStore = InMemorySteamSessionStore()
