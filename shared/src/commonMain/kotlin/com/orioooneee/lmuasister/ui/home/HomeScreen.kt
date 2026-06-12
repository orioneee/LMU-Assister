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
import com.orioooneee.lmuasister.data.model.RaceType
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
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.app_name
import lmuassister.shared.generated.resources.home_empty
import lmuassister.shared.generated.resources.home_events
import lmuassister.shared.generated.resources.section_daily
import lmuassister.shared.generated.resources.section_weekly
import lmuassister.shared.generated.resources.tab_championship
import lmuassister.shared.generated.resources.tab_special
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    schedule: Schedule,
    weeks: List<WeekTab>,
    selectedWeek: String,
    onSelectWeek: (String) -> Unit,
    onOpenRace: (Race) -> Unit,
) {
    val tabs = remember(schedule) { buildTabs(schedule) }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val heroHeight = with(density) { windowInfo.containerSize.height.toDp() / 3 }.coerceIn(180.dp, 300.dp)

    Column(Modifier.fillMaxSize().background(Carbon)) {
        HomeHeader(dailyCount = schedule.races.size, modifier = Modifier.padding(16.dp))

        if (weeks.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weeks.forEach { w -> WeekPill(w.label, w.key == selectedWeek) { onSelectWeek(w.key) } }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (tabs.isEmpty()) {
            EmptyHint()
            return@Column
        }

        val pager = rememberPagerState(pageCount = { tabs.size })
        val scope = rememberCoroutineScope()

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { i, tab ->
                TierTab(tab.label(), pager.currentPage == i) {
                    scope.launch { pager.animateScrollToPage(i) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            TabContent(tabs[page], schedule, heroHeight, onOpenRace)
        }
    }
}

private val TIER_ORDER = listOf("Beginner", "Intermediate", "Advanced")

private sealed interface HomeTab
private data class Tier(val name: String) : HomeTab
private data object SpecialTab : HomeTab
private data object ChampTab : HomeTab

@Composable
private fun HomeTab.label(): String = when (this) {
    is Tier -> name
    SpecialTab -> stringResource(Res.string.tab_special)
    ChampTab -> stringResource(Res.string.tab_championship)
}

private fun buildTabs(schedule: Schedule): List<HomeTab> = buildList {
    TIER_ORDER.forEach { t ->
        val hasTier = schedule.races.any {
            (it.type == RaceType.DAILY || it.type == RaceType.WEEKLY) && it.difficulty.equals(t, ignoreCase = true)
        }
        if (hasTier) add(Tier(t))
    }
    if (schedule.special.isNotEmpty()) add(SpecialTab)
    if (schedule.championship.isNotEmpty()) add(ChampTab)
}

private data class Section(val label: String?, val races: List<Race>)

@Composable
private fun TabContent(tab: HomeTab, schedule: Schedule, heroHeight: Dp, onOpenRace: (Race) -> Unit) {
    val sections: List<Section> = when (tab) {
        is Tier -> listOf(
            Section(stringResource(Res.string.section_daily), schedule.daily.filter { it.difficulty.equals(tab.name, true) }),
            Section(stringResource(Res.string.section_weekly), schedule.weekly.filter { it.difficulty.equals(tab.name, true) }),
        ).filter { it.races.isNotEmpty() }
        SpecialTab -> listOf(Section(null, schedule.special))
        ChampTab -> listOf(Section(null, schedule.championship))
    }

    val now = remember { Clock.System.now() }
    fun nextKey(r: Race) = r.nextStart(now)?.toEpochMilliseconds() ?: Long.MAX_VALUE
    val all = sections.flatMap { it.races }
    val cols = rememberGridColumns()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            all.isEmpty() -> item { EmptyHint() }

            // single event on this tab → just one bigger hero, no list
            all.size == 1 -> item {
                HeroRaceCard(all.first(), maxHeight = heroHeight * 1.5f) { onOpenRace(all.first()) }
            }

            else -> {
                val hero = all.minByOrNull { nextKey(it) }!!
                item { HeroRaceCard(hero, maxHeight = heroHeight) { onOpenRace(hero) } }
                sections.forEach { sec ->
                    val sorted = sec.races.sortedBy { nextKey(it) }
                    sec.label?.let { lbl -> item { SectionHeader(lbl) } }
                    items(sorted.chunked(cols)) { row -> EqualHeightRaceRow(row, cols, onOpenRace) }
                }
            }
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
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Black)
            Text(stringResource(Res.string.home_events, dailyCount), style = MaterialTheme.typography.bodySmall, color = TextMed)
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
            stringResource(Res.string.home_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = TextLow,
        )
    }
}
