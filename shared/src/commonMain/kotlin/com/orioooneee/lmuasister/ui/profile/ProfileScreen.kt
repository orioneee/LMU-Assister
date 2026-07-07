package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.orioooneee.lmuasister.data.remote.CarDetailedDto
import com.orioooneee.lmuasister.supportsSteamGuardMobileApproval
import com.orioooneee.lmuasister.data.steam.SteamGuardKind
import com.orioooneee.lmuasister.ui.IconSteam
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.Lime
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.profile_field_2fa
import lmuassister.shared.generated.resources.profile_field_login
import lmuassister.shared.generated.resources.profile_field_password
import lmuassister.shared.generated.resources.profile_login_subtitle
import lmuassister.shared.generated.resources.profile_login_title
import lmuassister.shared.generated.resources.profile_sign_in
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SteamLogoBg = Color(0xFF223044)
private val AuthAccent = Color(0xFFE7B84E)
private val QrPaper = Color(0xFFFAF8F1)
private val DangerRed = Color(0xFFE5484D)
private const val STEAM_GUARD_APPROVAL_SECONDS = 120
private const val STEAM_QR_SECONDS = 120
private const val MINTER_INSTALL_URL = "https://www.lmu-assister.com/#minter-install"

private enum class AuthStepKey { Local, Steam, Verify, Profile }

private enum class AuthStepState { Pending, Current, Done, Error }

private enum class AuthStage {
    Silent,
    Login,
    GuardWait,
    GuardCheck,
    Approval,
    QrStart,
    Qr,
    Profile,
}

private data class AuthStepUi(
    val key: AuthStepKey,
    val label: String,
    val state: AuthStepState,
)

