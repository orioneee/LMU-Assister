package com.orioooneee.lmuasister.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Holds the signed-in app token so token-optional calls (e.g. the leaderboard's "your
 * position") can use it without depending on the profile ViewModel. The profile VM is the
 * single writer; it sets the token as soon as the session is restored at app launch.
 */
class AppTokenHolder {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    fun set(value: String?) {
        _token.value = value
    }

    /** Current token, or wait up to [timeoutMs] for one to arrive — null if none by then. */
    suspend fun await(timeoutMs: Long): String? =
        _token.value ?: withTimeoutOrNull(timeoutMs) { token.filterNotNull().first() }
}
