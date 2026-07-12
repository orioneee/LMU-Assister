package com.orioooneee.lmuasister.di

import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.mock.mockHttpClient
import com.orioooneee.lmuasister.data.remote.AppTokenHolder
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.SteamBackendApi
import com.orioooneee.lmuasister.data.remote.appTokenAuthPlugin
import com.orioooneee.lmuasister.analytics.installPerformanceMonitoring
import com.orioooneee.lmuasister.featureflags.FeatureFlagsRepository
import com.orioooneee.lmuasister.featureflags.platformFeatureFlagRemoteSource
import com.orioooneee.lmuasister.ui.ScheduleViewModel
import com.orioooneee.lmuasister.ui.profile.SteamAuthRunner
import com.orioooneee.lmuasister.ui.profile.SteamLoginViewModel
import com.orioooneee.lmuasister.ui.publicusers.PublicUsersViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect fun platformModules(): List<Module>

fun appCheckPlugin(
    provider: AppCheckProvider
) = createClientPlugin("AppCheckPlugin") {
    onRequest { request, _ ->
        println("Request X-Token")
        val token = provider.provideToken()
        println("Token: $token")
        request.headers.remove("X-Token")
        token?.let { request.headers.append("X-Token", it) }
    }
}
val appModule = module {
    single { AppTokenHolder() }
    single {
        val tokenHolder = get<AppTokenHolder>()
        val appCheckProvider: AppCheckProvider = get()
        // No real backend configured (or backend.mock=true) → serve bundled mock data.
        if (BuildConfig.USE_MOCK) mockHttpClient(tokenHolder)
        else HttpClient {
            install(appTokenAuthPlugin(tokenHolder))
            installPerformanceMonitoring()
            followRedirects = true
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
            }
            install(appCheckPlugin(appCheckProvider))
        }
    }
    single { BackendApi(get()) }
    single { SteamBackendApi(get()) }
    single { RaceRepository(get(), get()) }
    single { platformFeatureFlagRemoteSource() }
    single { FeatureFlagsRepository(get()) }
    single { SteamAuthRunner(get()) }
    viewModelOf(::ScheduleViewModel)
    viewModelOf(::PublicUsersViewModel)
    // SteamSignIn / SteamSessionStore are bound per-platform via steamModule().
    viewModelOf(::SteamLoginViewModel)
}
