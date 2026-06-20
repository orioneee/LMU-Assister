package com.orioooneee.lmuasister.data.steam

import org.koin.core.module.Module

/** Binds [SteamSignIn] — on-device JavaSteam on Android/JVM, the device tunnel on iOS. */
expect fun steamModule(): Module
