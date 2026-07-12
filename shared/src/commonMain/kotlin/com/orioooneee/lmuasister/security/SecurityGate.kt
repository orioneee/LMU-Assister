package com.orioooneee.lmuasister.security

import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

enum class SecurityGateState {
    Starting,
    Allowed,
    Blocked,
}

class SecurityBlockedException : CancellationException("security_blocked")

object SecurityGate {
    private val state = MutableStateFlow(SecurityGateState.Starting)

    val currentState: SecurityGateState
        get() = state.value

    fun resetForChecks() {
        state.value = SecurityGateState.Starting
    }

    fun allow() {
        if (state.value == SecurityGateState.Starting) {
            state.value = SecurityGateState.Allowed
        }
    }

    fun block() {
        state.value = SecurityGateState.Blocked
    }

    suspend fun awaitAllowed() {
        when (state.value) {
            SecurityGateState.Allowed -> return
            SecurityGateState.Blocked -> throw SecurityBlockedException()
            SecurityGateState.Starting -> Unit
        }

        when (state.filter { it != SecurityGateState.Starting }.first()) {
            SecurityGateState.Allowed -> return
            SecurityGateState.Blocked -> throw SecurityBlockedException()
            SecurityGateState.Starting -> error("unreachable")
        }
    }
}

fun securityGatePlugin() = createClientPlugin("SecurityGate") {
    onRequest { _, _ ->
        SecurityGate.awaitAllowed()
    }
}
