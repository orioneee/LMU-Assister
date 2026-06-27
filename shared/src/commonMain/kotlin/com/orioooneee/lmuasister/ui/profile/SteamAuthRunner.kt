package com.orioooneee.lmuasister.ui.profile

import com.orioooneee.lmuasister.data.steam.SignInOutcome
import com.orioooneee.lmuasister.data.steam.SteamLog
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
    private var activeRunId = 0L
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

    fun cancelRunning(reason: String) {
        SteamLog.d("runner: cancel requested reason=$reason active=${currentJob?.isActive == true}")
        activeRunId++
        currentJob?.cancel()
        currentJob = null
        _state.value = SteamAuthRunnerState.Idle
    }

    private fun run(stage: String, block: suspend () -> SignInOutcome): Boolean {
        if (currentJob?.isActive == true) {
            SteamLog.d("runner: refusing stage=$stage because another auth job is active")
            return false
        }
        val runId = ++activeRunId
        SteamLog.d("runner: start stage=$stage")
        _state.value = SteamAuthRunnerState.Running(stage)
        currentJob = scope.launch {
            try {
                val outcome = try {
                    block()
                } catch (e: CancellationException) {
                    val requestedByApp = activeRunId != runId
                    SteamLog.d(
                        "runner: cancellation stage=$stage requestedByApp=$requestedByApp " +
                            "type=${e::class.simpleName} message=${e.message.orEmpty()}",
                    )
                    if (requestedByApp) return@launch
                    SignInOutcome.Failure(e.message ?: e::class.simpleName ?: "Steam login was interrupted")
                } catch (t: Throwable) {
                    SteamLog.e("runner: failed stage=$stage", t)
                    SignInOutcome.Failure(t.message ?: t::class.simpleName ?: "Steam login failed")
                }
                if (activeRunId == runId) {
                    currentJob = null
                    SteamLog.d("runner: finished stage=$stage outcome=${outcome.describeForLog()}")
                    _state.value = SteamAuthRunnerState.Finished(nextId++, stage, outcome)
                } else {
                    SteamLog.d("runner: ignored stale finish stage=$stage outcome=${outcome.describeForLog()}")
                }
            } finally {
                if (activeRunId == runId) {
                    currentJob = null
                }
            }
        }
        return true
    }

    private fun SignInOutcome.describeForLog(): String =
        when (this) {
            is SignInOutcome.Success -> "Success(uid=$uid)"
            is SignInOutcome.GuardRequired -> "GuardRequired(kind=$kind)"
            is SignInOutcome.DeviceConfirmationPending ->
                "DeviceConfirmationPending(challenge=${SteamLog.short(challengeId)}, expiresIn=$expiresIn)"
            is SignInOutcome.Failure -> "Failure(reason=$reason)"
            /*
             * TUNNEL_DISABLED:
             * SignInOutcome.TunnelRequired is no longer part of the active SteamSignIn
             * contract. The old log label is kept here for easy restoration.
             *
             * SignInOutcome.TunnelRequired -> "TunnelRequired"
             */
        }
}
