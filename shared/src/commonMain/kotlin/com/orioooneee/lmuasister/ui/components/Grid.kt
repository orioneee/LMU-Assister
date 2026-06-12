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

/**
 * Column count for the race grids: as many [minCardWidth]-wide cards as fit,
 * down to a single full-width card on narrow screens, capped at [max].
 */
@Composable
fun rememberGridColumns(minCardWidth: Dp = 185.dp, max: Int = 5): Int {
    val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    if (widthDp.value <= 0f) return 1
    return (widthDp.value / minCardWidth.value).toInt().coerceIn(1, max)
}

/**
 * One row of race cards, all stretched to the tallest card's height
 * (`IntrinsicSize.Max` + `fillMaxHeight`) so a row never has uneven cards.
 */
@Composable
fun EqualHeightRaceRow(
    races: List<Race>,
    columns: Int,
    onOpenRace: (Race) -> Unit,
    spacing: Dp = 12.dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        races.forEach { race ->
            RaceCard(race, Modifier.weight(1f).fillMaxHeight()) { onOpenRace(race) }
        }
        repeat(columns - races.size) { Spacer(Modifier.weight(1f)) }
    }
}
