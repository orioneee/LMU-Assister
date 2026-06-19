package com.orioooneee.lmuasister.data.steam

import org.koin.core.module.Module

/** Binds [SteamAuthClient] — the real JavaSteam impl on Android, a stub elsewhere. */
expect fun steamModule(): Module
