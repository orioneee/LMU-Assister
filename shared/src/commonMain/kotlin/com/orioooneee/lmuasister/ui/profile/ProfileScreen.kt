package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.data.steam.SteamGuardKind
import com.orioooneee.lmuasister.ui.IconSteam
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.ClassGt3
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SteamTop = Color(0xFF2A475E)
private val SteamBottom = Color(0xFF171A21)
private val SteamLogoBg = Color(0xFF1B2838)
private val DangerRed = Color(0xFFE5484D)

@Composable
fun ProfileScreen(
    viewModel: SteamLoginViewModel = koinViewModel(),
    insets: PaddingValues = PaddingValues(),
    onSeeAllRaces: () -> Unit = {},
    onOpenRace: (eventId: String, split: Int?) -> Unit = { _, _ -> },
    onOpenSuspensions: (active: Boolean) -> Unit = {},
    onOpenCategory: (StatCategory) -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val loading = state is SteamLoginUiState.Loading
    val guardRequired = state is SteamLoginUiState.GuardRequired
    val restoring = state is SteamLoginUiState.Restoring
    val signedIn = state as? SteamLoginUiState.SignedIn

    if (restoring) {
        Column(
            Modifier.fillMaxSize().background(Carbon).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(topInset + 24.dp))
            ProfileSkeleton()
        }
        return
    }

    if (signedIn != null) {
        val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
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
                ProfileContent(signedIn.backend, onSeeAllRaces, onOpenRace, onOpenSuspensions, onOpenCategory)
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
        Spacer(Modifier.height(topInset + 48.dp))
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(SteamLogoBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IconSteam, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(Res.string.profile_login_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TextHigh,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.profile_login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMed,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

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
            enabled = !loading,
        )
        Spacer(Modifier.height(14.dp))
        Field(
            value = code,
            onValueChange = { code = it },
            label = stringResource(Res.string.profile_field_2fa),
            keyboardType = KeyboardType.Number,
            enabled = guardRequired && !loading,
        )

        StatusLine(state)

        Spacer(Modifier.height(24.dp))

        SignInButton(
            loading = loading,
            onClick = { viewModel.login(login, password, code) },
        )

        Spacer(Modifier.height(16.dp))
        ConsentNote(onOpenPrivacy)

        Spacer(Modifier.height(32.dp + bottomInset))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsentNote(onOpenPrivacy: () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Text(
            "By signing in you agree to the",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
        )
        Text(
            "Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onOpenPrivacy),
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
                    "Don't worry — all your race, profile and rating data lives on the game's " +
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
    onSeeAllRaces: () -> Unit,
    onOpenRace: (eventId: String, split: Int?) -> Unit,
    onOpenSuspensions: (active: Boolean) -> Unit,
    onOpenCategory: (StatCategory) -> Unit,
) {
    when (backend) {
        is BackendState.Ok -> ProfileView(
            backend.profile,
            accountName = "",
            onSeeAllRaces = onSeeAllRaces,
            onOpenRace = onOpenRace,
            onOpenSuspensions = onOpenSuspensions,
            onOpenCategory = onOpenCategory,
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

@Composable
private fun StatusLine(state: SteamLoginUiState) {
    val (text, color) = when (state) {
        is SteamLoginUiState.GuardRequired -> {
            val via = if (state.kind == SteamGuardKind.EMAIL) "email" else "your Steam app"
            "Enter the Steam Guard code from $via." to MaterialTheme.colorScheme.primary
        }
        is SteamLoginUiState.Error -> state.message to MaterialTheme.colorScheme.error
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
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface1,
            unfocusedContainerColor = Surface1,
            disabledContainerColor = Surface1.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Outline,
            disabledBorderColor = Outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = TextMed,
            disabledLabelColor = TextLow,
            focusedTextColor = TextHigh,
            unfocusedTextColor = TextHigh,
            disabledTextColor = TextLow,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun SignInButton(loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(SteamTop, SteamBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(
                stringResource(Res.string.profile_sign_in),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
