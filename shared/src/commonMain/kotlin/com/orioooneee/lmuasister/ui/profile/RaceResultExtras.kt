package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orioooneee.lmuasister.data.remote.FuelStrategyDto
import com.orioooneee.lmuasister.data.remote.BackendApiException
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.SessionStrategyDto
import com.orioooneee.lmuasister.data.remote.TyreStintDto
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.OutlineSoft
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.parseHexColor
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.analytics.TelemetryError
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.launch

private val RaceCardGold = Color(0xFFD6B56B)
private val RaceCardGoldText = Color(0xFFE7CF98)
private val RaceCardDark = Color(0xFF0D1015)
private val QualifyingStrategyAccent = Color(0xFF76A9FF)
private val FuelAccent = Color(0xFFF1667E)
private val UnknownTyre = Color(0xFF6F7785)

private enum class RaceCardAction { Share, Copy }

@Composable
internal fun RaceCardActionsRow(
    detail: RaceDetailDto,
    loadCard: suspend () -> ByteArray,
    snackbar: SnackbarHostState,
) {
    val controller = rememberRaceCardActionsController()
    if (!controller.canShare && !controller.canCopy) return

    val scope = rememberCoroutineScope()
    var running by remember(detail.eventId, detail.split) { mutableStateOf<RaceCardAction?>(null) }
    var cardBytes by remember(detail.eventId, detail.split) { mutableStateOf<ByteArray?>(null) }
    val fileName = remember(detail.eventId, detail.title, detail.split) { raceCardFileName(detail) }

    fun runAction(action: RaceCardAction) {
        if (running != null) return
        scope.launch {
            running = action
            val result = runCatching {
                val bytes = cardBytes ?: loadCard().also { cardBytes = it }
                require(bytes.isNotEmpty()) { "The race card is empty" }
                when (action) {
                    RaceCardAction.Share -> controller.sharePng(bytes, fileName)
                    RaceCardAction.Copy -> controller.copyPng(bytes, fileName)
                }
            }.getOrElse {
                RaceCardActionResult(false, raceCardLoadError(it), cause = it)
            }
            running = null
            if (!result.success) {
                logRaceCardFailure(
                    detail = detail,
                    action = action,
                    error = result.cause ?: TelemetryError("race_card_action_failed"),
                )
            }
            snackbar.showSnackbar(result.message)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (controller.canShare) {
            RaceCardActionButton(
                label = "Share",
                icon = Icons.Outlined.Share,
                loading = running == RaceCardAction.Share,
                enabled = running == null,
                modifier = Modifier.weight(1f),
            ) { runAction(RaceCardAction.Share) }
        }
        if (controller.canCopy) {
            RaceCardActionButton(
                label = "Copy race card",
                icon = Icons.Outlined.ContentCopy,
                loading = running == RaceCardAction.Copy,
                enabled = running == null,
                modifier = Modifier.weight(1f),
            ) { runAction(RaceCardAction.Copy) }
        }
    }
}

@Composable
internal fun RaceCardSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        Snackbar(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .border(1.dp, RaceCardGold.copy(alpha = 0.38f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            containerColor = Surface2,
            contentColor = TextHigh,
            actionContentColor = RaceCardGoldText,
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun raceCardLoadError(error: Throwable): String {
    val code = (error as? BackendApiException)?.code ?: error.message
    return when (code) {
        "public_race_card_unsupported" -> "Race card sharing isn't available yet."
        "race_not_found", "user_not_found" -> "Race card isn't available for this race"
        "share_card_download_failed" -> "Couldn't download the race card. Try again."
        "share_card_storage_unavailable", "share_card_storage_error",
        "nakama_unavailable", "share_card_unavailable", "share_card_invalid_response",
        "share_card_invalid_url", "share_card_empty" -> "Couldn't create the race card. Try again later."
        else -> "Couldn't create the race card. Try again later."
    }
}

private fun logRaceCardFailure(
    detail: RaceDetailDto,
    action: RaceCardAction,
    error: Throwable,
) {
    val backendError = error as? BackendApiException
    val safeMessage = error.message
        ?.replace(Regex("\\s+"), " ")
        ?.take(240)
        .orEmpty()
    println(
        "[RaceCard] ERROR" +
            " action=${action.name.lowercase()}" +
            " eventId=${detail.eventId}" +
            " split=${detail.split ?: "none"}" +
            " status=${backendError?.statusCode ?: "none"}" +
            " code=${backendError?.code ?: "none"}" +
            " type=${error::class.simpleName ?: "Throwable"}" +
            " message=$safeMessage",
    )
    Telemetry.recordError(
        error,
        "feature" to "race_card",
        "action" to action.name.lowercase(),
        "event_id" to detail.eventId,
        "split" to detail.split,
        "http_status" to backendError?.statusCode,
        "error_code" to backendError?.code,
    )
}

@Composable
private fun RaceCardActionButton(
    label: String,
    icon: ImageVector,
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        RaceCardGold.copy(alpha = 0.20f),
                        RaceCardDark.copy(alpha = 0.96f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = RaceCardGold.copy(alpha = if (enabled) 0.62f else 0.28f),
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                color = RaceCardGoldText,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RaceCardGoldText.copy(alpha = if (enabled) 1f else 0.55f),
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RaceCardGoldText.copy(alpha = if (enabled) 1f else 0.55f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun raceCardFileName(detail: RaceDetailDto): String {
    val slug = detail.title
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "race" }
    val split = detail.split?.let { "-split-$it" }.orEmpty()
    return "lmu-$slug$split.png"
}

@Composable
internal fun SessionStrategiesSection(detail: RaceDetailDto) {
    val strategies = listOf(
        "qualifying" to detail.sessions["qualifying"]?.strategy,
        "race" to detail.sessions["race"]?.strategy,
    ).filter { (_, strategy) -> strategy?.hasContent() == true }
    if (strategies.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
        strategies.forEachIndexed { index, (key, strategy) ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .height(2.dp)
                        .background(Outline),
                )
            }
            SessionStrategyStrip(
                sessionKey = key,
                strategy = strategy!!,
                modifier = Modifier.padding(top = if (index == 0) 12.dp else 14.dp),
            )
        }
    }
}

private fun SessionStrategyDto.hasContent(): Boolean =
    tyres?.stints?.isNotEmpty() == true || fuel != null

@Composable
private fun SessionStrategyStrip(
    sessionKey: String,
    strategy: SessionStrategyDto,
    modifier: Modifier = Modifier,
) {
    val tyres = strategy.tyres
    val fuel = strategy.fuel
    val accent = if (sessionKey == "qualifying") QualifyingStrategyAccent else Amber
    val sessionLabel = if (sessionKey == "qualifying") "QUALIFYING" else "RACE"
    val kind = if (tyres?.stints?.isNotEmpty() == true) "TYRES" else "FUEL"
    val counts = buildList {
        strategy.lapCount.takeIf { it > 0 }?.let { add("$it ${if (it == 1) "LAP" else "LAPS"}") }
        tyres?.stintCount?.takeIf { it > 0 }?.let { add("$it ${if (it == 1) "STINT" else "STINTS"}") }
    }.joinToString("  ·  ")

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(30.dp).background(accent))
            Text(
                "$sessionLabel $kind",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = accent,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            if (counts.isNotBlank()) {
                Text(
                    counts,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }

        if (!tyres?.stints.isNullOrEmpty()) {
            Spacer(Modifier.height(10.dp))
            TyreStintsRow(tyres.stints)
        }
        fuel?.let {
            Spacer(Modifier.height(7.dp))
            FuelStrategyRow(it)
        }
    }
}

@Composable
private fun TyreStintsRow(stints: List<TyreStintDto>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        stints.forEachIndexed { index, stint ->
            if (index > 0) {
                Box(Modifier.width(1.dp).height(29.dp).background(Outline))
            }
            TyreStint(
                stint = stint,
                modifier = Modifier
                    .weight(stint.lapCount.coerceAtLeast(1).toFloat())
                    .widthIn(min = 48.dp)
                    .padding(horizontal = if (stints.size == 1) 0.dp else 7.dp),
            )
        }
    }
}

@Composable
private fun TyreStint(stint: TyreStintDto, modifier: Modifier = Modifier) {
    val front = parseHexColor(stint.frontColor) ?: UnknownTyre
    val rear = parseHexColor(stint.rearColor) ?: front
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stint.label.ifBlank { "?" },
            style = MaterialTheme.typography.labelSmall,
            color = front,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${stint.lapCount} ${if (stint.lapCount == 1) "LAP" else "LAPS"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(1.dp)),
        ) {
            Box(Modifier.fillMaxWidth().weight(1f).background(front))
            Box(Modifier.fillMaxWidth().weight(1f).background(rear))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuelStrategyRow(fuel: FuelStrategyDto) {
    val accent = parseHexColor(fuel.accentColor) ?: FuelAccent
    val stats = listOf(
        "${strategyPercent(fuel.usedPct)} used",
        "${strategyPercent(fuel.remainingPct)} left",
        "${strategyPercent(fuel.averagePerLapPct, average = true)}/lap",
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "FUEL",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = accent,
                fontWeight = FontWeight.Black,
            )
            stats.forEachIndexed { index, value ->
                if (index > 0) Text("·", color = Outline, fontWeight = FontWeight.Bold)
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMed,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun strategyPercent(value: Double?, average: Boolean = false): String {
    value ?: return "-"
    val decimals = if (average && abs(value) < 1.0) 2 else 1
    val scale = if (decimals == 2) 100L else 10L
    val rounded = (value * scale).roundToLong()
    val sign = if (rounded < 0) "-" else ""
    val magnitude = abs(rounded)
    val whole = magnitude / scale
    val fraction = (magnitude % scale).toString().padStart(decimals, '0').trimEnd('0')
    return "$sign$whole${if (fraction.isNotEmpty()) ".$fraction" else ""}%"
}
