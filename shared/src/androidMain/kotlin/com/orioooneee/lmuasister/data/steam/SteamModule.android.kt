package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import org.koin.dsl.module

// Android uses the device-tunnel mechanism (sidecar logs into Steam through the phone's
// IP) — same path as JVM/iOS. No on-device Steam client.
actual fun steamModule() = module {
    single<SteamSessionStore> { steamSessionStore() }
    single<SteamSignIn> { TunnelSteamSignIn(get<SteamBackendApi>(), get()) }
}