@Composable
fun ProfileScreen(
    viewModel: SteamLoginViewModel = koinViewModel(),
    insets: PaddingValues = PaddingValues(),
    onSeeAllRaces: () -> Unit = {},
    onOpenRace: (eventId: String, split: Int?) -> Unit = { _, _ -> },
    onOpenSuspensions: (active: Boolean) -> Unit = {},
    onOpenCategory: (StatCategory) -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenTracks: () -> Unit = {},
    onOpenCar: (CarDetailedDto) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var privacyAccepted by remember { mutableStateOf(false) }
    var qrPrivacyNotice by remember { mutableStateOf(false) }
    var guardApprovalSecondsLeft by remember { mutableStateOf(STEAM_GUARD_APPROVAL_SECONDS) }
    var qrSecondsLeft by remember { mutableStateOf(STEAM_QR_SECONDS) }

    val pendingApproval = state as? SteamLoginUiState.DeviceConfirmationPending
    val pendingQr = state as? SteamLoginUiState.QrCodePending
    val startingQr = state is SteamLoginUiState.QrCodeStarting
    val checkingAuthEnvironment = state is SteamLoginUiState.CheckingAuthEnvironment
    val permissionRequired = state as? SteamLoginUiState.LocalNetworkPermissionRequired
    val requestingLocalNetworkPermission = state is SteamLoginUiState.RequestingLocalNetworkPermission
    val minterUnavailable = state as? SteamLoginUiState.MinterUnavailable
    val authEnvironmentBlocked =
        checkingAuthEnvironment || permissionRequired != null || requestingLocalNetworkPermission || minterUnavailable != null
    val signedIn = state as? SteamLoginUiState.SignedIn
    val loadingProfileAfterAuth = signedIn?.backend == BackendState.Loading
    val restoring = state is SteamLoginUiState.Restoring
    val loading = state is SteamLoginUiState.Loading ||
        restoring ||
        loadingProfileAfterAuth ||
        startingQr ||
        pendingApproval != null ||
        pendingQr != null ||
        checkingAuthEnvironment ||
        requestingLocalNetworkPermission
    val guardRequired = state is SteamLoginUiState.GuardRequired
    val waitingForGuardApproval = pendingApproval != null
    val waitingForQr = pendingQr != null
    val canSubmitGuardCodeDuringApproval = pendingApproval != null && code.isNotBlank()
    val credentialFieldsVisible = !authEnvironmentBlocked && pendingQr == null && !startingQr && !loadingProfileAfterAuth
    val cancellableCredentialFlow = pendingApproval != null || guardRequired
    val currentAuthStage = authStageFor(state, code)
    var lastVisibleAuthStage by remember { mutableStateOf<AuthStage?>(null) }
    val visibleAuthStage = if (state is SteamLoginUiState.Error) lastVisibleAuthStage else currentAuthStage
    val authSteps = visibleAuthStage?.steps(markCurrentError = state is SteamLoginUiState.Error)
    val qrNoticeText = when {
        pendingQr != null || startingQr -> null
        qrPrivacyNotice && !privacyAccepted -> "Accept the Privacy Policy to continue with QR sign-in."
        loading && !restoring && !loadingProfileAfterAuth -> "Finish the current sign-in first."
        else -> null
    }
    val approvalTimerStart = pendingApproval?.expiresIn?.takeIf { it > 0 } ?: STEAM_GUARD_APPROVAL_SECONDS
    val qrTimerStart = pendingQr?.expiresIn?.takeIf { it > 0 } ?: STEAM_QR_SECONDS
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val onQrSignInClick = {
        if (privacyAccepted) {
            qrPrivacyNotice = false
            viewModel.loginWithQr()
        } else {
            qrPrivacyNotice = true
        }
    }

    LaunchedEffect(currentAuthStage) {
        if (currentAuthStage != null) lastVisibleAuthStage = currentAuthStage
    }

    LaunchedEffect(privacyAccepted) {
        if (privacyAccepted) qrPrivacyNotice = false
    }

    LaunchedEffect(waitingForGuardApproval, pendingApproval?.challengeId, approvalTimerStart) {
        guardApprovalSecondsLeft = approvalTimerStart
        if (!waitingForGuardApproval) return@LaunchedEffect
        while (guardApprovalSecondsLeft > 0) {
            delay(1_000L)
            guardApprovalSecondsLeft -= 1
        }
        viewModel.expireDeviceConfirmation()
    }

    LaunchedEffect(waitingForQr, pendingQr?.flowId, qrTimerStart) {
        qrSecondsLeft = qrTimerStart
        if (!waitingForQr) return@LaunchedEffect
        while (qrSecondsLeft > 0) {
            delay(1_000L)
            qrSecondsLeft -= 1
        }
        viewModel.expireQrSignIn()
    }

    DisposableEffect(lifecycleOwner, pendingApproval?.challengeId) {
        val challengeId = pendingApproval?.challengeId
        if (challengeId == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.continueDeviceConfirmation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (signedIn != null && !loadingProfileAfterAuth) {
        val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
        val updatingProfile by viewModel.updatingProfile.collectAsStateWithLifecycle()
        val exiting by viewModel.exiting.collectAsStateWithLifecycle()
        var showClearConfirm by remember { mutableStateOf(false) }

        RefreshableContent(refreshing, viewModel::refresh, topInset = topInset) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Carbon)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(topInset + 24.dp))
                ProfileContent(
                    signedIn.backend,
                    updatingProfile,
                    !refreshing && !updatingProfile,
                    viewModel::updateProfile,
                    onSeeAllRaces,
                    onOpenRace,
                    onOpenSuspensions,
                    onOpenCategory,
                    onOpenTracks,
                    onOpenCar,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExitButton(
                        text = "Sign out",
                        loading = exiting == ExitAction.SIGNING_OUT,
                        enabled = exiting == ExitAction.NONE,
                        destructive = false,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.signOut() },
                    )
                    ExitButton(
                        text = "Clear my data",
                        loading = exiting == ExitAction.CLEARING,
                        enabled = exiting == ExitAction.NONE,
                        destructive = true,
                        modifier = Modifier.weight(1f),
                        onClick = { showClearConfirm = true },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenPrivacy)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(32.dp + bottomInset))
            }
        }

        if (showClearConfirm) {
            ClearDataDialog(
                onConfirm = {
                    showClearConfirm = false
                    viewModel.clearMyData()
                },
                onDismiss = { showClearConfirm = false },
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Carbon)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(topInset + 30.dp))
        AuthIntro(Modifier.widthIn(max = 460.dp).fillMaxWidth())
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Surface1)
                .border(1.dp, Outline, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            if (authEnvironmentBlocked) {
                AuthEnvironmentGate(
                    checking = checkingAuthEnvironment,
                    requestingPermission = requestingLocalNetworkPermission,
                    permissionRequired = permissionRequired,
                    minterUnavailable = minterUnavailable,
                    onGrantPermission = viewModel::grantLocalNetworkPermission,
                    onRetryMinter = viewModel::retryAuthEnvironmentCheck,
                    onOpenMinterInstall = { runCatching { uriHandler.openUri(MINTER_INSTALL_URL) } },
                )
            } else {
                if (credentialFieldsVisible) {
                    PrivacyConsent(
                        accepted = privacyAccepted,
                        onAcceptedChange = {
                            privacyAccepted = it
                            if (it) qrPrivacyNotice = false
                        },
                        enabled = !loading,
                        onOpenPrivacy = onOpenPrivacy,
                    )

                    if (!cancellableCredentialFlow) {
                        Spacer(Modifier.height(16.dp))
                        SignInMethodLabel()
                        Spacer(Modifier.height(8.dp))
                        QrSignInButton(
                            active = false,
                            starting = false,
                            checking = false,
                            enabled = !loading,
                            onClick = onQrSignInClick,
                        )
                        if (qrNoticeText != null) {
                            Spacer(Modifier.height(8.dp))
                            AuthNotice(qrNoticeText)
                        }
                        Spacer(Modifier.height(16.dp))
                        OrDivider()
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (credentialFieldsVisible) {
                    Field(
                        value = login,
                        onValueChange = { login = it },
                        label = stringResource(Res.string.profile_field_login),
                        keyboardType = KeyboardType.Text,
                        enabled = !loading,
                    )
                    Spacer(Modifier.height(14.dp))
                    Field(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(Res.string.profile_field_password),
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = it },
                        enabled = !loading,
                    )
                    Spacer(Modifier.height(14.dp))
                    Field(
                        value = code,
                        onValueChange = { code = it },
                        label = stringResource(Res.string.profile_field_2fa),
                        keyboardType = KeyboardType.Number,
                        enabled = guardRequired || pendingApproval != null,
                    )
                }

                StatusLine(
                    state = state,
                    authStage = visibleAuthStage,
                    waitingForGuardApproval = waitingForGuardApproval,
                    guardApprovalSecondsLeft = guardApprovalSecondsLeft,
                    waitingForQr = waitingForQr,
                    qrSecondsLeft = qrSecondsLeft,
                    supportsGuardApproval = supportsSteamGuardMobileApproval,
                )

                if (authSteps != null) {
                    Spacer(Modifier.height(14.dp))
                    AuthSteps(authSteps)
                }

                if (pendingQr != null) {
                    Spacer(Modifier.height(14.dp))
                    QrCodePanel(pendingQr)
                }

                if (credentialFieldsVisible) {
                    Spacer(Modifier.height(16.dp))
                    SignInButton(
                        loading = (restoring || state is SteamLoginUiState.Loading || pendingApproval != null) &&
                            !canSubmitGuardCodeDuringApproval,
                        enabled = privacyAccepted && (!loading || canSubmitGuardCodeDuringApproval),
                        waitingForGuardApproval = waitingForGuardApproval,
                        onClick = { viewModel.login(login, password, code) },
                    )
                }

                if (cancellableCredentialFlow) {
                    Spacer(Modifier.height(10.dp))
                    CancelAuthButton(onClick = viewModel::cancelAuthFlow)
                }

                if (!credentialFieldsVisible && !loadingProfileAfterAuth) {
                    Spacer(Modifier.height(10.dp))
                    CancelAuthButton(onClick = viewModel::cancelAuthFlow)
                }
            }
        }

        Spacer(Modifier.height(32.dp + bottomInset))
    }
}

