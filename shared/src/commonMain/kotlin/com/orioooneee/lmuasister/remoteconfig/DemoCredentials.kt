package com.orioooneee.lmuasister.remoteconfig

import kotlin.coroutines.resume
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine

enum class DemoCredentialKey(val remoteName: String) {
    Login("DEMO_LOGIN"),
    Password("DEMO_PASS"),
}

data class DemoCredentials(
    val login: String,
    val password: String,
) {
    fun matches(login: String, password: String): Boolean =
        this.login == login && this.password == password
}

interface DemoCredentialsRemoteSource {
    fun fetch(keys: List<DemoCredentialKey>, onComplete: (Map<String, String>) -> Unit)
}

class NoopDemoCredentialsRemoteSource : DemoCredentialsRemoteSource {
    override fun fetch(keys: List<DemoCredentialKey>, onComplete: (Map<String, String>) -> Unit) {
        onComplete(emptyMap())
    }
}

expect fun platformDemoCredentialsRemoteSource(): DemoCredentialsRemoteSource

class DemoCredentialsRepository(
    private val remoteSource: DemoCredentialsRemoteSource,
) {
    private val mutex = Mutex()
    private var cached: DemoCredentials? = null

    suspend fun get(): DemoCredentials? {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: remoteSource.fetchOnce().toDemoCredentials()
                ?.also { cached = it }
        }
    }
}

private suspend fun DemoCredentialsRemoteSource.fetchOnce(): Map<String, String> =
    suspendCancellableCoroutine { continuation ->
        runCatching {
            fetch(DemoCredentialKey.entries) { values ->
                if (continuation.isActive) continuation.resume(values)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(emptyMap())
        }
    }

private fun Map<String, String>.toDemoCredentials(): DemoCredentials? {
    val login = get(DemoCredentialKey.Login.remoteName)?.trim().orEmpty()
    val password = get(DemoCredentialKey.Password.remoteName).orEmpty()
    return DemoCredentials(login, password).takeIf { it.login.isNotBlank() && it.password.isNotBlank() }
}
