package com.orioooneee.lmuasister.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDevicePushNotificationsController(): DevicePushNotificationsController =
    remember { UnavailableDevicePushNotificationsController() }
