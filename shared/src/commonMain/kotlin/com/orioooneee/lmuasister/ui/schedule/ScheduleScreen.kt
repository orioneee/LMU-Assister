package com.orioooneee.lmuasister.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceType
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.ui.WeekTab
import com.orioooneee.lmuasister.ui.components.EqualHeightRaceRow
import com.orioooneee.lmuasister.ui.components.rememberGridColumns
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlin.time.Clock

private fun tierRank(difficulty: String): Int = when {
    "rookie" in difficulty.lowercase() -> 0
    "beginner" in difficulty.lowercase() -> 1
    "intermediate" in difficulty.lowercase() -> 2
    "advanced" in difficulty.lowercase() -> 3
    "pro" in difficulty.lowercase() -> 4
    else -> 5
}

@Composable
fun ScheduleScreen(
    schedule: Schedule,
    weeks: List<WeekTab>,
    selectedWeek: String,
    onSelectWeek: (String) -> Unit,
    onOpenRace: (Race) -> Unit,
) {
    var category by remember { mutableStateOf(RaceType.DAILY) }
    val now = remember { Clock.System.now() }

    val races = when (category) {
        RaceType.DAILY -> schedule.daily
        RaceType.WEEKLY -> schedule.weekly
        RaceType.SPECIAL -> schedule.special
        RaceType.CHAMPIONSHIP -> schedule.championship
    }

    val groups = races
        .groupBy { it.difficulty.ifBlank { "Other" } }
        .toList()
        .sortedBy { tierRank(it.first) }
        .map { (tier, list) ->
            tier to list.sortedBy { r -> r.nextStart(now)?.toEpochMilliseconds() ?: Long.MAX_VALUE }
        }

    val cols = rememberGridColumns()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Carbon),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Race Control", style = MaterialTheme.typography.headlineMedium, color = TextHigh, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text("${category.label} · ${races.size} events", style = MaterialTheme.typography.bodyMedium, color = TextMed)
                if (weeks.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        weeks.forEach { w -> WeekPill(w.label, w.key == selectedWeek) { onSelectWeek(w.key) } }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RaceType.entries.forEach { type ->
                        CategoryPill(type.label, category == type) { category = type }
                    }
                }
            }
        }

        groups.forEach { (tier, list) ->
            item { TierHeader(tier, list.size) }
            items(list.chunked(cols)) { row -> EqualHeightRaceRow(row, cols, onOpenRace) }
        }

        if (races.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("No ${category.label.lowercase()} events.", color = TextLow, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TierHeader(tier: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(Modifier.size(width = 4.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(Amber))
        Spacer(Modifier.width(8.dp))
        Text(tier.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = TextLow)
    }
}

@Composable
private fun WeekPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.secondary
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else Surface1)
            .border(1.dp, if (selected) accent else Outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) accent else TextMed, maxLines = 1)
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Surface1
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else TextMed
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}
