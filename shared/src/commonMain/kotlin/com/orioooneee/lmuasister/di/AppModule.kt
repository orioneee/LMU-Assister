package com.orioooneee.lmuasister.di

import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.LmuCardImageApi
import com.orioooneee.lmuasister.data.remote.LmuPortalApi
import com.orioooneee.lmuasister.ui.ScheduleViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Wires the 3-source pipeline:
 *   HttpClient
 *     ├─ LmuScheduleApi   (schedule)
 *     ├─ LmuPortalApi     (track + class enrichment)
 *     └─ LmuCardImageApi  (cover images)
 *           → RaceRepository → ScheduleViewModel
 *
 * HttpClient uses the platform's default engine (OkHttp/CIO/Darwin/Js).
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
    single { LmuPortalApi(get()) }
    single { LmuCardImageApi(get()) }
    single { RaceRepository(get(), get()) }
    viewModelOf(::ScheduleViewModel)
}
