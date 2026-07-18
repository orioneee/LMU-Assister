package com.orioooneee.lmuasister.notifications

import androidx.compose.runtime.Composable

enum class DevicePushPermissionState {
    Granted,
    Denied,
    Unavailable,
}

interface DevicePushNotificationsController {
    val state: DevicePushPermissionState
    val unavailableMessage: String?
    val deviceId: String?

    suspend fun requestPermission(): DevicePushPermissionState
}

class UnavailableDevicePushNotificationsController(
    override val unavailableMessage: String = "Device push notifications are not available on this platform yet.",
) : DevicePushNotificationsController {
    override val state: DevicePushPermissionState = DevicePushPermissionState.Unavailable
    override val deviceId: String? = null

    override suspend fun requestPermission(): DevicePushPermissionState =
        DevicePushPermissionState.Unavailable
}

@Composable
expect fun rememberDevicePushNotificationsController(): DevicePushNotificationsController
