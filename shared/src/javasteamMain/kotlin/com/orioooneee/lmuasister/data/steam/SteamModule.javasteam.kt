package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import org.koin.dsl.module

// Android + desktop JVM: on-device JavaSteam (SteamKit2). No device tunnel.
actual fun steamModule() = module {
    single<SteamAuthClient> { JavaSteamAuthClient() }
    single<SteamSessionStore> { steamSessionStore() }
    single<SteamSignIn> { JavaSteamSignIn(get(), get<SteamBackendApi>(), get()) }
}
