package com.orioooneee.lmuasister.data.steam

import org.koin.core.module.Module

/** Binds [SteamSignIn] — the device-tunnel impl on Android/JVM/iOS, a stub on js/wasm. */
expect fun steamModule(): Module
