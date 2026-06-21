package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.model.Race

@Composable
fun rememberGridColumns(minCardWidth: Dp = 185.dp, max: Int = 5): Int {
    val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    if (widthDp.value <= 0f) return 1
    return (widthDp.value / minCardWidth.value).toInt().coerceIn(1, max)
}

@Composable
fun EqualHeightRaceRow(
    races: List<Race>,
    columns: Int,
    onOpenRace: (Race) -> Unit,
    spacing: Dp = 12.dp,
    showCountdown: Boolean = true,
    nextRaceIds: Set<String> = emptySet(),
    contentPadding: Dp = 32.dp, // LazyColumn horizontal padding (16dp each side)
) {
    // Compute the time-grid column count from each card's width here — the cards live
    // inside an IntrinsicSize row, so TimesGrid itself can't use BoxWithConstraints.
    val timeColumns = rememberTimeColumns(columns, spacing, contentPadding)
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        races.forEach { race ->
            RaceCard(
                race,
                Modifier.weight(1f).fillMaxHeight(),
                showCountdown = showCountdown,
                isNext = race.id in nextRaceIds,
                timeColumns = timeColumns,
            ) { onOpenRace(race) }
        }
        repeat(columns - races.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun rememberTimeColumns(columns: Int, spacing: Dp, contentPadding: Dp, perCol: Dp = 36.dp): Int {
    val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    if (widthDp.value <= 0f) return 3
    val cardW = (widthDp - contentPadding - spacing * (columns - 1)) / columns
    val innerW = cardW - 24.dp // RaceCard inner padding (12dp each side)
    return (innerW.value / perCol.value).toInt().coerceIn(3, 7)
}
