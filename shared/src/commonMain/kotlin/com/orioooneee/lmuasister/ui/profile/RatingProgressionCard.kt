package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orioooneee.lmuasister.data.remote.RatingHistoryDto
import com.orioooneee.lmuasister.data.remote.RatingPointDto
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DrAccent = Color(0xFFFF4D5E) // driver rating — pink/red
private val SrAccent = Color(0xFF53D9C9) // safety rating — teal

private enum class RatingKind { DR, SR }

private enum class RangeOpt(val label: String, val days: Long?) {
    D1("24h", 1), W1("7d", 7), M1("30d", 30), Y1("1y", 365), ALL("All", null)
}

private val MON = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Continuous score → rank/tier label, e.g. 466 → "S2" (Silver tier 2). Inverse of the
 *  backend's `score` packing: 100 pts per tier, 3 tiers per rank (Bronze→Platinum). */
private fun scoreLabel(score: Double): String {
    val step = floor(score / 100.0).toInt()           // 0 = Bronze-1
    val rankIdx = step.floorDiv(3).coerceIn(0, 3)
    val tier = step.mod(3) + 1
    return "${listOf("B", "S", "G", "P")[rankIdx]}$tier"
}

private fun shortDate(iso: String): String? = runCatching {
    val dt = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
    "${dt.day} ${MON[dt.month.ordinal]}"
}.getOrNull()

private fun RatingPointDto.instantOrNull(): Instant? =
    date?.let { runCatching { Instant.parse(it) }.getOrNull() }

private fun filterRange(points: List<RatingPointDto>, range: RangeOpt): List<RatingPointDto> {
    val window = range.days ?: return points
    val cutoff = Clock.System.now() - window.days
    return points.filter { p -> p.instantOrNull()?.let { it >= cutoff } ?: false }
}

/** DR/SR progression chart with a rank-tier Y axis, a DR/SR toggle and time-range chips. */
@Composable
fun RatingProgressionCard(history: RatingHistoryDto, modifier: Modifier = Modifier) {
    var kind by remember { mutableStateOf(RatingKind.DR) }
    var range by remember { mutableStateOf(RangeOpt.ALL) }

    val accent = if (kind == RatingKind.DR) DrAccent else SrAccent
    val series = if (kind == RatingKind.DR) history.dr else history.sr
    val points = remember(kind, range, history) {
        filterRange(series, range).filter { it.score != null }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "RATING PROGRESSION",
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TogglePill("DR", kind == RatingKind.DR, DrAccent) { kind = RatingKind.DR }
                TogglePill("SR", kind == RatingKind.SR, SrAccent) { kind = RatingKind.SR }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RangeOpt.entries.forEach { opt ->
                RangeChip(opt.label, range == opt) { range = opt }
            }
        }

        Text(
            "${points.size} online races",
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
        )

        if (points.size < 2) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Not enough races in this range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLow,
                )
            }
        } else {
            RatingChart(points, accent)
        }
    }
}

@Composable
private fun RatingChart(points: List<RatingPointDto>, accent: Color) {
    val measurer = rememberTextMeasurer()
    val values = points.map { it.score ?: 0.0 }
    val sMin = values.min()
    val sMax = values.max()
    val lo = floor((sMin - 30.0) / 100.0) * 100.0
    val hi = ceil((sMax + 30.0) / 100.0) * 100.0
    val span = (hi - lo).coerceAtLeast(1.0)
    val gridStep = when {
        span <= 600.0 -> 100.0
        span <= 1200.0 -> 200.0
        else -> 300.0
    }
    val gridScores = buildList {
        var v = lo
        while (v <= hi + 0.5) { add(v); v += gridStep }
    }
    val labelEvery = maxOf(1, points.size / 6)
    val axisStyle = TextStyle(color = TextLow, fontSize = 10.sp)
    val xStyle = TextStyle(color = TextLow, fontSize = 9.sp)

    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val leftPad = 30.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 6.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad
        val baseline = topPad + plotH

        fun xAt(i: Int): Float =
            if (points.size == 1) leftPad + plotW / 2f
            else leftPad + plotW * i / (points.size - 1)

        fun yAt(score: Double): Float = topPad + (1f - ((score - lo) / span).toFloat()) * plotH

        // Y grid lines + rank-tier labels
        gridScores.forEach { gs ->
            val y = yAt(gs)
            drawLine(Outline, Offset(leftPad, y), Offset(leftPad + plotW, y), strokeWidth = 0.5.dp.toPx())
            val lay = measurer.measure(scoreLabel(gs), axisStyle)
            drawText(
                lay,
                topLeft = Offset(leftPad - 6.dp.toPx() - lay.size.width, y - lay.size.height / 2f),
            )
        }

        // Gradient fill under the line
        val fill = Path().apply {
            moveTo(xAt(0), yAt(values[0]))
            for (i in 1 until values.size) lineTo(xAt(i), yAt(values[i]))
            lineTo(xAt(values.size - 1), baseline)
            lineTo(xAt(0), baseline)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.25f), Color.Transparent),
                startY = topPad,
                endY = baseline,
            ),
        )

        // The line itself
        val line = Path().apply {
            moveTo(xAt(0), yAt(values[0]))
            for (i in 1 until values.size) lineTo(xAt(i), yAt(values[i]))
        }
        drawPath(line, color = accent, style = Stroke(width = 2.dp.toPx()))

        // Dots per race
        val r = 2.2.dp.toPx()
        values.forEachIndexed { i, s -> drawCircle(accent, r, Offset(xAt(i), yAt(s))) }

        // X date labels (~6, evenly spaced)
        points.indices.forEach { i ->
            if (i % labelEvery == 0) {
                points[i].date?.let(::shortDate)?.let { d ->
                    val lay = measurer.measure(d, xStyle)
                    val maxX = (leftPad + plotW - lay.size.width).coerceAtLeast(leftPad)
                    val cx = (xAt(i) - lay.size.width / 2f).coerceIn(leftPad, maxX)
                    drawText(lay, topLeft = Offset(cx, baseline + 4.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun TogglePill(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextMed,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Surface3 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextHigh else TextLow,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
