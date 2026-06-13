package com.orioooneee.lmuasister.di

import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.ui.ScheduleViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Wires the backend-backed pipeline:
 *   HttpClient → BackendApi → RaceRepository → ScheduleViewModel
 *
 * HttpClient uses the platform's default engine (OkHttp/CIO/Darwin/Js).
 * The backend base URL comes from BuildConfig.BACKEND_URL (local.properties).
 */
val appModule = module {
    single {
        HttpClient {
            followRedirects = true
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
            }
        }
    }
    single { BackendApi(get()) }
    single { RaceRepository(get()) }
    viewModelOf(::ScheduleViewModel)
}
