package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.LmuTheme
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.material_symbols_outlined_rank_adjustment
import org.jetbrains.compose.resources.Font

private val RankAdjustmentUp = Color(0xFF57C77A)
private val RankAdjustmentDown = Color(0xFFE2231A)

private data class RankAdjustmentIconMetrics(
    val width: Dp,
    val height: Dp,
    val fontSize: TextUnit,
    val doubleGlyphVerticalOffset: Dp,
)

/**
 * RaceOS rank movement rendered with Google's Material Symbols Outlined glyphs.
 *
 * The bundled font is a static 24/400/FILL=0/GRAD=0 subset of Google's official
 * Material Symbols font. Level four intentionally overlays two level-two glyphs,
 * matching the web client. Negative adjustments rotate the complete mark downward.
 */
@Composable
fun RankAdjustmentIcon(adjustment: Int, compact: Boolean) {
    if (adjustment == 0) return

    val level = kotlin.math.abs(adjustment).coerceIn(1, 4)
    val metrics = if (compact) {
        RankAdjustmentIconMetrics(
            width = 18.dp,
            height = 13.dp,
            fontSize = 18.sp,
            doubleGlyphVerticalOffset = 2.dp,
        )
    } else {
        RankAdjustmentIconMetrics(
            width = 34.dp,
            height = 16.dp,
            fontSize = 22.sp,
            doubleGlyphVerticalOffset = 2.5.dp,
        )
    }
    val color = if (adjustment > 0) RankAdjustmentUp else RankAdjustmentDown
    val symbolFont = FontFamily(Font(Res.font.material_symbols_outlined_rank_adjustment))
    val glyphBoxSize = with(LocalDensity.current) { metrics.fontSize.toDp() }

    Box(
        modifier = Modifier
            .width(metrics.width)
            .height(metrics.height)
            .then(if (adjustment < 0) Modifier.rotate(180f) else Modifier)
            .clearAndSetSemantics {
                contentDescription = if (adjustment > 0) "+$level" else "-$level"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (level == 4) {
            MaterialSymbol(
                name = "keyboard_double_arrow_up",
                fontFamily = symbolFont,
                fontSize = metrics.fontSize,
                glyphBoxSize = glyphBoxSize,
                color = color,
                modifier = Modifier.offset(y = -metrics.doubleGlyphVerticalOffset),
            )
            MaterialSymbol(
                name = "keyboard_double_arrow_up",
                fontFamily = symbolFont,
                fontSize = metrics.fontSize,
                glyphBoxSize = glyphBoxSize,
                color = color,
                modifier = Modifier.offset(y = metrics.doubleGlyphVerticalOffset),
            )
        } else {
            MaterialSymbol(
                name = when (level) {
                    1 -> "keyboard_control_key"
                    2 -> "keyboard_double_arrow_up"
                    else -> "stat_3"
                },
                fontFamily = symbolFont,
                fontSize = metrics.fontSize,
                glyphBoxSize = glyphBoxSize,
                color = color,
            )
        }
    }
}

@Composable
private fun MaterialSymbol(
    name: String,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    glyphBoxSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier.requiredSize(glyphBoxSize),
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = fontSize,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Visible,
    )
}

@Preview
@Composable
private fun RankAdjustmentIconPreview() {
    LmuTheme {
        Column(
            modifier = Modifier.background(Carbon).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                (1..4).forEach { RankAdjustmentIcon(it, compact = false) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                (1..4).forEach { RankAdjustmentIcon(-it, compact = false) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                (1..4).forEach { RankAdjustmentIcon(it, compact = true) }
                (1..4).forEach { RankAdjustmentIcon(-it, compact = true) }
            }
        }
    }
}