@Composable
private fun AuthIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Surface1)
                .border(1.dp, Outline, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(SteamLogoBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconSteam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
                "Steam account required",
                style = MaterialTheme.typography.labelMedium,
                color = TextMed,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Text(
            stringResource(Res.string.profile_login_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TextHigh,
            fontWeight = FontWeight.Black,
        )
        Text(
            stringResource(Res.string.profile_login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMed,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacyConsent(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    enabled: Boolean,
    onOpenPrivacy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Carbon.copy(alpha = 0.62f))
            .border(1.dp, if (accepted) AuthAccent.copy(alpha = 0.42f) else Outline, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onAcceptedChange(!accepted) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = accepted,
            onCheckedChange = if (enabled) onAcceptedChange else null,
            colors = CheckboxDefaults.colors(
                checkedColor = AuthAccent,
                uncheckedColor = TextLow,
                checkmarkColor = Carbon,
            ),
        )
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "I agree to the",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
            )
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = AuthAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onOpenPrivacy),
            )
        }
    }
}

@Composable
private fun SignInMethodLabel() {
    Text(
        "Sign-in method",
        style = MaterialTheme.typography.labelMedium,
        color = TextMed,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Outline.copy(alpha = 0.7f)),
        )
        Text(
            "or",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Outline.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun AuthEnvironmentGate(
    checking: Boolean,
    requestingPermission: Boolean,
    permissionRequired: SteamLoginUiState.LocalNetworkPermissionRequired?,
    minterUnavailable: SteamLoginUiState.MinterUnavailable?,
    onGrantPermission: () -> Unit,
    onRetryMinter: () -> Unit,
    onOpenMinterInstall: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        when {
            checking -> {
                AuthGateProgress("Checking browser access")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Before sign-in, LMU Assister checks whether this browser can reach the local JVM minter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMed,
                )
            }
            requestingPermission -> {
                AuthGateProgress("Waiting for browser permission")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Confirm the Chrome permission prompt, then we'll check that the JVM minter is running.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMed,
                )
            }
            permissionRequired != null -> {
                Text(
                    if (permissionRequired.denied) "Local device access is blocked" else "Local device access required",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "LMU Assister needs browser permission to access apps and services on this device. " +
                        "It uses that permission only to talk to the local JVM minter on 127.0.0.1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMed,
                )
                if (permissionRequired.denied) {
                    Spacer(Modifier.height(10.dp))
                    AuthNotice("If no prompt appears, allow local device access in this site's browser settings and try again.")
                }
                Spacer(Modifier.height(16.dp))
                AuthGateButton(
                    text = "Grant permission",
                    loading = false,
                    enabled = true,
                    onClick = onGrantPermission,
                )
            }
            minterUnavailable != null -> {
                Text(
                    "JVM minter is not running",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Start the JVM minter, then check again. If it isn't installed yet, open the installation guide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMed,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    minterUnavailable.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                AuthGateButton(
                    text = "Installation instructions",
                    loading = false,
                    enabled = true,
                    onClick = onOpenMinterInstall,
                )
                Spacer(Modifier.height(10.dp))
                AuthGateButton(
                    text = "Check again",
                    loading = false,
                    enabled = true,
                    onClick = onRetryMinter,
                )
            }
        }
    }
}

