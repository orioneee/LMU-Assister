package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.orioooneee.lmuasister.ui.util.rememberNow
import com.orioooneee.lmuasister.ui.util.startsInLabel
import com.orioooneee.lmuasister.ui.util.weekdayShort
import kotlin.time.Instant
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.times
import lmuassister.shared.generated.resources.times_day
import org.jetbrains.compose.resources.stringResource

/**
 * The day's full start-time grid (like the in-game schedule cards): past slots
 * struck through, the next one highlighted, a "STARTS IN …" countdown up top.
 *
 * @param showCountdown live "STARTS IN …" badge — hide it for non-current weeks,
 *   where a relative countdown from `now` would be misleading.
 * @param centered center the header and time cells (single-event layout).
 */
@Composable
fun TimesGrid(
    times: List<Instant>,
    columns: Int = 3,
    showCountdown: Boolean = true,
    centered: Boolean = false,
) {
    val now = rememberNow()
    val upcoming = times.filter { it >= now }
    if (upcoming.isEmpty()) return
    val next = upcoming.first()

    val header = if (next.isToday(now)) stringResource(Res.string.times)
    else stringResource(Res.string.times_day, next.weekdayShort())

    val cellAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(Modifier.fillMaxWidth()) {
        if (showCountdown) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(header, style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.SemiBold)
                CountdownBadge(next, now)
            }
        } else {
            Text(
                header,
                modifier = Modifier.fillMaxWidth(),
                textAlign = cellAlign,
                style = MaterialTheme.typography.labelMedium,
                color = TextMed,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Outline)
        Spacer(Modifier.height(8.dp))

        upcoming.chunked(columns).forEach { rowTimes ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (centered) {
                    Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                } else {
                    Arrangement.Start
                },
            ) {
                rowTimes.forEach { t ->
                    val isNext = showCountdown && t == next
                    Text(
                        text = t.hhmm(),
                        // Centered: fixed-width cells kept as a tight, aligned cluster.
                        // Default: weighted columns that span the card width.
                        modifier = if (centered) Modifier.width(56.dp) else Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNext) TextHigh else TextMed,
                    )
                }
                if (!centered) repeat(columns - rowTimes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}
