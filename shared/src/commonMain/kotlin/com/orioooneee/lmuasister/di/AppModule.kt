package com.orioooneee.lmuasister.di

import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.mock.mockHttpClient
import com.orioooneee.lmuasister.data.remote.AppTokenHolder
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import com.orioooneee.lmuasister.ui.ScheduleViewModel
import com.orioooneee.lmuasister.ui.profile.SteamAuthRunner
import com.orioooneee.lmuasister.ui.profile.SteamLoginViewModel
import com.orioooneee.lmuasister.ui.publicusers.PublicUsersViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        // No real backend configured (or backend.mock=true) → serve bundled mock data.
        if (BuildConfig.USE_MOCK) mockHttpClient()
        else HttpClient {
            followRedirects = true
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
            }
        }
    }
    single { BackendApi(get()) }
    single { SteamBackendApi(get()) }
    single { AppTokenHolder() }
    single { RaceRepository(get(), get()) }
    single { SteamAuthRunner(get()) }
    viewModelOf(::ScheduleViewModel)
    viewModelOf(::PublicUsersViewModel)
    // SteamSignIn / SteamSessionStore are bound per-platform via steamModule().
    viewModelOf(::SteamLoginViewModel)
}
