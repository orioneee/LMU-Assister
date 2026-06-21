package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.remote.StatTotalsDto
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.SkillBeginner
import com.orioooneee.lmuasister.ui.theme.SkillInter
import com.orioooneee.lmuasister.ui.theme.SkillPro
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow

private val StatCyan = Color(0xFF6FE3F0)

private data class StatCell(val label: String, val value: Int, val color: Color)

private const val COLUMNS = 3

/** "Stats racecontrol" grid: career counters as colored cells, 3 per row. */
@Composable
fun CareerStatsGrid(totals: StatTotalsDto, modifier: Modifier = Modifier) {
    val cells = listOf(
        StatCell("RACES", totals.races, TextHigh),
        StatCell("WINS", totals.wins, SkillInter),
        StatCell("PODIUMS", totals.podiums, Amber),
        StatCell("POLES", totals.polePositions, StatCyan),
        StatCell("FAST LAPS", totals.fastestLaps, ClassMixed),
        StatCell("LAPS LED", totals.lapsLead, SkillBeginner),
        StatCell("TOP 5", totals.top5, ClassLmp2),
        StatCell("DNF", totals.dnfs, SkillPro),
        StatCell("LAPS", totals.lapsCompleted, TextHigh),
    )

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cells.chunked(COLUMNS).forEach { rowCells ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowCells.forEach { cell ->
                    StatTile(cell, Modifier.weight(1f))
                }
                // Pad the final row so cells keep a consistent width.
                repeat(COLUMNS - rowCells.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatTile(cell: StatCell, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            cell.value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = cell.color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            cell.label,
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
        )
    }
}
