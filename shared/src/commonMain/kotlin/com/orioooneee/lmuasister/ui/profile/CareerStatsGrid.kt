package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.orioooneee.lmuasister.ui.IconChevronRight
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassLmp3
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.theme.Lime
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.SkillBeginner
import com.orioooneee.lmuasister.ui.theme.SkillInter
import com.orioooneee.lmuasister.ui.theme.SkillPro
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow

private val StatCyan = Color(0xFF6FE3F0)

/** Stat categories that have a drill-down list endpoint (GET /profile/races/<key>).
 *  [title] is the plural counter label (profile tiles); [chip] is the singular per-race label
 *  (race-detail chips). [color] is the shared accent used by both. */
// Declaration order mirrors the profile stat tiles, so chips sorted by ordinal match that layout.
enum class StatCategory(val key: String, val title: String, val chip: String, val color: Color) {
    GrandSlam("grand_slam", "Grand Slam", "Grand Slam", Lime),
    Wins("wins", "Wins", "Win", SkillInter),
    Poles("poles", "Poles", "Pole", StatCyan),
    Podiums("podiums", "Podiums", "Podium", Amber),
    FastestLaps("fastest_laps", "Fastest laps", "Fastest lap", ClassMixed),
    Top5("top5", "Top 5", "Top 5", ClassLmp2),
    PolesConverted("poles_converted", "Poles converted", "Pole converted", ClassGt3),
    WinsNoPole("wins_no_pole", "Wins without pole", "Win without pole", ClassLmp3),
    Dnf("dnf", "DNF", "DNF", SkillPro);

    companion object {
        fun byKey(key: String): StatCategory? = entries.firstOrNull { it.key == key }
    }
}

private data class StatCell(
    val label: String,
    val value: Int,
    val color: Color,
    val category: StatCategory? = null,
    // Overrides the big number with a custom string (e.g. a percentage). null → show [value].
    val display: String? = null,
)

/** Whole-percent share, e.g. 5 of 13 → "38%". "—" when there's nothing to divide by. */
private fun pctOf(part: Int, whole: Int): String =
    if (whole <= 0) "—" else "${(part * 100 + whole / 2) / whole}%"

private const val COLUMNS = 3

/** "Stats racecontrol" grid: career counters as colored cells, 3 per row. Cells backed by a
 *  drill-down endpoint are clickable (accent border + chevron) and open the category list. */
@Composable
fun CareerStatsGrid(
    totals: StatTotalsDto,
    modifier: Modifier = Modifier,
    enableCategoryClicks: Boolean = true,
    onOpenCategory: (StatCategory) -> Unit = {},
) {
    val cells = listOf(
        // Static counters first…
        StatCell("RACES", totals.races, TextHigh),
        StatCell("LAPS", totals.lapsCompleted, TextHigh),
        StatCell("LAPS LED", totals.lapsLead, SkillBeginner),
        // …then every drill-down tile, DNF last.
        StatCell("GRAND SLAM", totals.grandSlams, Lime, StatCategory.GrandSlam),
        StatCell("WINS", totals.wins, SkillInter, StatCategory.Wins),
        StatCell("POLES", totals.polePositions, StatCyan, StatCategory.Poles),
        StatCell("PODIUMS", totals.podiums, Amber, StatCategory.Podiums),
        StatCell("FAST LAPS", totals.fastestLaps, ClassMixed, StatCategory.FastestLaps),
        StatCell("TOP 5", totals.top5, ClassLmp2, StatCategory.Top5),
        StatCell("POLE→WIN", totals.polesConverted, ClassGt3, StatCategory.PolesConverted, display = pctOf(totals.polesConverted, totals.polePositions)),
        StatCell("WIN W/O POLE", totals.winsNoPole, ClassLmp3, StatCategory.WinsNoPole),
        StatCell("DNF", totals.dnfs, SkillPro, StatCategory.Dnf),
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
                    StatTile(cell, Modifier.weight(1f), enableCategoryClicks, onOpenCategory)
                }
                // Pad the final row so cells keep a consistent width.
                repeat(COLUMNS - rowCells.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatTile(
    cell: StatCell,
    modifier: Modifier = Modifier,
    enableCategoryClicks: Boolean,
    onOpenCategory: (StatCategory) -> Unit,
) {
    val clickable = enableCategoryClicks && cell.category != null
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(Surface2)
            // Accent border in the cell's own color marks a tile as tappable.
            .then(if (clickable) Modifier.border(1.dp, cell.color.copy(alpha = 0.55f), shape) else Modifier)
            .then(if (clickable) Modifier.clickable { onOpenCategory(cell.category) } else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                cell.display ?: cell.value.toString(),
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
        if (clickable) {
            Icon(
                IconChevronRight,
                contentDescription = null,
                tint = cell.color.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).size(16.dp),
            )
        }
    }
}
