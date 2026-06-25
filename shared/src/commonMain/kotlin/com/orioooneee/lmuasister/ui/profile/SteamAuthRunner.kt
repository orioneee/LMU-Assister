package com.orioooneee.lmuasister.ui.profile

import com.orioooneee.lmuasister.data.steam.SignInOutcome
import com.orioooneee.lmuasister.data.steam.SteamSignIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SteamAuthRunnerState {
    data object Idle : SteamAuthRunnerState
    data class Running(val stage: String) : SteamAuthRunnerState
    data class Finished(
        val id: Long,
        val stage: String,
        val outcome: SignInOutcome,
    ) : SteamAuthRunnerState
}

/**
 * Runs the long Steam auth operation outside a ViewModel lifecycle. The latest result is
 * kept in StateFlow so a recreated profile ViewModel can pick it up after returning from
 * the Steam Guard app.
 */
class SteamAuthRunner(
    private val steamSignIn: SteamSignIn,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<SteamAuthRunnerState>(SteamAuthRunnerState.Idle)
    val state: StateFlow<SteamAuthRunnerState> = _state.asStateFlow()

    private var currentJob: Job? = null
    private var nextId = 1L

    fun startSignIn(username: String, password: String, guardCode: String?): Boolean =
        run("login") { steamSignIn.signIn(username, password, guardCode) }

    fun continueDeviceConfirmation(challengeId: String): Boolean =
        run("login_continue") { steamSignIn.continueDeviceConfirmation(challengeId) }

    fun resetFinished(id: Long) {
        if ((_state.value as? SteamAuthRunnerState.Finished)?.id == id) {
            _state.value = SteamAuthRunnerState.Idle
        }
    }

    fun cancelRunning() {
        currentJob?.cancel()
        currentJob = null
        _state.value = SteamAuthRunnerState.Idle
    }

    private fun run(stage: String, block: suspend () -> SignInOutcome): Boolean {
        if (currentJob?.isActive == true) return false
        _state.value = SteamAuthRunnerState.Running(stage)
        currentJob = scope.launch {
            val outcome = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                SignInOutcome.Failure(t.message ?: t::class.simpleName ?: "Steam login failed")
            }
            _state.value = SteamAuthRunnerState.Finished(nextId++, stage, outcome)
            currentJob = null
        }
        return true
    }
}
