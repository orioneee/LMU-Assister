package com.orioooneee.lmuasister.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.BackendApiException
import com.orioooneee.lmuasister.data.remote.SCHEDULE_UPDATE_TYPE_DEVICE_PUSH
import com.orioooneee.lmuasister.data.remote.SCHEDULE_UPDATE_TYPE_EMAIL
import com.orioooneee.lmuasister.notifications.DevicePushPermissionState
import com.orioooneee.lmuasister.notifications.rememberDevicePushNotificationsController
import com.orioooneee.lmuasister.ui.profile.SteamLoginUiState
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ScheduleUpdatesScreen(
    insets: PaddingValues,
    authState: SteamLoginUiState,
    onBack: () -> Unit,
) {
    val repository = koinInject<RaceRepository>()
    val appToken by repository.appToken.collectAsState()
    val emailAuthState = remember(authState, appToken) { scheduleUpdatesEmailAuthState(authState, appToken) }
    val devicePushController = rememberDevicePushNotificationsController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()
    val devicePermission = devicePushController.state
    val deviceId = devicePushController.deviceId
    val canUseDevicePush = devicePermission == DevicePushPermissionState.Granted && !deviceId.isNullOrBlank()
    val canRequestDevicePush = devicePermission != DevicePushPermissionState.Unavailable
    val canUseEmail = emailAuthState == ScheduleUpdatesEmailAuthState.Available
    val emailAuthLoading = emailAuthState == ScheduleUpdatesEmailAuthState.Pending

    var initialDevicePush by remember { mutableStateOf(false) }
    var initialEmail by remember { mutableStateOf(false) }
    var devicePushSelected by remember { mutableStateOf(false) }
    var emailSelected by remember { mutableStateOf(false) }
    var loadingSubscriptions by remember { mutableStateOf(false) }
    var requestingDevicePermission by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val includeEmail = emailAuthState == ScheduleUpdatesEmailAuthState.Available

    LaunchedEffect(deviceId, includeEmail, reloadKey) {
        if (deviceId.isNullOrBlank() && !includeEmail) {
            loadingSubscriptions = false
            errorMessage = null
            initialDevicePush = false
            devicePushSelected = false
            initialEmail = false
            emailSelected = false
            return@LaunchedEffect
        }

        loadingSubscriptions = true
        errorMessage = null
        repository.scheduleUpdateSubscriptions(deviceId, includeEmail)
            .onSuccess { subscriptions ->
                val pushSubscribed = deviceId?.let { id ->
                    subscriptions.any { it.type == SCHEDULE_UPDATE_TYPE_DEVICE_PUSH && it.target == id }
                } ?: false
                val emailSubscribed = subscriptions.any { it.type == SCHEDULE_UPDATE_TYPE_EMAIL }

                initialDevicePush = pushSubscribed
                devicePushSelected = pushSubscribed
                if (includeEmail) {
                    initialEmail = emailSubscribed
                    emailSelected = emailSubscribed
                }
                Telemetry.log(
                    AnalyticsEvent.ScheduleUpdatesLoaded(
                        devicePush = pushSubscribed,
                        email = emailSubscribed,
                    ),
                )
            }
            .onFailure {
                if (!includeEmail && it.isScheduleUpdatesAuthOnlyFailure()) {
                    initialEmail = false
                    emailSelected = false
                } else {
                    Telemetry.log(AnalyticsEvent.ScheduleUpdatesLoadFailed(it.scheduleUpdatesAnalyticsReason()))
                    errorMessage = "Couldn't load schedule update subscriptions: ${it.scheduleUpdatesMessage()}"
                }
            }
        loadingSubscriptions = false
    }

    LaunchedEffect(emailAuthState) {
        if (emailAuthState == ScheduleUpdatesEmailAuthState.SignedOut) {
            initialEmail = false
            emailSelected = false
        }
    }

    val deviceChanged = devicePushSelected != initialDevicePush
    val emailChanged = emailSelected != initialEmail
    val deviceChangeAllowed = !deviceChanged || !devicePushSelected || canUseDevicePush
    val emailChangeAllowed = !emailChanged || canUseEmail
    val saveEnabled = !loadingSubscriptions &&
        !requestingDevicePermission &&
        !saving &&
        (deviceChanged || emailChanged) &&
        deviceChangeAllowed &&
        emailChangeAllowed
    val devicePushEnabled = deviceId != null && (canUseDevicePush || canRequestDevicePush || devicePushSelected)
    val devicePushLoading = loadingSubscriptions || requestingDevicePermission
    val emailLoading = emailAuthLoading || (loadingSubscriptions && includeEmail)

    val onDevicePushClick: () -> Unit = {
        when {
            saving || loadingSubscriptions || requestingDevicePermission -> Unit
            devicePushSelected -> {
                Telemetry.log(AnalyticsEvent.ScheduleUpdatesChannelToggled("device_push", false))
                devicePushSelected = false
            }
            canUseDevicePush -> {
                Telemetry.log(AnalyticsEvent.ScheduleUpdatesChannelToggled("device_push", true))
                devicePushSelected = true
            }
            canRequestDevicePush -> scope.launch {
                requestingDevicePermission = true
                val state = devicePushController.requestPermission()
                requestingDevicePermission = false
                Telemetry.log(
                    AnalyticsEvent.NotificationPermissionResult(
                        source = "schedule_updates",
                        state = state.name.lowercase(),
                    ),
                )
                if (state == DevicePushPermissionState.Granted) {
                    Telemetry.log(AnalyticsEvent.ScheduleUpdatesChannelToggled("device_push", true))
                    devicePushSelected = true
                } else {
                    snackbarHostState.showSnackbar("Enable notification permission to use device push")
                }
            }
        }
    }

    val onSaveClick: () -> Unit = {
        val currentDeviceId = deviceId
        scope.launch {
            saving = true
            errorMessage = null
            Telemetry.log(
                AnalyticsEvent.ScheduleUpdatesSubmit(
                    deviceChanged = deviceChanged,
                    emailChanged = emailChanged,
                ),
            )

            val deviceResult = when {
                !deviceChanged -> Result.success(Unit)
                currentDeviceId.isNullOrBlank() -> Result.failure(IllegalStateException("device_id_required"))
                devicePushSelected -> repository.subscribeScheduleUpdatesDevicePush(currentDeviceId)
                else -> repository.unsubscribeScheduleUpdatesDevicePush(currentDeviceId)
            }
            val emailResult = when {
                !emailChanged -> Result.success(Unit)
                emailSelected -> repository.subscribeScheduleUpdatesEmail()
                else -> repository.unsubscribeScheduleUpdatesEmail()
            }

            saving = false
            if (deviceChanged) {
                Telemetry.log(
                    AnalyticsEvent.ScheduleUpdatesResult(
                        channel = "device_push",
                        subscribed = devicePushSelected,
                        success = deviceResult.isSuccess,
                        reason = deviceResult.exceptionOrNull().scheduleUpdatesAnalyticsReasonOrNull(),
                    ),
                )
            }
            if (emailChanged) {
                Telemetry.log(
                    AnalyticsEvent.ScheduleUpdatesResult(
                        channel = "email",
                        subscribed = emailSelected,
                        success = emailResult.isSuccess,
                        reason = emailResult.exceptionOrNull().scheduleUpdatesAnalyticsReasonOrNull(),
                    ),
                )
            }
            if (deviceResult.isSuccess && emailResult.isSuccess) {
                initialDevicePush = devicePushSelected
                initialEmail = emailSelected
                snackbarHostState.showSnackbar("Schedule update subscriptions saved")
            } else {
                val message = scheduleUpdatesSaveMessage(
                    deviceError = deviceResult.exceptionOrNull(),
                    emailError = emailResult.exceptionOrNull(),
                )
                errorMessage = message
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Carbon)) {
        val desktopLayout = maxWidth >= 900.dp
        val horizontalPadding = when {
            desktopLayout -> 32.dp
            maxWidth >= 600.dp -> 24.dp
            else -> 16.dp
        }
        val contentMaxWidth = when {
            desktopLayout -> 680.dp
            maxWidth >= 600.dp -> 620.dp
            else -> maxWidth
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = topInset + if (desktopLayout) 28.dp else 16.dp,
                end = horizontalPadding,
                bottom = bottomInset + if (desktopLayout) 32.dp else 24.dp,
            ),
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier.widthIn(max = contentMaxWidth).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (desktopLayout) 18.dp else 16.dp),
                    ) {
                        ScheduleUpdatesHeader(onBack)
                        ScheduleUpdatesIntro(emailAuthState = emailAuthState, errorMessage = errorMessage)
                        ScheduleUpdatesControls(
                            loadingSubscriptions = loadingSubscriptions,
                            devicePermission = devicePermission,
                            devicePushSelected = devicePushSelected,
                            devicePushEnabled = devicePushEnabled,
                            devicePushLoading = devicePushLoading,
                            onDevicePushClick = onDevicePushClick,
                            emailAuthState = emailAuthState,
                            emailSelected = emailSelected,
                            emailEnabled = canUseEmail,
                            emailLoading = emailLoading,
                            onEmailClick = {
                                if (canUseEmail && !saving && !loadingSubscriptions) {
                                    Telemetry.log(
                                        AnalyticsEvent.ScheduleUpdatesChannelToggled(
                                            channel = "email",
                                            enabled = !emailSelected,
                                        ),
                                    )
                                    emailSelected = !emailSelected
                                }
                            },
                            saving = saving,
                            saveEnabled = saveEnabled,
                            onRefresh = { reloadKey += 1 },
                            onSave = onSaveClick,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 12.dp),
        )
    }
}

