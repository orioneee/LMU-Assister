package com.orioooneee.lmuasister

import com.orioooneee.lmuasister.di.AppCheckProvider

class WebAppCheckProvider : AppCheckProvider {
    override suspend fun provideToken(): String? = null
}
