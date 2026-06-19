package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import org.koin.dsl.module

// Android keeps on-device JavaSteam (ready to switch to the tunnel later if needed).
actual fun steamModule() = module {
    single<SteamAuthClient> { JavaSteamAuthClient() }
    single<SteamSessionStore> { steamSessionStore() }
    single<SteamSignIn> { AndroidSteamSignIn(get(), get<SteamBackendApi>(), get()) }
}
