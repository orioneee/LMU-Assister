package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.data.remote.SuspensionDto
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.SkillBeginner
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatEpochDateTime
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.susp_empty_active
import lmuassister.shared.generated.resources.susp_empty_past
import lmuassister.shared.generated.resources.susp_empty_subtitle
import lmuassister.shared.generated.resources.susp_from
import lmuassister.shared.generated.resources.susp_issued
import lmuassister.shared.generated.resources.susp_permanent_note
import lmuassister.shared.generated.resources.susp_reason_none
import lmuassister.shared.generated.resources.susp_reason_redacted
import lmuassister.shared.generated.resources.susp_status_active
import lmuassister.shared.generated.resources.susp_status_expired
import lmuassister.shared.generated.resources.susp_status_permanent
import lmuassister.shared.generated.resources.susp_title_active
import lmuassister.shared.generated.resources.susp_title_past
import lmuassister.shared.generated.resources.susp_type
import lmuassister.shared.generated.resources.susp_until
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val PosGreen = SkillBeginner
private val NegRed = ClassHyper
private val FlagGray = Color(0xFF8A8F98)

@Composable
fun SuspensionsScreen(
    viewModel: SteamLoginViewModel,
    insets: PaddingValues,
    active: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = (state as? SteamLoginUiState.SignedIn)?.backend
        ?.let { it as? BackendState.Ok }?.profile
    val suspensions = profile?.suspensions.orEmpty().filter { it.active == active }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                stringResource(if (active) Res.string.susp_title_active else Res.string.susp_title_past),
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (suspensions.isEmpty()) {
            EmptyState(active)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(suspensions) { SuspensionCard(it) }
        }
    }
}

@Composable
private fun EmptyState(active: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✓", style = MaterialTheme.typography.displaySmall, color = PosGreen, fontWeight = FontWeight.Black)
            Text(
                stringResource(if (active) Res.string.susp_empty_active else Res.string.susp_empty_past),
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(Res.string.susp_empty_subtitle), style = MaterialTheme.typography.bodySmall, color = TextMed)
        }
    }
}

@Composable
private fun SuspensionCard(s: SuspensionDto) {
    val accent = if (s.active) NegRed else FlagGray
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBadge(stringResource(statusLabelRes(s)), accent)
            if (s.type != null) {
                Text(stringResource(Res.string.susp_type, s.type), style = MaterialTheme.typography.labelSmall, color = TextLow)
            }
        }

        val reason = s.reason?.takeIf { it.isNotBlank() }
        Text(
            reason ?: stringResource(if (s.redacted) Res.string.susp_reason_redacted else Res.string.susp_reason_none),
            style = MaterialTheme.typography.bodyMedium,
            color = if (reason != null) TextHigh else TextMed,
        )

        DateRange(s)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = onBadgeText(color),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DateRange(s: SuspensionDto) {
    if (s.permanent) {
        DateRow(Res.string.susp_issued, s.from)
        Text(stringResource(Res.string.susp_permanent_note), style = MaterialTheme.typography.labelMedium, color = NegRed)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DateRow(Res.string.susp_from, s.from)
        DateRow(Res.string.susp_until, s.to)
    }
}

@Composable
private fun DateRow(label: StringResource, ms: Long?) {
    if (ms == null || ms <= 0) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Surface3).padding(horizontal = 6.dp)) {
            Text(formatEpochDateTime(ms), style = MaterialTheme.typography.labelMedium, color = TextMed)
        }
    }
}

private fun statusLabelRes(s: SuspensionDto): StringResource = when {
    s.permanent -> Res.string.susp_status_permanent
    s.active -> Res.string.susp_status_active
    else -> Res.string.susp_status_expired
}
