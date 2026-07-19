package com.orioooneee.lmuasister

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.messaging.FirebaseMessaging
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.BackendApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.UUID
import androidx.core.content.edit

@Composable
fun FcmTokenStartupEffect() {
    val context = LocalContext.current.applicationContext
    val repository = koinInject<RaceRepository>()

    LaunchedEffect(repository) {
        FcmTokenRegistrar.bind(repository)
        FcmTokenRegistrar.syncCurrentToken(context)
    }
}

object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"
    private const val PREFS_NAME = "lmu_fcm_token"
    private const val UUID_KEY = "uuid"
    private const val TOKEN_KEY = "token"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var repository: RaceRepository? = null

    fun bind(repository: RaceRepository) {
        this.repository = repository
    }

    fun deviceId(context: Context): String =
        getOrCreateUuid(context.applicationContext)

    fun syncCurrentToken(context: Context) {
        val appContext = context.applicationContext
        savedToken(appContext)?.let { token ->
            registerIfPossible(appContext, token)
        }

        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Telemetry.log(AnalyticsEvent.FcmTokenResult(stage = "fetch", success = true))
                persistAndRegisterIfPossible(appContext, token)
            }
            .addOnFailureListener { error ->
                Telemetry.log(
                    AnalyticsEvent.FcmTokenResult(
                        stage = "fetch",
                        success = false,
                        reason = error.fcmTokenAnalyticsReason(),
                    ),
                )
                Log.w(TAG, "Couldn't get FCM token", error)
            }
    }

    fun persistAndRegisterIfPossible(context: Context, token: String) {
        if (token.isBlank()) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        getOrCreateUuid(appContext)
        prefs.edit { putString(TOKEN_KEY, token) }
        registerIfPossible(appContext, token)
    }

    private fun registerIfPossible(context: Context, token: String) {
        val activeRepository = repository ?: return
        val uuid = getOrCreateUuid(context.applicationContext)
        scope.launch {
            activeRepository.registerFcmToken(uuid, token)
                .onSuccess {
                    Telemetry.log(AnalyticsEvent.FcmTokenResult(stage = "register", success = true))
                }
                .onFailure { error ->
                    Telemetry.log(
                        AnalyticsEvent.FcmTokenResult(
                            stage = "register",
                            success = false,
                            reason = error.fcmTokenAnalyticsReason(),
                        ),
                    )
                    Log.w(TAG, "Couldn't register FCM token", error)
                }
        }
    }

    private fun savedToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, null)
            ?.takeIf { it.isNotBlank() }

    private fun getOrCreateUuid(context: Context): String = synchronized(this) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(UUID_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { return@synchronized it }

        val uuid = UUID.randomUUID().toString()
        prefs.edit { putString(UUID_KEY, uuid) }
        uuid
    }
}

private fun Throwable.fcmTokenAnalyticsReason(): String = when (this) {
    is BackendApiException -> code
    else -> message?.takeIf { it.isNotBlank() }?.let { "client_error" } ?: "unknown"
}
