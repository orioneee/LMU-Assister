package com.orioooneee.lmuasister.data.steam

import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import org.koin.dsl.module

internal fun javaSteamModule() = module {
    single<SteamAuthClient> { JavaSteamAuthClient() }
    single<SteamSessionStore> { steamSessionStore() }
    single { JavaSteamSignIn(get(), get<SteamBackendApi>(), get()) }
    single<SteamSignIn> { get<JavaSteamSignIn>() }
    single<SteamAchievementsClient> { get<JavaSteamSignIn>() }
}
