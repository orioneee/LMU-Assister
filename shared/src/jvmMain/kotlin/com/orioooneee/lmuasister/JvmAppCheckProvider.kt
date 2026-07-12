package com.orioooneee.lmuasister

import com.orioooneee.lmuasister.config.JvmBuildConfig
import com.orioooneee.lmuasister.di.AppCheckProvider

class JvmAppCheckProvider: AppCheckProvider {
    override suspend fun provideToken(): String {
        return JvmBuildConfig.DEBUG_APP_CHECK
    }
}