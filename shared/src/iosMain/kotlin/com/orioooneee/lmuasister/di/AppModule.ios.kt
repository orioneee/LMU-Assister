package com.orioooneee.lmuasister.di

import com.orioooneee.lmuasister.IosAppCheckProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> {
    return listOf(
        module {
            single<AppCheckProvider> {
                IosAppCheckProvider()
            }
        }
    )
}