@Composable
private fun AuthGateProgress(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(color = AuthAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = TextHigh,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AuthGateButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled && !loading) 1f else 0.48f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AuthAccent.copy(alpha = 0.92f * alpha))
            .border(1.dp, AuthAccent.copy(alpha = 0.62f * alpha), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Carbon, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = Carbon.copy(alpha = alpha),
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun ExitButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    destructive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = if (destructive) DangerRed else TextMed
    val border = if (destructive) DangerRed.copy(alpha = 0.45f) else Outline
    val bg = if (destructive) DangerRed.copy(alpha = 0.10f) else Surface1
    val alpha = if (enabled) 1f else 0.5f

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = bg.alpha * alpha))
            .border(1.dp, border.copy(alpha = border.alpha * alpha), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = accent.copy(alpha = accent.alpha * alpha),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ClearDataDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        titleContentColor = TextHigh,
        textContentColor = TextMed,
        title = { Text("Clear your data?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This clears the data we store for you on our server. " +
                    "Don't worry - all your race, profile and rating data lives on the game's " +
                    "own servers and is independent of us. Next time you sign in, everything " +
                    "loads back automatically.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear", color = DangerRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMed)
            }
        },
    )
}

@Composable
private fun ProfileContent(
    backend: BackendState,
    updatingProfile: Boolean,
    canUpdateProfile: Boolean,
    onUpdateProfile: () -> Unit,
    onSeeAllRaces: () -> Unit,
    onOpenRace: (eventId: String, split: Int?) -> Unit,
    onOpenSuspensions: (active: Boolean) -> Unit,
    onOpenCategory: (StatCategory) -> Unit,
    onOpenTracks: () -> Unit,
    onOpenCar: (CarDetailedDto) -> Unit,
) {
    when (backend) {
        is BackendState.Ok -> ProfileView(
            backend.profile,
            accountName = "",
            isRefreshingProfile = updatingProfile,
            canRefreshProfile = canUpdateProfile,
            onRefreshProfile = onUpdateProfile,
            onSeeAllRaces = onSeeAllRaces,
            onOpenRace = onOpenRace,
            onOpenSuspensions = onOpenSuspensions,
            onOpenCategory = onOpenCategory,
            onOpenTracks = onOpenTracks,
            onOpenCar = onOpenCar,
        )
        BackendState.Loading -> ProfileSkeleton()
        is BackendState.AuthFailed -> ProfileMessage("Couldn't load profile", backend.message)
        is BackendState.Error -> ProfileMessage("Couldn't load profile", backend.message)
    }
}

