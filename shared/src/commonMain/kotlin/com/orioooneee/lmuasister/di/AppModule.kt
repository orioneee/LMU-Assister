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
import com.orioooneee.lmuasister.data.remote.ApiBaseUrlProvider
import com.orioooneee.lmuasister.security.SecurityGate
import com.orioooneee.lmuasister.security.securityGatePlugin
import com.orioooneee.lmuasister.ui.ScheduleViewModel
import com.orioooneee.lmuasister.ui.profile.SteamAuthRunner
import com.orioooneee.lmuasister.ui.profile.SteamLoginViewModel
import com.orioooneee.lmuasister.ui.publicusers.PublicUsersViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

expect fun platformModules(): List<Module>

fun appCheckPlugin(
    provider: AppCheckProvider
) = createClientPlugin("AppCheckPlugin") {
    onRequest { request, _ ->
        val start = Clock.System.now()
        val token = provider.provideToken()
        println("Total fetch token took: ${Clock.System.now().minus(start).inWholeMilliseconds} ms")
        request.headers.remove("X-Token")
        token?.let { request.headers.append("X-Token", it) }
    }
}
val appModule = module {
    single {
        val appCheckProvider = get<AppCheckProvider>()
        ApiBaseUrlProvider(
            appCheckTokenProvider = { appCheckProvider.provideToken() },
            awaitNetworkAllowed = { SecurityGate.awaitAllowed() },
            fixedBaseUrl = if (BuildConfig.USE_MOCK) "https://mock.local/api/v3" else null,
        ).also { provider ->
            if (!BuildConfig.USE_MOCK) provider.warmUp()
        }
    }
    single { AppTokenHolder() }
    single {
        val tokenHolder = get<AppTokenHolder>()
        val appCheckProvider: AppCheckProvider = get()
        val apiBaseUrlProvider = get<ApiBaseUrlProvider>()
        // No real backend configured (or backend.mock=true) → serve bundled mock data.
        if (BuildConfig.USE_MOCK) mockHttpClient()
        else HttpClient {
            install(securityGatePlugin())
            install(appTokenAuthPlugin(tokenHolder, apiBaseUrlProvider))
            installPerformanceMonitoring()
            followRedirects = true
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
            }
            install(appCheckPlugin(appCheckProvider))
        }
    }
    single { BackendApi(get(), get()) }
    single { SteamBackendApi(get(), get()) }
    single { RaceRepository(get(), get()) }
    single { platformFeatureFlagRemoteSource() }
    single { FeatureFlagsRepository(get()) }
    single { SteamAuthRunner(get()) }
    viewModelOf(::ScheduleViewModel)
    viewModelOf(::PublicUsersViewModel)
    // SteamSignIn / SteamSessionStore are bound per-platform via steamModule().
    viewModelOf(::SteamLoginViewModel)
}
