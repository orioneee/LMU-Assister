package com.orioooneee.lmuasister.di

import com.orioooneee.lmuasister.WebAppCheckProvider
import com.orioooneee.lmuasister.security.SecurityGate
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> {
    SecurityGate.allow()
    return listOf(
        module {
            single<AppCheckProvider> {
                WebAppCheckProvider()
            }
        }
    )
}