@Composable
private fun ProfileMessage(title: String, detail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = TextMed, textAlign = TextAlign.Center)
    }
}

private fun authStageFor(state: SteamLoginUiState, guardCode: String): AuthStage? =
    when (state) {
        SteamLoginUiState.Restoring -> AuthStage.Silent
        SteamLoginUiState.Loading -> if (guardCode.isNotBlank()) AuthStage.GuardCheck else AuthStage.Login
        SteamLoginUiState.QrCodeStarting -> AuthStage.QrStart
        is SteamLoginUiState.GuardRequired -> AuthStage.GuardWait
        is SteamLoginUiState.DeviceConfirmationPending -> AuthStage.Approval
        is SteamLoginUiState.QrCodePending -> AuthStage.Qr
        is SteamLoginUiState.SignedIn ->
            if (state.backend == BackendState.Loading) AuthStage.Profile else null
        else -> null
    }

private fun AuthStage.steps(markCurrentError: Boolean): List<AuthStepUi> {
    var localLabel = "Local minter ready"
    var steamLabel = "Signing in to Steam"
    var verifyLabel = "Steam Guard check"
    var profileLabel = "Getting your LMU profile"

    var localState = AuthStepState.Done
    var steamState = AuthStepState.Pending
    var verifyState = AuthStepState.Pending
    var profileState = AuthStepState.Pending
    var currentStep = AuthStepKey.Steam

    when (this) {
        AuthStage.Silent -> {
            steamLabel = "Restoring Steam session"
            verifyLabel = "Steam authorized"
            steamState = AuthStepState.Current
            currentStep = AuthStepKey.Steam
        }
        AuthStage.Login -> {
            steamLabel = "Checking Steam login"
            verifyLabel = "Steam Guard if needed"
            steamState = AuthStepState.Current
            currentStep = AuthStepKey.Steam
        }
        AuthStage.GuardWait -> {
            steamLabel = "Steam credentials accepted"
            verifyLabel = "Waiting for Steam Guard code"
            steamState = AuthStepState.Done
            verifyState = AuthStepState.Current
            currentStep = AuthStepKey.Verify
        }
        AuthStage.GuardCheck -> {
            steamLabel = "Steam credentials accepted"
            verifyLabel = "Checking Steam Guard code"
            steamState = AuthStepState.Done
            verifyState = AuthStepState.Current
            currentStep = AuthStepKey.Verify
        }
        AuthStage.Approval -> {
            steamLabel = "Steam sign-in requested"
            verifyLabel = "Waiting for Steam app approval"
            steamState = AuthStepState.Done
            verifyState = AuthStepState.Current
            currentStep = AuthStepKey.Verify
        }
        AuthStage.QrStart -> {
            steamLabel = "Requesting Steam QR sign-in"
            verifyLabel = "Waiting for Steam app response"
            steamState = AuthStepState.Current
            currentStep = AuthStepKey.Steam
        }
        AuthStage.Qr -> {
            steamLabel = "Steam QR generated"
            verifyLabel = "Waiting for Steam app response"
            steamState = AuthStepState.Done
            verifyState = AuthStepState.Current
            currentStep = AuthStepKey.Verify
        }
        AuthStage.Profile -> {
            steamLabel = "Steam authorized"
            verifyLabel = "Steam verification complete"
            profileLabel = "Getting your LMU profile"
            steamState = AuthStepState.Done
            verifyState = AuthStepState.Done
            profileState = AuthStepState.Current
            currentStep = AuthStepKey.Profile
        }
    }

    if (markCurrentError) {
        when (currentStep) {
            AuthStepKey.Local -> localState = AuthStepState.Error
            AuthStepKey.Steam -> steamState = AuthStepState.Error
            AuthStepKey.Verify -> verifyState = AuthStepState.Error
            AuthStepKey.Profile -> profileState = AuthStepState.Error
        }
    }

    return listOf(
        AuthStepUi(AuthStepKey.Local, localLabel, localState),
        AuthStepUi(AuthStepKey.Steam, steamLabel, steamState),
        AuthStepUi(AuthStepKey.Verify, verifyLabel, verifyState),
        AuthStepUi(AuthStepKey.Profile, profileLabel, profileState),
    )
}

