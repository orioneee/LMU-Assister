package com.orioooneee.lmuasister.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.ui.WeekTab
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.components.HeroRaceCard
import com.orioooneee.lmuasister.ui.components.EqualHeightRaceRow
import com.orioooneee.lmuasister.ui.components.SectionHeader
import com.orioooneee.lmuasister.ui.components.rememberGridColumns
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlin.time.Clock
import kotlinx.coroutines.launch

private fun tierRank(difficulty: String): Int = when {
    "rookie" in difficulty.lowercase() -> 0
    "beginner" in difficulty.lowercase() -> 1
    "intermediate" in difficulty.lowercase() -> 2
    "advanced" in difficulty.lowercase() -> 3
    "pro" in difficulty.lowercase() -> 4
    else -> 5
}

@Composable
fun HomeScreen(
    schedule: Schedule,
    weeks: List<WeekTab>,
    selectedWeek: String,
    onSelectWeek: (String) -> Unit,
    onOpenRace: (Race) -> Unit,
) {
    val all = schedule.races
    val tiers = remember(all) {
        all.map { it.difficulty.ifBlank { "Other" } }.distinct().sortedBy { tierRank(it) }
    }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val heroHeight = with(density) { windowInfo.containerSize.height.toDp() / 3 }.coerceIn(180.dp, 300.dp)

    Column(Modifier.fillMaxSize().background(Carbon)) {
        HomeHeader(dailyCount = all.size, modifier = Modifier.padding(16.dp))

        if (weeks.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weeks.forEach { w -> WeekPill(w.label, w.key == selectedWeek) { onSelectWeek(w.key) } }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (tiers.isEmpty()) {
            EmptyHint()
            return@Column
        }

        val pager = rememberPagerState(pageCount = { tiers.size })
        val scope = rememberCoroutineScope()

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tiers.forEachIndexed { i, tier ->
                TierTab(tier, pager.currentPage == i) {
                    scope.launch { pager.animateScrollToPage(i) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val tier = tiers[page]
            TierPage(
                tier = tier,
                races = all.filter { it.difficulty.ifBlank { "Other" } == tier },
                heroHeight = heroHeight,
                onOpenRace = onOpenRace,
            )
        }
    }
}

@Composable
private fun TierPage(tier: String, races: List<Race>, heroHeight: Dp, onOpenRace: (Race) -> Unit) {
    val now = remember { Clock.System.now() }
    val sorted = remember(races) {
        races.sortedBy { it.nextStart(now)?.toEpochMilliseconds() ?: Long.MAX_VALUE }
    }
    val cols = rememberGridColumns()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sorted.firstOrNull()?.let { hero ->
            item { HeroRaceCard(hero, maxHeight = heroHeight) { onOpenRace(hero) } }
            item { SectionHeader("All ${tier.lowercase()} races") }
            items(sorted.chunked(cols)) { row -> EqualHeightRaceRow(row, cols, onOpenRace) }
        }
        if (sorted.isEmpty()) {
            item { EmptyHint() }
        }
    }
}

@Composable
private fun TierTab(tier: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Surface1
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else TextMed
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(tier, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
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
private fun HomeHeader(dailyCount: Int, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IconFlag, contentDescription = null, tint = Carbon, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Lmu Assister", style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Black)
            Text("Race Control · $dailyCount events", style = MaterialTheme.typography.bodySmall, color = TextMed)
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Surface1).border(1.dp, Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🏁", style = MaterialTheme.typography.titleMedium, color = TextLow)
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Text(
            "No races parsed.\nPull to refresh or check the schedule tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextLow,
        )
    }
}
