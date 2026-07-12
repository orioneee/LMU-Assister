package com.orioooneee.lmuasister

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.orioooneee.lmuasister.di.AppCheckProvider
import kotlinx.coroutines.tasks.await

class AndroidAppCheckProvider: AppCheckProvider {
    override suspend fun provideToken(): String? {
        return runCatching {
            Firebase.appCheck.getAppCheckToken(false).await().token
        }.getOrNull()
    }
}