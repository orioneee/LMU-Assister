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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceType
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.ui.WeekTab
import com.orioooneee.lmuasister.ui.components.EmptyState
import com.orioooneee.lmuasister.ui.components.HeroRaceTimesCard
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
import kotlin.time.Instant
import kotlinx.coroutines.launch
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.action_refresh
import lmuassister.shared.generated.resources.empty_subtitle
import lmuassister.shared.generated.resources.empty_title
import lmuassister.shared.generated.resources.section_daily
import lmuassister.shared.generated.resources.section_weekly
import lmuassister.shared.generated.resources.tab_championship
import lmuassister.shared.generated.resources.tab_races
import lmuassister.shared.generated.resources.tab_special
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    schedule: Schedule,
    weeks: List<WeekTab>,
    selectedWeek: String,
    onSelectWeek: (String) -> Unit,
    onOpenRace: (Race) -> Unit,
    onRefresh: () -> Unit = {},
    cars: List<CarModel> = emptyList(),
) {
    val now = remember { Clock.System.now() }
    val tabs = remember(schedule) { buildTabs(schedule, now) }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val heroHeight = with(density) { windowInfo.containerSize.height.toDp() / 3 }.coerceIn(180.dp, 300.dp)

    // No upcoming races at all → just the (optional) week pills + empty state.
    if (tabs.isEmpty()) {
        Column(Modifier.fillMaxSize().background(Carbon)) {
            Spacer(Modifier.height(12.dp))
            if (weeks.size > 1) {
                WeekPillsRow(weeks, selectedWeek, onSelectWeek)
                Spacer(Modifier.height(12.dp))
            }
            NoRaces(Modifier.weight(1f), onRefresh)
        }
        return
    }

    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val isCurrentWeek = weeks.isEmpty() || selectedWeek == weeks.first().key

    // Collapsing header: the week pills + tier tabs translate up with the list as it
    // scrolls (enter-always), instead of staying pinned at the top.
    val headerHeight = remember { mutableStateOf(0f) }
    val headerOffset = remember { mutableStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val h = headerHeight.value
                if (h <= 0f) return Offset.Zero
                val old = headerOffset.value
                val new = (old + available.y).coerceIn(-h, 0f)
                headerOffset.value = new
                return Offset(0f, new - old) // consume the part used to move the header
            }
        }
    }

    Box(Modifier.fillMaxSize().clipToBounds().background(Carbon).nestedScroll(connection)) {
        // Pager content, pushed down by the currently-visible header height.
        Box(Modifier.fillMaxSize().offset { IntOffset(0, (headerHeight.value + headerOffset.value).roundToInt()) }) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                TabContent(tabs[page], schedule, heroHeight, isCurrentWeek, now, onOpenRace, onRefresh)
            }
        }
        // The header itself, translating up as the user scrolls.
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, headerOffset.value.roundToInt()) }
                .onSizeChanged { headerHeight.value = it.height.toFloat() }
                .background(Carbon),
        ) {
            Spacer(Modifier.height(12.dp))
            if (weeks.size > 1) {
                WeekPillsRow(weeks, selectedWeek, onSelectWeek)
                Spacer(Modifier.height(12.dp))
            }
            // Tabs only for Special / Championship — the main Daily+Weekly grid needs none.
            if (tabs.size > 1) {
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
            }
        }
    }
}

@Composable
private fun WeekPillsRow(weeks: List<WeekTab>, selectedWeek: String, onSelectWeek: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        weeks.forEach { w -> WeekPill(w.label, w.key == selectedWeek) { onSelectWeek(w.key) } }
    }
}

private sealed interface HomeTab
private data object MainTab : HomeTab
private data object SpecialTab : HomeTab
private data object ChampTab : HomeTab

@Composable
private fun HomeTab.label(): String = when (this) {
    MainTab -> stringResource(Res.string.tab_races)
    SpecialTab -> stringResource(Res.string.tab_special)
    ChampTab -> stringResource(Res.string.tab_championship)
}

private fun buildTabs(schedule: Schedule, now: Instant): List<HomeTab> = buildList {
    val hasMain = schedule.races.any {
        (it.type == RaceType.DAILY || it.type == RaceType.WEEKLY) && it.nextStart(now) != null
    }
    if (hasMain) add(MainTab)
    if (schedule.special.any { it.nextStart(now) != null }) add(SpecialTab)
    if (schedule.championship.any { it.nextStart(now) != null }) add(ChampTab)
}

/** SR tier order for sorting: bronze(0) → silver(1) → gold(2) → platinum(3) → unranked(4). */
private fun srRank(sr: String?): Int = when (sr?.trim()?.firstOrNull()?.lowercaseChar()) {
    'b' -> 0
    's' -> 1
    'g' -> 2
    'p' -> 3
    else -> 4
}

private data class Section(val label: String?, val races: List<Race>)

@Composable
private fun TabContent(
    tab: HomeTab,
    schedule: Schedule,
    heroHeight: Dp,
    isCurrentWeek: Boolean,
    now: Instant,
    onOpenRace: (Race) -> Unit,
    onRefresh: () -> Unit,
) {
    val sections: List<Section> = when (tab) {
        MainTab -> listOf(
            Section(stringResource(Res.string.section_daily), schedule.daily),
            Section(stringResource(Res.string.section_weekly), schedule.weekly),
        )
        SpecialTab -> listOf(Section(null, schedule.special))
        ChampTab -> listOf(Section(null, schedule.championship))
    }
        // Only show races with an upcoming start — hide finished weeks/seasons and
        // anything with no start times.
        .map { sec -> sec.copy(races = sec.races.filter { it.nextStart(now) != null }) }
        .filter { it.races.isNotEmpty() }

    fun nextKey(r: Race) = r.nextStart(now)?.toEpochMilliseconds() ?: Long.MAX_VALUE
    val all = sections.flatMap { it.races }
    val cols = rememberGridColumns()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            all.isEmpty() -> item { NoRaces(Modifier.fillParentMaxSize(), onRefresh) }

            // single event on this tab → hero + its start times in one card
            all.size == 1 -> {
                val race = all.first()
                item {
                    HeroRaceTimesCard(race, heroHeight = heroHeight, showCountdown = isCurrentWeek) {
                        onOpenRace(race)
                    }
                }
            }

            else -> {
                // Each schedule runs independently (Beginner/Intermediate/Advanced daily,
                // and weekly), so "NEXT" is the soonest race PER (type + difficulty) group.
                val nextRaceIds: Set<String> = if (isCurrentWeek) {
                    all.groupBy { it.type to it.difficulty.lowercase() }
                        .values.mapNotNull { g -> g.minByOrNull { nextKey(it) }?.id }
                        .toSet()
                } else {
                    emptySet()
                }
                sections.forEach { sec ->
                    // group by SR tier (bronze → silver → gold → platinum), each by start time
                    val sorted = sec.races.sortedWith(
                        compareBy({ srRank(it.settings.safetyRank) }, { nextKey(it) }),
                    )
                    sec.label?.let { lbl -> item { SectionHeader(lbl) } }
                    items(sorted.chunked(cols)) { row ->
                        EqualHeightRaceRow(row, cols, onOpenRace, showCountdown = isCurrentWeek, nextRaceIds = nextRaceIds)
                    }
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
private fun NoRaces(modifier: Modifier = Modifier, onRefresh: () -> Unit) {
    EmptyState(
        title = stringResource(Res.string.empty_title),
        subtitle = stringResource(Res.string.empty_subtitle),
        actionLabel = stringResource(Res.string.action_refresh),
        onAction = onRefresh,
        modifier = modifier,
    )
}
