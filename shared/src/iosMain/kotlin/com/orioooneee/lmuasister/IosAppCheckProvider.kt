package com.orioooneee.lmuasister

import com.orioooneee.lmuasister.config.IosBuildConfig
import com.orioooneee.lmuasister.di.AppCheckProvider

class IosAppCheckProvider : AppCheckProvider {
    override suspend fun provideToken(): String? {
        return IosBuildConfig.DEBUG_APP_CHECK.takeIf { it.isNotBlank() }
    }
}