@Composable
private fun ScheduleUpdatesHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface1)
                .border(1.dp, Outline, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHigh)
        }
        Text(
            "Schedule updates",
            style = MaterialTheme.typography.headlineMedium,
            color = TextHigh,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScheduleUpdatesIntro(
    emailAuthState: ScheduleUpdatesEmailAuthState,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Subscribe to schedule updates",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Get notified when the race schedule changes. Choose device push, email, or both.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMed,
            )
        }
        when (emailAuthState) {
            ScheduleUpdatesEmailAuthState.Pending -> ScheduleUpdatesStatus(
                title = "Checking email sign-in",
                message = "Device push is available now. Email will unlock if your session restores.",
                loading = true,
            )
            ScheduleUpdatesEmailAuthState.SignedOut -> ScheduleUpdatesStatus(
                title = "Email unavailable",
                message = "Device push is still available. Sign in from Profile to use email updates.",
            )
            ScheduleUpdatesEmailAuthState.Available -> Unit
        }
        errorMessage?.let { ScheduleUpdatesError(it) }
    }
}

@Composable
private fun ScheduleUpdatesControls(
    loadingSubscriptions: Boolean,
    devicePermission: DevicePushPermissionState,
    devicePushSelected: Boolean,
    devicePushEnabled: Boolean,
    devicePushLoading: Boolean,
    onDevicePushClick: () -> Unit,
    emailAuthState: ScheduleUpdatesEmailAuthState,
    emailSelected: Boolean,
    emailEnabled: Boolean,
    emailLoading: Boolean,
    onEmailClick: () -> Unit,
    saving: Boolean,
    saveEnabled: Boolean,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Notify via",
                style = MaterialTheme.typography.labelLarge,
                color = TextMed,
                fontWeight = FontWeight.SemiBold,
            )

            ScheduleUpdateMethodCard(
                title = "Device Push",
                message = devicePushMessage(
                    loading = loadingSubscriptions,
                    permission = devicePermission,
                    selected = devicePushSelected,
                ),
                icon = Icons.Filled.Notifications,
                checked = devicePushSelected,
                enabled = devicePushEnabled,
                loading = devicePushLoading,
                onClick = onDevicePushClick,
            )

            ScheduleUpdateMethodCard(
                title = "Email",
                message = emailMethodMessage(emailAuthState),
                icon = Icons.Filled.Email,
                checked = emailSelected,
                enabled = emailEnabled,
                loading = emailLoading,
                onClick = onEmailClick,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onRefresh,
                enabled = !loadingSubscriptions && !saving,
            ) {
                Text("Refresh")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSave,
                enabled = saveEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = TextHigh,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScheduleUpdatesStatus(
    title: String,
    message: String,
    loading: Boolean = false,
    error: Boolean = false,
) {
    val accent = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = if (error) 0.12f else 0.1f))
            .border(1.dp, accent.copy(alpha = if (error) 0.4f else 0.34f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = accent,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = TextMed,
            )
        }
    }
}

