package com.orioooneee.lmuasister

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.BackendApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

@Composable
fun IosFcmTokenStartupEffect() {
    val repository = koinInject<RaceRepository>()

    LaunchedEffect(repository) {
        IosFcmTokenRegistrar.bind(repository)
    }
}

/**
 * Swift/Firebase Messaging bridge for the iOS app.
 *
 * Firebase owns the APNs -> FCM token mapping in Swift. This object keeps the
 * stable app device id identical to Android's contract and registers every new
 * FCM token with the existing v3 backend endpoint.
 */
object IosFcmTokenRegistrar {
    private const val UUID_KEY = "lmu_fcm_uuid"
    private const val TOKEN_KEY = "lmu_fcm_token"

    private val defaults = NSUserDefaults.standardUserDefaults
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var repository: RaceRepository? = null

    var pushNotificationsConfigured by mutableStateOf(false)
        private set

    val deviceId: String
        get() = getOrCreateUuid()

    fun configurePushNotifications(enabled: Boolean) {
        pushNotificationsConfigured = enabled
    }

    fun bind(repository: RaceRepository) {
        this.repository = repository
        if (pushNotificationsConfigured) {
            savedToken()?.let(::registerIfPossible)
        }
    }

    fun updateFcmToken(token: String?) {
        if (!pushNotificationsConfigured) return
        val normalized = token?.trim()?.takeIf { it.isNotEmpty() } ?: return
        getOrCreateUuid()
        defaults.setObject(normalized, forKey = TOKEN_KEY)
        Telemetry.log(AnalyticsEvent.FcmTokenResult(stage = "fetch", success = true))
        registerIfPossible(normalized)
    }

    fun reportFcmTokenFailure(reason: String?) {
        Telemetry.log(
            AnalyticsEvent.FcmTokenResult(
                stage = "fetch",
                success = false,
                reason = reason?.takeIf { it.isNotBlank() } ?: "unknown",
            ),
        )
    }

    fun logNotificationReceived(notificationType: String, hasBody: Boolean) {
        Telemetry.log(
            AnalyticsEvent.PushNotificationReceived(
                notificationType = notificationType,
                hasNotification = true,
                hasBody = hasBody,
            ),
        )
    }

    fun logNotificationDisplayed(notificationType: String) {
        Telemetry.log(AnalyticsEvent.PushNotificationDisplayed(notificationType, "ios_system"))
    }

    fun logNotificationOpened(notificationType: String, notificationId: String?) {
        Telemetry.log(
            AnalyticsEvent.PushNotificationOpened(
                notificationType = notificationType,
                notificationId = notificationId?.toIntOrNull(),
            ),
        )
    }

    private fun registerIfPossible(token: String) {
        val activeRepository = repository ?: return
        val uuid = getOrCreateUuid()
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
                }
        }
    }

    private fun savedToken(): String? =
        defaults.stringForKey(TOKEN_KEY)?.takeIf { it.isNotBlank() }

    private fun getOrCreateUuid(): String {
        defaults.stringForKey(UUID_KEY)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val uuid = NSUUID().UUIDString()
        defaults.setObject(uuid, forKey = UUID_KEY)
        return uuid
    }
}

private fun Throwable.fcmTokenAnalyticsReason(): String = when (this) {
    is BackendApiException -> code
    else -> message?.takeIf { it.isNotBlank() }?.let { "client_error" } ?: "unknown"
}
