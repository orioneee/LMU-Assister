package com.orioooneee.lmuasister.data.steam

import org.koin.core.module.Module

/** Binds [SteamSignIn] — kSteam on-device auth on Android/iOS. */
expect fun steamModule(): Module