@Composable
private fun ScheduleUpdateMethodCard(
    title: String,
    message: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val selected = checked && enabled
    val shape = RoundedCornerShape(8.dp)
    val borderColor = when {
        selected -> accent.copy(alpha = 0.72f)
        loading -> accent.copy(alpha = 0.42f)
        enabled -> Outline
        else -> Outline.copy(alpha = 0.45f)
    }
    val bg = when {
        selected -> accent.copy(alpha = 0.14f)
        loading -> accent.copy(alpha = 0.08f)
        enabled -> Surface2
        else -> Surface2.copy(alpha = 0.58f)
    }
    val contentAlpha = if (enabled || loading) 1f else 0.58f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) accent.copy(alpha = 0.22f) else Carbon.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) accent else TextMed.copy(alpha = contentAlpha),
                modifier = Modifier.size(21.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextHigh.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = TextLow.copy(alpha = if (enabled || loading) 0.9f else 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = accent,
                strokeWidth = 2.dp,
            )
        } else {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = accent,
                    uncheckedColor = TextLow,
                    disabledCheckedColor = TextLow.copy(alpha = 0.32f),
                    disabledUncheckedColor = TextLow.copy(alpha = 0.32f),
                    checkmarkColor = Carbon,
                ),
            )
        }
    }
}

