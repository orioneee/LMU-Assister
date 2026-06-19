package com.orioooneee.lmuasister.data.steam

import org.koin.dsl.module

actual fun steamModule() = module {
    single<SteamSessionStore> { steamSessionStore() }
    single<SteamSignIn> { UnsupportedSteamSignIn() }
}
