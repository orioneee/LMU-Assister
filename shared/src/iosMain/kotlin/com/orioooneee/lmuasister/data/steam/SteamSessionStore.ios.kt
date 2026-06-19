package com.orioooneee.lmuasister.data.steam

// Steam auth runs on the backend for iOS, so no on-device session is persisted here.
actual fun steamSessionStore(): SteamSessionStore = InMemorySteamSessionStore()
