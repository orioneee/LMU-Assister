package com.orioooneee.lmuasister.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.orioooneee.lmuasister.FcmTokenRegistrar
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberDevicePushNotificationsController(): DevicePushNotificationsController {
    val context = LocalContext.current.applicationContext
    var controllerRef by remember { mutableStateOf<AndroidDevicePushNotificationsController?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        controllerRef?.onPermissionResult(granted)
    }
    val controller = remember(context, launcher) {
        AndroidDevicePushNotificationsController(context, launcher)
    }
    SideEffect {
        controllerRef = controller
        controller.refresh()
    }
    return controller
}

private class AndroidDevicePushNotificationsController(
    private val context: Context,
    private val launcher: ActivityResultLauncher<String>,
) : DevicePushNotificationsController {
    private var pendingRequest: CancellableContinuation<DevicePushPermissionState>? = null

    override var state by mutableStateOf(resolveState())
        private set

    override val unavailableMessage: String?
        get() = if (state == DevicePushPermissionState.Denied) {
            "Enable notification permission in Android settings to use device push."
        } else {
            null
        }

    override val deviceId: String
        get() = FcmTokenRegistrar.deviceId(context)

    override suspend fun requestPermission(): DevicePushPermissionState {
        refresh()
        if (state == DevicePushPermissionState.Granted) return state
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return DevicePushPermissionState.Granted

        pendingRequest?.cancel()
        return suspendCancellableCoroutine { continuation ->
            pendingRequest = continuation
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            continuation.invokeOnCancellation {
                if (pendingRequest === continuation) pendingRequest = null
            }
        }
    }

    fun refresh() {
        state = resolveState()
    }

    fun onPermissionResult(granted: Boolean) {
        state = if (granted) DevicePushPermissionState.Granted else DevicePushPermissionState.Denied
        pendingRequest?.resume(state)
        pendingRequest = null
    }

    private fun resolveState(): DevicePushPermissionState =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            DevicePushPermissionState.Granted
        } else {
            DevicePushPermissionState.Denied
        }
}
