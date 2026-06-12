package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.hhmm
import com.orioooneee.lmuasister.ui.util.isToday
import com.orioooneee.lmuasister.ui.util.startsInLabel
import com.orioooneee.lmuasister.ui.util.weekdayShort
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The day's full start-time grid (like the in-game schedule cards): past slots
 * struck through, the next one highlighted, a "STARTS IN …" countdown up top.
 */
@Composable
fun TimesGrid(times: List<Instant>, columns: Int = 4) {
    val now = remember { Clock.System.now() }
    val upcoming = remember(times) { times.filter { it >= now } }
    if (upcoming.isEmpty()) return
    val next = upcoming.first()

    val header = next.let { if (it.isToday(now)) "TIMES" else "${it.weekdayShort()} · TIMES" }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(header, style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.SemiBold)
            Text(
                startsInLabel(next, now),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Outline)
        Spacer(Modifier.height(8.dp))

        upcoming.chunked(columns).forEach { rowTimes ->
            Row(Modifier.fillMaxWidth()) {
                rowTimes.forEach { t ->
                    val isNext = t == next
                    Text(
                        text = t.hhmm(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNext) TextHigh else TextMed,
                    )
                }
                repeat(columns - rowTimes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}