@Composable
private fun ScheduleUpdatesError(message: String) {
    val error = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(error.copy(alpha = 0.12f))
            .border(1.dp, error.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(error.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = error, modifier = Modifier.size(19.dp))
        }
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = TextMed,
            modifier = Modifier.weight(1f),
        )
    }
}

private enum class ScheduleUpdatesEmailAuthState {
    Pending,
    Available,
    SignedOut,
}

private fun scheduleUpdatesEmailAuthState(
    authState: SteamLoginUiState,
    appToken: String?,
): ScheduleUpdatesEmailAuthState = when {
    !appToken.isNullOrBlank() -> ScheduleUpdatesEmailAuthState.Available
    authState.isScheduleUpdatesEmailAuthPending() -> ScheduleUpdatesEmailAuthState.Pending
    else -> ScheduleUpdatesEmailAuthState.SignedOut
}

private fun SteamLoginUiState.isScheduleUpdatesEmailAuthPending(): Boolean = when (this) {
    SteamLoginUiState.Restoring,
    SteamLoginUiState.CheckingAuthEnvironment,
    SteamLoginUiState.RequestingLocalNetworkPermission,
    SteamLoginUiState.Loading,
    SteamLoginUiState.QrCodeStarting,
    is SteamLoginUiState.DeviceConfirmationPending,
    is SteamLoginUiState.QrCodePending,
    is SteamLoginUiState.SignedIn -> true
    SteamLoginUiState.Idle,
    is SteamLoginUiState.LocalNetworkPermissionRequired,
    is SteamLoginUiState.MinterUnavailable,
    is SteamLoginUiState.Error,
    is SteamLoginUiState.GuardRequired -> false
}

private fun devicePushMessage(
    loading: Boolean,
    permission: DevicePushPermissionState,
    selected: Boolean,
): String = when {
    loading -> "Checking this device..."
    selected -> "Subscribed on this device"
    permission == DevicePushPermissionState.Granted -> "Receive updates as push notifications"
    permission == DevicePushPermissionState.Denied -> "Tap to allow notification permission"
    else -> "Device push is not available on this platform yet"
}

private fun emailMethodMessage(authState: ScheduleUpdatesEmailAuthState): String = when (authState) {
    ScheduleUpdatesEmailAuthState.Available -> "Send updates to your account email"
    ScheduleUpdatesEmailAuthState.Pending -> "Waiting for email sign-in restore"
    ScheduleUpdatesEmailAuthState.SignedOut -> "Sign in to enable email updates"
}

private fun scheduleUpdatesSaveMessage(deviceError: Throwable?, emailError: Throwable?): String = when {
    deviceError == null && emailError == null -> "Schedule update subscriptions saved"
    deviceError != null && emailError != null ->
        "Couldn't save device push (${deviceError.scheduleUpdatesMessage()}) or email (${emailError.scheduleUpdatesMessage()})"
    deviceError != null -> "Couldn't save device push: ${deviceError.scheduleUpdatesMessage()}"
    emailError != null -> "Couldn't save email: ${emailError.scheduleUpdatesMessage()}"
    else -> "Nothing changed"
}

private fun Throwable.isScheduleUpdatesAuthOnlyFailure(): Boolean =
    this is BackendApiException && code in setOf("unauthorized", "user_not_found", "email_required")

private fun Throwable?.scheduleUpdatesAnalyticsReasonOrNull(): String? = this?.scheduleUpdatesAnalyticsReason()

private fun Throwable.scheduleUpdatesAnalyticsReason(): String = when (this) {
    is BackendApiException -> code
    else -> message?.takeIf { it.isNotBlank() }?.let { "client_error" } ?: "unknown"
}

private fun Throwable?.scheduleUpdatesMessage(): String = when (val error = this) {
    is BackendApiException -> when (error.code) {
        "bad_json" -> "request payload was rejected"
        "type_required" -> "subscription type is missing"
        "type_invalid" -> "subscription type is invalid"
        "target_required" -> "device id is missing"
        "target_invalid" -> "device id is invalid"
        "unauthorized" -> "sign in again and try once more"
        "user_not_found" -> "profile was not found"
        "email_required" -> "add an email to your account"
        "forbidden" -> "app check failed"
        else -> error.code
    }
    null -> "unknown error"
    else -> error.message?.takeIf { it.isNotBlank() } ?: "unknown error"
}
