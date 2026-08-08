package com.orioooneee.lmuasister

import com.orioooneee.lmuasister.di.AppCheckProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IosAppCheckTokenSource {
    fun fetch(onComplete: (String?) -> Unit)
}

private object NoopIosAppCheckTokenSource : IosAppCheckTokenSource {
    override fun fetch(onComplete: (String?) -> Unit) = onComplete(null)
}

object IosAppCheck {
    var tokenSource: IosAppCheckTokenSource = NoopIosAppCheckTokenSource
}

class IosAppCheckProvider : AppCheckProvider {
    override suspend fun provideToken(): String? = suspendCancellableCoroutine { continuation ->
        runCatching {
            IosAppCheck.tokenSource.fetch { token ->
                if (continuation.isActive) {
                    continuation.resume(token?.takeIf(String::isNotBlank))
                }
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
