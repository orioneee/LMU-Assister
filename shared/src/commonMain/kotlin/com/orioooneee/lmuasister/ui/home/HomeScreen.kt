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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.data.model.ScheduleCategory
import com.orioooneee.lmuasister.ui.IconCoffee
import com.orioooneee.lmuasister.ui.IconGithub
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
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.rememberNow
import kotlin.time.Instant
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.action_refresh
import lmuassister.shared.generated.resources.empty_subtitle
import lmuassister.shared.generated.resources.empty_title
import lmuassister.shared.generated.resources.error_title
import lmuassister.shared.generated.resources.retry
import lmuassister.shared.generated.resources.section_daily
import lmuassister.shared.generated.resources.section_weekly
import lmuassister.shared.generated.resources.tab_championship
import lmuassister.shared.generated.resources.tab_races
import lmuassister.shared.generated.resources.tab_special
import org.jetbrains.compose.resources.stringResource

private const val REPO_URL = "https://github.com/orioneee/LMU-Assister"
private const val JAR_URL = "https://send.monobank.ua/jar/A13DDkcaK5"

@Composable
fun HomeScreen(
    schedule: Schedule,
    weeks: List<WeekTab>,
    selectedWeek: String,
    selectedCategory: ScheduleCategory,
    loading: Boolean,
    errorMessage: String?,
    insets: PaddingValues,
    onSelectWeek: (String) -> Unit,
    onSelectCategory: (ScheduleCategory) -> Unit,
    onOpenRace: (Race) -> Unit,
    onOpenScheduleUpdates: () -> Unit,
    onRefresh: () -> Unit = {},
    cars: List<CarModel> = emptyList(),
    showTimerInScheduleCard: Boolean = false,
) {
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()
    val now = rememberNow()

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val heroHeight = with(density) { windowInfo.containerSize.height.toDp() / 3 }.coerceIn(180.dp, 300.dp)
    val uriHandler = LocalUriHandler.current
    val openRepo = { uriHandler.openUri(REPO_URL) }
    val openJar = { uriHandler.openUri(JAR_URL) }

    val isCurrentWeek = weeks.isEmpty() || selectedWeek == weeks.first().key
    val useTrackBackgroundCover = !isCurrentWeek

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
                return Offset(0f, new - old)
            }
        }
    }

    Box(Modifier.fillMaxSize().clipToBounds().background(Carbon).nestedScroll(connection)) {
        Box(Modifier.fillMaxSize().offset { IntOffset(0, (headerHeight.value + headerOffset.value).roundToInt()) }) {
            if (loading) {
                LoadingScheduleContent(bottomInset)
            } else {
                TabContent(
                    selectedCategory,
                    schedule,
                    errorMessage,
                    heroHeight,
                    isCurrentWeek,
                    now,
                    bottomInset,
                    onOpenRace,
                    onRefresh,
                    showTimerInScheduleCard,
                    useTrackBackgroundCover,
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, headerOffset.value.roundToInt()) }
                .onSizeChanged { headerHeight.value = it.height.toFloat() }
                .background(Carbon),
        ) {
            Spacer(Modifier.height(topInset + 12.dp))
            ScheduleHeaderControls(
                weeks = weeks,
                selectedWeek = selectedWeek,
                selectedCategory = selectedCategory,
                onSelectWeek = onSelectWeek,
                onSelectCategory = onSelectCategory,
                onOpenScheduleUpdates = onOpenScheduleUpdates,
                onOpenRepo = openRepo,
                onOpenJar = openJar,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ScheduleHeaderControls(
    weeks: List<WeekTab>,
    selectedWeek: String,
    selectedCategory: ScheduleCategory,
    onSelectWeek: (String) -> Unit,
    onSelectCategory: (ScheduleCategory) -> Unit,
    onOpenScheduleUpdates: () -> Unit,
    onOpenRepo: () -> Unit,
    onOpenJar: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (weeks.size > 1) {
                WeekPillsRow(
                    weeks = weeks,
                    selectedWeek = selectedWeek,
                    onSelectWeek = onSelectWeek,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            GithubButton(onOpenRepo)
        }
        TierTabsRow(
            tabs = ScheduleCategory.entries.toList(),
            selectedCategory = selectedCategory,
            onSelect = onSelectCategory,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScheduleUpdatesChip(onOpenScheduleUpdates, Modifier.weight(1f))
            CoffeeChip(onOpenJar)
        }
    }
}

@Composable
private fun ScheduleUpdatesChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = TextMed, modifier = Modifier.size(17.dp))
        Text(
            "Schedule updates",
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CoffeeChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Amber.copy(alpha = 0.14f))
            .border(1.dp, Amber.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(IconCoffee, contentDescription = null, tint = Amber, modifier = Modifier.size(17.dp))
        Text(
            "Buy me a coffee",
            style = MaterialTheme.typography.labelMedium,
            color = Amber,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun GithubButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Surface1)
            .border(1.dp, Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(IconGithub, contentDescription = "Open GitHub repository", tint = TextMed, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TierTabsRow(
    tabs: List<ScheduleCategory>,
    selectedCategory: ScheduleCategory,
    onSelect: (ScheduleCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            TierTab(tab.label(), selectedCategory == tab) { onSelect(tab) }
        }
    }
}

@Composable
private fun WeekPillsRow(
    weeks: List<WeekTab>,
    selectedWeek: String,
    onSelectWeek: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        weeks.forEach { w -> WeekPill(w.label, w.key == selectedWeek) { onSelectWeek(w.key) } }
    }
}

@Composable
private fun ScheduleCategory.label(): String = when (this) {
    ScheduleCategory.RACES -> stringResource(Res.string.tab_races)
    ScheduleCategory.SPECIAL -> stringResource(Res.string.tab_special)
    ScheduleCategory.CHAMPIONSHIP -> stringResource(Res.string.tab_championship)
}

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
    tab: ScheduleCategory,
    schedule: Schedule,
    errorMessage: String?,
    heroHeight: Dp,
    isCurrentWeek: Boolean,
    now: Instant,
    bottomInset: Dp,
    onOpenRace: (Race) -> Unit,
    onRefresh: () -> Unit,
    showTimerInScheduleCard: Boolean,
    useTrackBackgroundCover: Boolean,
) {
    val sections: List<Section> = when (tab) {
        ScheduleCategory.RACES -> listOf(
            Section(stringResource(Res.string.section_daily), schedule.daily),
            Section(stringResource(Res.string.section_weekly), schedule.weekly),
        )
        ScheduleCategory.SPECIAL -> listOf(Section(null, schedule.special))
        ScheduleCategory.CHAMPIONSHIP -> listOf(Section(null, schedule.championship))
    }
        .filter { it.races.isNotEmpty() }

    fun nextKey(r: Race) = r.nextStart(now)?.toEpochMilliseconds() ?: Long.MAX_VALUE
    val all = sections.flatMap { it.races }
    val cols = rememberGridColumns()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            errorMessage != null -> item { ScheduleError(Modifier.fillParentMaxSize(), errorMessage, onRefresh) }

            all.isEmpty() -> item { NoRaces(Modifier.fillParentMaxSize(), onRefresh) }

            all.size == 1 -> {
                val race = all.first()
                item {
                    HeroRaceTimesCard(
                        race,
                        heroHeight = heroHeight,
                        showCountdown = isCurrentWeek,
                        showTimer = showTimerInScheduleCard,
                        useTrackBackgroundCover = useTrackBackgroundCover,
                    ) {
                        onOpenRace(race)
                    }
                }
            }

            else -> {
                sections.forEach { sec ->
                    val sorted = sec.races.sortedWith(
                        compareBy({ srRank(it.settings.safetyRank) }, { nextKey(it) }),
                    )
                    sec.label?.let { lbl -> item { SectionHeader(lbl) } }
                    items(sorted.chunked(cols)) { row ->
                        EqualHeightRaceRow(
                            row,
                            cols,
                            onOpenRace,
                            showCountdown = isCurrentWeek,
                            showTimer = showTimerInScheduleCard,
                            useTrackBackgroundCover = useTrackBackgroundCover,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleError(modifier: Modifier = Modifier, message: String, onRefresh: () -> Unit) {
    EmptyState(
        title = stringResource(Res.string.error_title),
        subtitle = message,
        accent = MaterialTheme.colorScheme.error,
        actionLabel = stringResource(Res.string.retry),
        onAction = onRefresh,
        modifier = modifier,
    )
}

@Composable
private fun LoadingScheduleContent(bottomInset: Dp) {
    Box(
        Modifier.fillMaxSize().padding(bottom = bottomInset),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
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
