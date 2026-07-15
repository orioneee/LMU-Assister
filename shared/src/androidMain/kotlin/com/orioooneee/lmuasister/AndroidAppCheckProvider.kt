package com.orioooneee.lmuasister

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.orioooneee.lmuasister.di.AppCheckProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class AndroidAppCheckProvider: AppCheckProvider {
    private val mutex = Mutex()
    override suspend fun provideToken(): String? {
        return mutex.withLock {
            runCatching {
                Firebase.appCheck.getAppCheckToken(false).await().token
            }.getOrNull()
        }
    }
}