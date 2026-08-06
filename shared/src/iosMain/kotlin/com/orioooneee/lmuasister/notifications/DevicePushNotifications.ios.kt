package com.orioooneee.lmuasister.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.orioooneee.lmuasister.IosFcmTokenRegistrar
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@Composable
actual fun rememberDevicePushNotificationsController(): DevicePushNotificationsController {
    val pushConfigured = IosFcmTokenRegistrar.pushNotificationsConfigured
    val controller = remember { IosDevicePushNotificationsController() }
    LaunchedEffect(controller, pushConfigured) {
        controller.refresh()
    }
    return controller
}

private class IosDevicePushNotificationsController : DevicePushNotificationsController {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override var state by mutableStateOf(DevicePushPermissionState.Unavailable)
        private set

    override val unavailableMessage: String?
        get() = when (state) {
            DevicePushPermissionState.Unavailable ->
                "Push notifications require a paid Apple Developer Program team."
            DevicePushPermissionState.Denied ->
                "Enable notification permission in iOS Settings to use device push."
            DevicePushPermissionState.Granted -> null
        }

    override val deviceId: String?
        get() = if (IosFcmTokenRegistrar.pushNotificationsConfigured) {
            IosFcmTokenRegistrar.deviceId
        } else {
            null
        }

    override suspend fun requestPermission(): DevicePushPermissionState {
        if (!IosFcmTokenRegistrar.pushNotificationsConfigured) {
            state = DevicePushPermissionState.Unavailable
            return state
        }
        val currentState = currentPermissionState()
        if (currentState == DevicePushPermissionState.Granted) {
            state = currentState
            return currentState
        }

        val requestedState = suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
            center.requestAuthorizationWithOptions(options) { granted, _ ->
                if (continuation.isActive) {
                    continuation.resume(
                        if (granted) DevicePushPermissionState.Granted else DevicePushPermissionState.Denied,
                    )
                }
            }
        }
        state = requestedState
        return requestedState
    }

    suspend fun refresh() {
        state = if (IosFcmTokenRegistrar.pushNotificationsConfigured) {
            currentPermissionState()
        } else {
            DevicePushPermissionState.Unavailable
        }
    }

    private suspend fun currentPermissionState(): DevicePushPermissionState =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                if (continuation.isActive) {
                    continuation.resume(
                        settings?.toPermissionState() ?: DevicePushPermissionState.Denied,
                    )
                }
            }
        }
}

private fun UNNotificationSettings.toPermissionState(): DevicePushPermissionState =
    when (authorizationStatus) {
        UNAuthorizationStatusAuthorized,
        UNAuthorizationStatusProvisional,
        UNAuthorizationStatusEphemeral -> DevicePushPermissionState.Granted
        else -> DevicePushPermissionState.Denied
    }