@Composable
private fun AuthSteps(steps: List<AuthStepUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Carbon.copy(alpha = 0.62f))
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        steps.forEach { step ->
            AuthStepRow(step)
        }
    }
}

@Composable
private fun AuthStepRow(step: AuthStepUi) {
    val color = when (step.state) {
        AuthStepState.Pending -> TextLow
        AuthStepState.Current -> AuthAccent
        AuthStepState.Done -> TextMed
        AuthStepState.Error -> ClassHyper
    }
    val dotFill = when (step.state) {
        AuthStepState.Pending -> Color.Transparent
        AuthStepState.Current -> AuthAccent.copy(alpha = 0.24f)
        AuthStepState.Done -> Lime
        AuthStepState.Error -> ClassHyper
    }
    val dotSize = if (step.state == AuthStepState.Current) 12.dp else 10.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotFill)
                    .border(1.dp, color, CircleShape),
            )
        }
        Text(
            step.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (step.state == AuthStepState.Current || step.state == AuthStepState.Error) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatusLine(
    state: SteamLoginUiState,
    authStage: AuthStage?,
    waitingForGuardApproval: Boolean,
    guardApprovalSecondsLeft: Int,
    waitingForQr: Boolean,
    qrSecondsLeft: Int,
    supportsGuardApproval: Boolean,
) {
    val (text, color) = when (state) {
        SteamLoginUiState.Loading -> {
            if (waitingForGuardApproval) {
                val minutes = guardApprovalSecondsLeft / 60
                val seconds = guardApprovalSecondsLeft % 60
                "Approve the sign-in request in the Steam app, then return here. ${minutes}:${seconds.toString().padStart(2, '0')} left." to
                    AuthAccent
            } else if (waitingForQr) {
                val minutes = qrSecondsLeft / 60
                val seconds = qrSecondsLeft % 60
                "Scan the Steam code, then keep this page open. ${minutes}:${seconds.toString().padStart(2, '0')} left." to
                    AuthAccent
            } else {
                if (authStage == AuthStage.GuardCheck) {
                    "Checking Steam Guard code..."
                } else {
                    "Signing in to Steam..."
                } to AuthAccent
            }
        }
        SteamLoginUiState.Restoring ->
            "Restoring your Steam session..." to AuthAccent
        SteamLoginUiState.QrCodeStarting ->
            "Requesting Steam QR sign-in..." to AuthAccent
        is SteamLoginUiState.DeviceConfirmationPending -> {
            val minutes = guardApprovalSecondsLeft / 60
            val seconds = guardApprovalSecondsLeft % 60
            "Approve the sign-in request in the Steam app, or enter the Steam Guard code here. ${minutes}:${seconds.toString().padStart(2, '0')} left." to
                AuthAccent
        }
        is SteamLoginUiState.QrCodePending -> {
            val minutes = qrSecondsLeft / 60
            val seconds = qrSecondsLeft % 60
            "Scan this code in the Steam mobile app. ${minutes}:${seconds.toString().padStart(2, '0')} left." to
                AuthAccent
        }
        is SteamLoginUiState.GuardRequired -> {
            when (state.kind) {
                SteamGuardKind.EMAIL -> "Enter the Steam Guard code from your email."
                SteamGuardKind.DEVICE ->
                    if (supportsGuardApproval) "Enter the Steam Guard code from your Steam app."
                    else "Open Steam Guard and enter the current code."
            } to AuthAccent
        }
        is SteamLoginUiState.SignedIn -> {
            if (state.backend != BackendState.Loading) return
            if (state.restored) {
                "Steam session restored. Getting your profile..."
            } else {
                "Steam authorized. Getting your profile..."
            } to AuthAccent
        }
        is SteamLoginUiState.Error -> state.message to ClassHyper
        else -> return
    }
    Spacer(Modifier.height(14.dp))
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QrCodePanel(state: SteamLoginUiState.QrCodePending) {
    val matrix = remember(state.challengeUrl) { QrCodeMatrix.encode(state.challengeUrl) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Carbon.copy(alpha = 0.62f))
            .border(1.dp, AuthAccent.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (matrix != null) {
            QrMatrix(matrix)
            Spacer(Modifier.height(14.dp))
        }
        state.displayCode?.let { displayCode ->
            QrDisplayCode(displayCode)
            Spacer(Modifier.height(10.dp))
        }
        SelectionContainer {
            Text(
                state.challengeUrl,
                style = MaterialTheme.typography.bodySmall,
                color = TextLow,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.checking) {
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = AuthAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Checking code",
                    style = MaterialTheme.typography.labelMedium,
                    color = AuthAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun QrDisplayCode(code: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AuthAccent.copy(alpha = 0.12f))
            .border(1.dp, AuthAccent.copy(alpha = 0.34f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Steam code",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            code.chunked(4).joinToString(" "),
            style = MaterialTheme.typography.titleMedium,
            color = TextHigh,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QrMatrix(matrix: QrCodeMatrix) {
    Canvas(
        modifier = Modifier
            .sizeIn(maxWidth = 260.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(QrPaper)
            .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        val quietZone = 4
        val cells = matrix.size + quietZone * 2
        val cellSize = size.minDimension / cells
        val left = (size.width - cellSize * cells) / 2f
        val top = (size.height - cellSize * cells) / 2f
        val inset = (cellSize * 0.08f).coerceAtMost(1.2f)
        val moduleSize = (cellSize - inset * 2f).coerceAtLeast(1f)
        val radius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f)
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(
                            left + (x + quietZone) * cellSize + inset,
                            top + (y + quietZone) * cellSize + inset,
                        ),
                        size = Size(moduleSize, moduleSize),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    enabled = enabled,
                    onClick = { onPasswordVisibilityChange(!passwordVisible) },
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = if (enabled) TextMed else TextLow,
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Carbon.copy(alpha = 0.64f),
            unfocusedContainerColor = Carbon.copy(alpha = 0.64f),
            disabledContainerColor = Surface1.copy(alpha = 0.5f),
            focusedBorderColor = AuthAccent,
            unfocusedBorderColor = Outline,
            disabledBorderColor = Outline.copy(alpha = 0.5f),
            focusedLabelColor = AuthAccent,
            unfocusedLabelColor = TextMed,
            disabledLabelColor = TextLow,
            focusedTextColor = TextHigh,
            unfocusedTextColor = TextHigh,
            disabledTextColor = TextLow,
            cursorColor = AuthAccent,
        ),
    )
}

@Composable
private fun QrSignInButton(
    active: Boolean,
    starting: Boolean,
    checking: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val busy = active || starting || checking
    val alpha = if (enabled || busy) 1f else 0.48f
    val bg = if (busy) SteamLogoBg else AuthAccent.copy(alpha = 0.92f)
    val border = if (busy) Outline else AuthAccent.copy(alpha = 0.62f)
    val primaryText = if (busy) TextHigh else Carbon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = bg.alpha * alpha))
            .border(1.dp, border.copy(alpha = border.alpha * alpha), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && !active, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (checking || starting) {
            CircularProgressIndicator(color = primaryText, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            when {
                starting -> "Generating QR code"
                checking -> "Checking QR code"
                active -> "Waiting for QR scan"
                else -> "Sign in with Steam QR"
            },
            style = MaterialTheme.typography.titleMedium,
            color = primaryText.copy(alpha = alpha),
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun AuthNotice(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = AuthAccent,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AuthAccent.copy(alpha = 0.10f))
            .border(1.dp, AuthAccent.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun CancelAuthButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Carbon.copy(alpha = 0.58f))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "Cancel sign-in",
            style = MaterialTheme.typography.titleSmall,
            color = TextMed,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SignInButton(
    loading: Boolean,
    enabled: Boolean,
    waitingForGuardApproval: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled && !loading) 1f else 0.48f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Carbon.copy(alpha = 0.58f * alpha))
            .border(1.dp, Outline.copy(alpha = 0.92f * alpha), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = AuthAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            if (waitingForGuardApproval) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "Waiting for approval",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                stringResource(Res.string.profile_sign_in),
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
