package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import org.koin.dsl.module

// JVM uses the device-tunnel mechanism (sidecar logs into Steam through this machine's IP).
actual fun steamModule() = module {
    single<SteamSessionStore> { steamSessionStore() }
    single<SteamSignIn> { TunnelSteamSignIn(get<SteamBackendApi>(), get()) }
}
