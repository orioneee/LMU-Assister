package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.remote.AvailableCarDto
import com.orioooneee.lmuasister.data.remote.ClassificationRowDto
import com.orioooneee.lmuasister.data.remote.LapDto
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.RaceSessionDetailDto
import com.orioooneee.lmuasister.data.remote.RatingDto
import com.orioooneee.lmuasister.data.remote.ReasonDto
import com.orioooneee.lmuasister.data.remote.TrackDto
import com.orioooneee.lmuasister.ui.TrackLogoIndex
import com.orioooneee.lmuasister.ui.publicusers.PublicUsersViewModel
import com.orioooneee.lmuasister.ui.tracks.TrackPreview
import com.orioooneee.lmuasister.ui.components.BlockSkeleton
import com.orioooneee.lmuasister.ui.components.LeaderboardRowSkeleton
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.components.classColorFor
import com.orioooneee.lmuasister.ui.components.classDisplayLabel
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.components.RankLight
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatIsoDateTime
import com.orioooneee.lmuasister.ui.util.formatLap
import kotlin.math.roundToLong
import org.koin.compose.viewmodel.koinViewModel

private val PosGreen = Color(0xFF53D769)
private val NegRed = Color(0xFFE5484D)

// Rows shown above/below your own position in the collapsed classification (1 → me ±1).
private const val WINDOW = 1
private const val FOREIGN_PREVIEW = 7

@Composable
fun RaceProfileDetailScreen(
    viewModel: SteamLoginViewModel,
    insets: PaddingValues,
    eventId: String,
    split: Int?,
    onBack: () -> Unit,
) {
    val result by produceState<Result<RaceDetailDto>?>(null, eventId, split) {
        // Offline-first: paint the cached page instantly, then revalidate over the network.
        viewModel.cachedRaceDetail(eventId)?.let { value = Result.success(it) }
        val fresh = runCatching { viewModel.raceDetail(eventId, split, null) }
        if (fresh.isSuccess || value == null) value = fresh
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                result?.getOrNull()?.title ?: "Race",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val res = result) {
            null -> DetailSkeleton()
            else -> res.fold(
                onSuccess = {
                    DetailContent(
                        eventId = eventId,
                        d = it,
                        bottomInset = insets.calculateBottomPadding(),
                        cachedSplitSessions = { splitNo -> viewModel.cachedSplit(eventId, splitNo)?.sessions },
                        loadSplitSessions = { splitNo -> viewModel.raceSplit(eventId, splitNo, it.seriesId).sessions },
                    )
                },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            it.message ?: "Couldn't load this race",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMed,
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun PublicRaceProfileDetailScreen(
    viewModel: PublicUsersViewModel = koinViewModel(),
    insets: PaddingValues,
    uid: String,
    eventId: String,
    split: Int?,
    onBack: () -> Unit,
) {
    val result by produceState<Result<RaceDetailDto>?>(null, uid, eventId, split) {
        viewModel.cachedRaceDetail(uid, eventId, split)?.let { value = Result.success(it) }
        val fresh = runCatching { viewModel.raceDetail(uid, eventId, split) }
        if (fresh.isSuccess || value == null) value = fresh
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                result?.getOrNull()?.title ?: "Race",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val res = result) {
            null -> DetailSkeleton()
            else -> res.fold(
                onSuccess = {
                    DetailContent(
                        eventId = eventId,
                        d = it,
                        bottomInset = insets.calculateBottomPadding(),
                        cachedSplitSessions = { splitNo -> viewModel.cachedRaceDetail(uid, eventId, splitNo)?.sessions },
                        loadSplitSessions = { splitNo -> viewModel.raceDetail(uid, eventId, splitNo).sessions },
                    )
                },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            publicRaceErrorMessage(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                },
            )
        }
    }
}

private fun publicRaceErrorMessage(error: Throwable): String = when (error.message) {
    "race_not_found" -> "Race details unavailable"
    "nakama_unavailable" -> "Race details unavailable. Try again later."
    "user_not_found" -> "User not found"
    "external_user_unsupported", "local_user_unsupported" -> "Race details unavailable"
    else -> error.message ?: "Couldn't load this race"
}

@Composable
private fun DetailSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlockSkeleton(brush, 220.dp)   // track card
        BlockSkeleton(brush, 110.dp)   // summary card
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { ShimmerBar(Modifier.width(64.dp).height(32.dp), brush, corner = 8.dp) }
        }
        SplitSessionsSkeleton(brush)
    }
}

@Composable
private fun DetailContent(
    eventId: String,
    d: RaceDetailDto,
    bottomInset: Dp,
    cachedSplitSessions: (Int) -> Map<String, RaceSessionDetailDto?>? = { null },
    loadSplitSessions: suspend (Int) -> Map<String, RaceSessionDetailDto?>,
) {
    val mySplit = d.split
    val tabs = d.splitsAvailable.takeIf { it.isNotEmpty() }
        ?: d.splits.map { it.splitNo }.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(mySplit)
    var selected by remember(eventId) { mutableStateOf(mySplit ?: tabs.firstOrNull()) }
    var reloadNonce by remember(eventId) { mutableStateOf(0) }
    // Foreign splits, loaded lazily; kept in memory across tab switches. null = still loading.
    val loaded = remember(eventId) { mutableStateMapOf<Int, Result<Map<String, RaceSessionDetailDto?>>>() }

    LaunchedEffect(selected, reloadNonce) {
        val s = selected
        if (s != null && s != mySplit && loaded[s] == null) {
            val bundled = d.splits.firstOrNull { it.splitNo == s }?.sessions
            if (bundled != null) {
                loaded[s] = Result.success(bundled)
            } else {
                // Offline-first: show the cached split instantly, then revalidate.
                cachedSplitSessions(s)?.let { loaded[s] = Result.success(it) }
                val fresh = runCatching { loadSplitSessions(s) }
                if (fresh.isSuccess || loaded[s] == null) loaded[s] = fresh
            }
        }
    }

    val selectedSessions: Result<Map<String, RaceSessionDetailDto?>>? = when (selected) {
        null, mySplit -> Result.success(d.sessions)
        else -> loaded[selected]
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        d.trackInfo?.let { item { TrackCard(it) } }
        item { SummaryCard(d) }
        if (d.availableCars.isNotEmpty()) {
            item { AvailableCarsSection(d.availableCars) }
        }
        if (tabs.size > 1) {
            item { SplitTabs(tabs, selected, mySplit) { selected = it } }
        }
        when {
            selectedSessions == null -> item { SplitSessionsSkeleton(shimmerBrush()) }
            selectedSessions.isFailure -> item {
                SplitError {
                    loaded.remove(selected)
                    reloadNonce++
                }
            }
            else -> {
                val sessions = selectedSessions.getOrThrow()
                for (key in listOf("practice", "qualifying", "race")) {
                    val session = sessions[key] ?: continue
                    if (session.classification.isEmpty() && session.teamClassification.isEmpty()) continue
                    item(key = "$selected-$key") {
                        SessionCard(
                            sessionLabel(key),
                            session,
                            showRatingDeltas = key == "race",
                            showSectors = d.features?.sectors != false,
                        )
                    }
                }
            }
        }
    }
}

/** Horizontal, scrollable split selector; the player's own split is marked with a dot. */
@Composable
private fun SplitTabs(tabs: List<Int>, selected: Int?, mySplit: Int?, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { n ->
            val sel = n == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Amber else Surface1)
                    .border(1.dp, if (sel) Amber else Outline, RoundedCornerShape(8.dp))
                    .clickable { onSelect(n) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    "Split $n",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sel) Carbon else TextMed,
                    fontWeight = FontWeight.Bold,
                )
                if (n == mySplit) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (sel) Carbon else Amber))
                }
            }
        }
    }
}

@Composable
private fun SplitSessionsSkeleton(brush: androidx.compose.ui.graphics.Brush) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBar(Modifier.width(80.dp).height(12.dp), brush)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface1)
                        .border(1.dp, Outline, RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp),
                ) {
                    repeat(6) { LeaderboardRowSkeleton(brush) }
                }
            }
        }
    }
}

@Composable
private fun SplitError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Couldn't load this split", style = MaterialTheme.typography.bodyMedium, color = TextMed)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Surface2)
                .clickable(onClick = onRetry)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Retry", style = MaterialTheme.typography.labelMedium, color = Amber, fontWeight = FontWeight.SemiBold)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackCard(t: TrackDto) {
    val flag = flagFor(t.countryCode)
    val logo = t.logo
    val map = t.scheme
    val bg = t.background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TrackPreview(
            backgroundUrl = bg, mapUrl = map, logoUrl = logo, flagUrl = flag,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            height = 170.dp, emblemHeight = 28.dp, flagSize = 24.dp,
        )
        val name = t.simpleName?.takeIf { it.isNotBlank() } ?: t.name.takeIf { it.isNotBlank() }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            name?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold) }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                t.country?.let { MetaChip(it) }
                t.lengthKm?.let { MetaChip("$it km") }
                t.numTurns?.let { MetaChip("${it} turns") }
            }
        }
    }
}


/** Wrapping row of highlight-category chips (win / pole / grand slam…), each tinted with the
 *  same accent as its profile stat tile. Unknown keys are skipped. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(keys: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Order chips like the profile stat tiles (StatCategory declaration order), not the
        // backend's order. Unknown keys are dropped.
        keys.mapNotNull { StatCategory.byKey(it) }
            .sortedBy { it.ordinal }
            .forEach { CategoryChip(it.chip, it.color) }
    }
}

@Composable
private fun CategoryChip(label: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(d: RaceDetailDto) {
    var showBreakdown by remember { mutableStateOf(false) }
    var lapSheet by remember { mutableStateOf<String?>(null) }
    val hasBreakdown = d.srReasons.isNotEmpty() || d.drReasons.isNotEmpty()
    val showLapProgress = d.features?.lapProgress != false
    val laps = raceLaps(d)
    val qualifyingLaps = sessionLaps(d, "qualifying")
    // Any laps at all — laps without a time still get a row (shown as "—").
    val hasRaceLaps = showLapProgress && laps.isNotEmpty()
    val hasQualifyingLaps = showLapProgress && qualifyingLaps.isNotEmpty()
    val engine = listOf(
        d.engine,
        sessionMeRow(d, "race")?.engine,
        sessionMeRow(d, "qualifying")?.engine,
        sessionMeRow(d, "practice")?.engine,
    ).firstOrNull { !it.isNullOrBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Highlight chips this race earned (win / pole / grand slam…), colored like the profile stats.
        if (d.categories.isNotEmpty()) CategoryChips(d.categories)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            d.carClass?.let { ClassPill(it) }
            if (d.manufacturerLogoUrl != null) {
                AsyncImage(
                    model = d.manufacturerLogoUrl,
                    contentDescription = d.manufacturer,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp),
                )
            }
            if (d.carImageUrl != null) {
                AsyncImage(
                    model = d.carImageUrl,
                    contentDescription = d.carName ?: d.car,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(96.dp).height(72.dp).clip(RoundedCornerShape(5.dp)),
                )
            }
            if ((d.carName ?: d.car)?.isNotBlank() == true || engine != null) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    (d.carName ?: d.car)?.takeIf { it.isNotBlank() }?.let { name ->
                        val cleanName = stripCarClass(name)
                        Text(
                            cleanName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHigh,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    engine?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLow,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().basicMarquee(),
                        )
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            formatIsoDateTime(d.date)?.let { MetaChip(it) }
            d.gameVersion?.let { versionFullLabel(it) }?.let { MetaChip("v$it") }
            d.eventType?.let { MetaChip(it.replaceFirstChar(Char::uppercaseChar)) }
            d.split?.let { MetaChip(if (d.totalSplits != null) "Split $it/${d.totalSplits}" else "Split $it") }
            if (d.externalData || d.source == "racecenter") MetaChip("RaceCenter data")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // In-class start/finish/delta. Absolute positions aren't shown — users care about the class result.
            val start = d.classQualiPosition ?: d.gridPosition
            val finish = d.classRacePosition ?: d.position
            // Denominator is the in-class car count; falls back to the overall field only if class size is absent.
            val finishSize = (if (d.classRacePosition != null) d.classRaceSize else d.fieldSize.takeIf { it > 0 } ?: d.totalDrivers)
                ?.takeIf { it > 0 }
            start?.takeIf { it > 0 }?.let { Stat("Start", "P$it") }
            finish?.takeIf { it > 0 }?.let { Stat("Finish", "P$it" + (finishSize?.let { s -> " / $s" } ?: "")) }
            gainLost(start, finish)?.let { (txt, color) -> StatColored("+/-", txt, color) }
            DeltaStat("DR", d.drChange)
            DeltaStat("SR", d.srChange)
        }
        // Average pace over valid laps (skips the warm-up laps and pit laps), with the gap to
        // the best lap. The number of warm-up laps to drop is adjustable (default 1).
        val maxSkip = if (showLapProgress) maxOpeningSkip(laps) else -1
        if (maxSkip >= 0) {
            var showPaceInfo by remember { mutableStateOf(false) }
            var warmup by remember { mutableStateOf(if (maxSkip >= 1) 1 else 0) }
            val skip = warmup.coerceIn(0, maxSkip)
            val pace = paceFrom(laps, skip)!!
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PACE", style = MaterialTheme.typography.labelSmall, color = TextLow, fontWeight = FontWeight.Bold)
                        InfoDot { showPaceInfo = true }
                        Text("· ${pace.validLaps} valid laps", style = MaterialTheme.typography.labelSmall, color = TextLow)
                    }
                    // Warm-up-laps stepper: how many opening laps to leave out of the average.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Warm-up laps", style = MaterialTheme.typography.labelSmall, color = TextLow)
                        StepButton("−") { warmup = (skip - 1).coerceAtLeast(0) }
                        Text(
                            "$skip",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextHigh,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(16.dp),
                        )
                        StepButton("+") { warmup = (skip + 1).coerceAtMost(maxSkip) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Stat("Avg", formatLap(pace.avgMs))
                    StatColored("Δ avg", deltaSeconds(pace.deltaMs), Amber)
                }
            }
            if (showPaceInfo) PaceInfoDialog { showPaceInfo = false }
        }
        if (hasQualifyingLaps || hasRaceLaps || hasBreakdown) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // All per-lap times + extra stats for each session when the backend provides them.
                if (hasQualifyingLaps) SheetLink("Qualifying lap times") { lapSheet = "qualifying" }
                if (hasRaceLaps) SheetLink("Race lap times") { lapSheet = "race" }
                // Why the SR/DR moved — only when the backend sent a breakdown.
                if (hasBreakdown) SheetLink("Rating breakdown") { showBreakdown = true }
            }
        }
    }

    lapSheet?.let { sessionKey -> LapsSheet(d, sessionKey) { lapSheet = null } }
    if (showBreakdown) RatingBreakdownSheet(d) { showBreakdown = false }
}

/** An amber "Label ›" link that opens a detail bottom sheet. */
@Composable
private fun SheetLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Amber, fontWeight = FontWeight.SemiBold)
        Text("›", style = MaterialTheme.typography.labelMedium, color = Amber, fontWeight = FontWeight.Bold)
    }
}

/** Bottom sheet listing every lap (time + sectors + pit flag) plus the headline lap stats. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LapsSheet(d: RaceDetailDto, sessionKey: String, onDismiss: () -> Unit) {
    val showSectors = d.features?.sectors != false
    val sessionRow = sessionMeRow(d, sessionKey)
    val laps = sessionLaps(d, sessionKey)
    val bestMs = laps.mapNotNull { it.lapTimeMs?.takeIf { t -> t > 0L } }.minOrNull()
        ?: sessionRow?.bestLapMs?.takeIf { it > 0L }
    // Fastest time per sector across all laps — used to green-highlight the best sector each lap.
    val lapBestSectors = bestSectorsByIndex(laps)
    val bestSectors = lapBestSectors.takeIf { it.any { s -> (s ?: 0L) > 0L } }
        ?: sessionRow?.bestSectorsMs?.takeIf { it.any { s -> (s ?: 0L) > 0L } }
        ?: sessionRow?.bestLapSectorsMs?.takeIf { it.any { s -> (s ?: 0L) > 0L } }
        ?: emptyList()
    val theoreticalBestMs = theoreticalBestLapMs(bestSectors)
    // Track length (km) → average speed per lap and overall.
    val lengthKm = d.trackInfo?.lengthKm?.toDoubleOrNull()?.takeIf { it > 0.0 }
    val pace = paceFrom(laps, 1)
    val finishStatus = when (sessionKey) {
        "race" -> sessionRow?.finishStatus ?: d.finishStatus
        else -> sessionRow?.finishStatus
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface1,
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight())   // cap at ~70% of the screen, scroll past that
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${sessionLabel(sessionKey)} lap times", style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Laps", laps.size.toString())
                bestMs?.let { Stat("Best", formatLap(it)) }
                pace?.let { Stat("Avg", formatLap(it.avgMs)) }
                finishStatus?.takeIf { it.isNotBlank() }?.let { Stat("Result", it) }
            }
            if (showSectors) theoreticalBestMs?.let { TheoreticalBestLap(it, bestSectors) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Outline, RoundedCornerShape(10.dp)),
            ) {
                laps.forEachIndexed { i, lap ->
                    LapRow(
                        lap,
                        alt = i % 2 == 1,
                        isBest = bestMs != null && lap.lapTimeMs == bestMs,
                        bestSectors = bestSectors,
                        lengthKm = lengthKm,
                        showSectors = showSectors,
                    )
                }
            }
        }
    }
}

@Composable
private fun TheoreticalBestLap(totalMs: Long, bestSectors: List<Long?>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Theoretical best lap",
                style = MaterialTheme.typography.labelMedium,
                color = TextLow,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                formatLap(totalMs),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = PosGreen,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            bestSectors.mapIndexedNotNull { i, s -> s?.takeIf { it > 0L }?.let { "S${i + 1} ${sectorFmt(it)}" } }
                .joinToString("  "),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = TextLow,
            maxLines = 1,
        )
    }
}

@Composable
private fun LapRow(lap: LapDto, alt: Boolean, isBest: Boolean, bestSectors: List<Long?>, lengthKm: Double?, showSectors: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (alt) Surface2 else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lap.lap?.toString() ?: "—", style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
            // Class position this lap (overall as fallback), shown prominently.
            (lap.classPosition ?: lap.position)?.takeIf { it > 0 }?.let {
                Text("P$it", style = MaterialTheme.typography.labelMedium, color = TextHigh, fontWeight = FontWeight.SemiBold)
            }
            // Average speed on this lap (track length / lap time, 2 decimals) — next to the position.
            val speed = lap.lapTimeMs?.takeIf { it > 0L }?.let { ms -> lengthKm?.let { speedKmh(it, ms) } }
            speed?.let {
                Text(
                    "avg $it km/h",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextLow,
                )
            }
            if (lap.pit) PitBadge()
            Spacer(Modifier.weight(1f))
            Text(
                lap.lapTimeMs?.let { formatLap(it) } ?: "—",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (isBest) PosGreen else TextHigh,
                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (showSectors && lap.sectorsMs.any { (it ?: 0L) > 0L }) {
            // Each sector green when it's the fastest of that sector across the whole race.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                lap.sectorsMs.forEachIndexed { i, s ->
                    val isBestSector = s != null && s > 0L && bestSectors.getOrNull(i) == s
                    Text(
                        s?.takeIf { it > 0L }?.let { sectorFmt(it) } ?: "—",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (isBestSector) PosGreen else TextLow,
                        fontWeight = if (isBestSector) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** Fastest time per sector index across all laps (null where a sector was never set). */
private fun bestSectorsByIndex(laps: List<LapDto>): List<Long?> {
    val width = laps.maxOfOrNull { it.sectorsMs.size } ?: 0
    return (0 until width).map { i ->
        laps.mapNotNull { it.sectorsMs.getOrNull(i)?.takeIf { v -> v > 0L } }.minOrNull()
    }
}

private fun theoreticalBestLapMs(bestSectors: List<Long?>): Long? {
    if (bestSectors.isEmpty()) return null
    val sectors = bestSectors.map { it?.takeIf { v -> v > 0L } ?: return null }
    return sectors.sum()
}

@Composable
private fun PitBadge() {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text("PIT", style = MaterialTheme.typography.labelSmall, color = Amber, fontWeight = FontWeight.Bold)
    }
}

/** Average speed for a lap: track length (km) / lap time (ms) → km/h, to 2 decimals ("198.34"). */
private fun speedKmh(lengthKm: Double, lapMs: Long): String {
    if (lapMs <= 0L) return "—"
    val r = (lengthKm * 3_600_000.0 / lapMs * 100.0).roundToLong()
    return "${r / 100}.${(r % 100).toString().padStart(2, '0')}"
}

/** A sector time in milliseconds as "S.mmm" seconds. */
private fun sectorFmt(ms: Long): String {
    val s = ms / 1000
    val frac = (ms % 1000).toString().padStart(3, '0')
    return "$s.$frac"
}

/** Bottom sheet that explains the player's own SR/DR change for this race, component by
 *  component (signed weight + reason), grouped into an SR section and a DR section. */
/** Cap for the detail bottom sheets: ~85% of the screen height; content scrolls past it. */
@Composable
private fun sheetMaxHeight(): Dp {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    return with(density) { (windowInfo.containerSize.height * 0.85f).toDp() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingBreakdownSheet(d: RaceDetailDto, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface1,
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight())   // cap at ~70% of the screen, scroll past that
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "Rating breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
            )
            BreakdownSection("SR", d.srChange, d.srReasons)
            BreakdownSection("DR", d.drChange, d.drReasons)
        }
    }
}

@Composable
private fun BreakdownSection(label: String, total: Double?, reasons: List<ReasonDto>) {
    if (reasons.isEmpty() && total == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = TextHigh, fontWeight = FontWeight.Bold)
            total?.let {
                val color = if (it > 0) PosGreen else if (it < 0) NegRed else TextMed
                Text(signedOneDecimal(it), style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            }
        }
        if (reasons.isEmpty()) {
            Text("No breakdown for this race.", style = MaterialTheme.typography.bodySmall, color = TextLow)
        } else {
            // Zebra-striped rows, like the classification tables.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Outline, RoundedCornerShape(10.dp)),
            ) {
                reasons.forEachIndexed { i, r -> ReasonRow(r, alt = i % 2 == 1) }
            }
        }
    }
}

@Composable
private fun ReasonRow(r: ReasonDto, alt: Boolean) {
    val positive = r.positive || (r.impact ?: 0.0) > 0.0
    val color = if (positive) PosGreen else NegRed
    // `impact` is a 1–4 weight, not a real point value — render it as that many arrows
    // (up when it helped the rating, down when it hurt) instead of a misleading number.
    val count = (r.impact ?: 0.0).let { if (it < 0) -it else it }.toInt().coerceIn(1, 4)
    val arrows = (if (positive) "▲" else "▼").repeat(count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (alt) Surface2 else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            arrows,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.width(56.dp),
        )
        Text(
            r.reason?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = TextMed,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "+2.0" / "−3.2" — one decimal, explicit sign, minus rendered as U+2212. */
private fun signedOneDecimal(v: Double): String {
    val abs = if (v < 0) -v else v
    val r = (abs * 10).toLong()
    val sign = if (v < 0) "−" else "+"
    return "$sign${r / 10}.${r % 10}"
}

private fun sessionMeRow(d: RaceDetailDto, sessionKey: String): ClassificationRowDto? =
    d.sessions[sessionKey]?.classification?.firstOrNull { it.isMe }

/** Lap list for a session. Prefer the session "me" row because it carries class_position per lap.
 *  Race keeps the legacy top-level lap_progress fallback for older payloads. */
private fun sessionLaps(d: RaceDetailDto, sessionKey: String): List<LapDto> {
    val sessionLaps = sessionMeRow(d, sessionKey)?.lapProgress?.takeIf { it.isNotEmpty() }
    return when (sessionKey) {
        "race" -> sessionLaps ?: d.lapProgress
        else -> sessionLaps.orEmpty()
    }
}

private fun raceLaps(d: RaceDetailDto): List<LapDto> = sessionLaps(d, "race")

/** Player's average pace over valid laps + the gap to the best valid lap. */
private class PaceStats(
    val avgMs: Long,
    val deltaMs: Long,   // avg − best valid lap
    val validLaps: Int,
)

/** Average pace over VALID laps: timed laps that aren't among the first [skipOpening] laps and
 *  aren't pit laps. `delta` is avg − best valid lap. null when fewer than two valid laps remain. */
private fun paceFrom(laps: List<LapDto>, skipOpening: Int): PaceStats? {
    val timed = laps.filter { (it.lapTimeMs ?: 0L) > 0L }.sortedBy { it.lap ?: Int.MAX_VALUE }
    val valid = timed.drop(skipOpening.coerceAtLeast(0)).filter { !it.pit }
    if (valid.size < 2) return null
    val times = valid.map { it.lapTimeMs!! }
    val avg = times.sum() / times.size
    return PaceStats(avgMs = avg, deltaMs = avg - times.min(), validLaps = valid.size)
}

/** Largest "skip first N laps" value that still leaves ≥2 valid (non-pit) laps, or -1 when
 *  there isn't enough lap data to show a pace card at all. Bounds the skip stepper. */
private fun maxOpeningSkip(laps: List<LapDto>): Int {
    val timed = laps.filter { (it.lapTimeMs ?: 0L) > 0L }.sortedBy { it.lap ?: Int.MAX_VALUE }
    var max = -1
    for (skip in 0..timed.size) {
        if (timed.drop(skip).count { !it.pit } >= 2) max = skip
    }
    return max
}

/** A non-negative millisecond gap as "+S.mmm" seconds. */
private fun deltaSeconds(ms: Long): String {
    val s = ms / 1000
    val frac = (ms % 1000).toString().padStart(3, '0')
    return "+$s.$frac"
}

/** Compact square +/− button for the warm-up-laps stepper. */
@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.labelMedium, color = Amber, fontWeight = FontWeight.Bold)
    }
}

/** Small tappable "ⓘ" badge (hand-rolled to avoid a material-icons dependency). */
@Composable
private fun InfoDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(15.dp)
            .clip(CircleShape)
            .border(1.dp, TextLow, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("i", style = MaterialTheme.typography.labelSmall, color = TextLow, fontWeight = FontWeight.Bold)
    }
}

/** Explains how the average pace is computed (which laps count). */
@Composable
private fun PaceInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it", color = Amber, fontWeight = FontWeight.SemiBold) } },
        title = { Text("Average pace", color = TextHigh, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "The average uses your valid laps only — it leaves out the first few warm-up laps " +
                    "(adjustable, default 1) and any in/out laps around a pit stop. Δ avg is how far " +
                    "your average sits off your best valid lap.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMed,
            )
        },
        containerColor = Surface1,
    )
}

private fun gainLost(grid: Int?, finish: Int?): Pair<String, Color>? {
    if (grid == null || grid <= 0 || finish == null || finish <= 0) return null
    val g = grid - finish
    val color = if (g > 0) PosGreen else if (g < 0) NegRed else TextMed
    val txt = if (g > 0) "+$g" else g.toString()
    return txt to color
}

@Composable
private fun Stat(label: String, value: String) = StatColored(label, value, TextHigh)

@Composable
private fun StatColored(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLow)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeltaStat(label: String, delta: Double?) {
    if (delta == null) return
    val color = if (delta > 0) PosGreen else if (delta < 0) NegRed else TextMed
    val arrow = if (delta > 0) "▲" else if (delta < 0) "▼" else "•"
    val abs = if (delta < 0) -delta else delta
    val r = (abs * 10).toLong()
    StatColored(label, "$arrow ${r / 10}.${r % 10}", color)
}


@Composable
private fun SessionCard(
    label: String,
    session: RaceSessionDetailDto,
    showRatingDeltas: Boolean,
    showSectors: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val rows = session.classification
    val teamRows = session.teamClassification
    val meIndex = rows.indexOfFirst { it.isMe }
    val shown = when {
        expanded -> rows
        // Your split: window the rows around your own position.
        meIndex >= 0 -> rows.subList((meIndex - WINDOW).coerceAtLeast(0), (meIndex + WINDOW).coerceAtMost(rows.lastIndex) + 1)
        // Foreign split (no "me"): preview the first few, expand for the full field.
        else -> rows.take(FOREIGN_PREVIEW)
    }
    val canToggle = expanded || shown.size < rows.size

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface1)
                .border(1.dp, Outline, RoundedCornerShape(12.dp)),
        ) {
            teamRows.forEachIndexed { i, row ->
                ClassificationLine(
                    row,
                    alt = i % 2 == 1,
                    showRatingDeltas = showRatingDeltas,
                    showSectors = showSectors,
                )
            }
            if (teamRows.isNotEmpty() && shown.isNotEmpty()) {
                Text(
                    "Drivers",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().background(Surface2).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            shown.forEachIndexed { i, row ->
                ClassificationLine(
                    row,
                    alt = (i + teamRows.size) % 2 == 1,
                    showRatingDeltas = showRatingDeltas,
                    showSectors = showSectors,
                )
            }
            if (canToggle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .background(Surface2)
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (expanded) "Show less ▴" else "Show all ${rows.size} ▾",
                        style = MaterialTheme.typography.labelMedium,
                        color = Amber,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassificationLine(
    r: ClassificationRowDto,
    alt: Boolean,
    showRatingDeltas: Boolean,
    showSectors: Boolean,
) {
    // Zebra striping like the race leaderboards; the player's own row always wins with an amber tint.
    val bg = when {
        r.isMe -> Amber.copy(alpha = 0.12f)
        alt -> Surface2
        else -> Color.Transparent
    }
    val posColor = if (r.isMe) Amber else r.carClass?.let { classColorFor(it) } ?: TextHigh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            r.position?.toString() ?: "—",
            style = MaterialTheme.typography.labelMedium,
            color = posColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.width(18.dp),
        )
        Column(
            modifier = Modifier.width(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val flag = flagFor(r.nationality)
            when {
                flag != null -> FlagCircle(flag, 16.dp)
                !r.teamIcon.isNullOrBlank() -> AsyncImage(
                    model = r.teamIcon,
                    contentDescription = r.teamName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(16.dp).clip(CircleShape),
                )
            }
        }
        // Name + badges share the flexible middle column; the name marquees if it can't fit.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val driverName = r.name ?: "—"
            val carName = r.car
                ?.takeIf { it.isNotBlank() }
                ?.let { car -> stripCarClass(car).ifBlank { car } }
            val title = buildAnnotatedString {
                append(driverName)
                carName?.let {
                    withStyle(SpanStyle(color = TextLow, fontWeight = FontWeight.Normal)) {
                        append(", $it")
                    }
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = if (r.isMe) TextHigh else TextMed,
                fontWeight = if (r.isMe) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                r.carClass?.takeIf { it.isNotBlank() }?.let { ClassMiniBadge(it) }
                // Position within the driver's own class (the leading number is the overall position).
                r.classPosition?.takeIf { it > 0 }?.let { ClassPosBadge(it, r.carClass) }
                r.driverRating?.let { RatingMiniBadge("DR", it) }
                r.safetyRating?.let { RatingMiniBadge("SR", it) }
                if (showRatingDeltas) RatingDeltaColumn(r.drChange, r.srChange)
            }
            // Best-lap sector splits, when the backend provides them.
            val sectors = r.bestLapSectorsMs.filterNotNull().filter { it > 0L }
            if (showSectors && sectors.isNotEmpty()) {
                Text(
                    sectors.mapIndexed { i, s -> "S${i + 1} ${sectorFmt(s)}" }.joinToString("  "),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextLow,
                    maxLines = 1,
                )
            }
        }
        (r.bestLapMs ?: r.finishTimeMs)?.let {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formatLap(it),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = TextMed,
                    maxLines = 1,
                )
                // Class-relative gap to the leader (race: finish gap / quali: best-lap delta).
                gapToLeaderLabel(r)?.let { gap ->
                    Text(
                        gap,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextLow,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Class-relative gap to the leader: "+N LAPS" when laps down, "+S.mmm" for a time gap,
 *  null for the class leader (or a row with no gap data). */
private fun gapToLeaderLabel(r: ClassificationRowDto): String? = when {
    r.gapLaps > 0 -> "+${r.gapLaps} " + if (r.gapLaps == 1) "LAP" else "LAPS"
    (r.gapMs ?: 0L) > 0L -> deltaSeconds(r.gapMs!!)
    else -> null
}

/** Compact class chip (colored like the schedule badges) for a single classification line. */
@Composable
private fun ClassMiniBadge(carClass: String) {
    val label = classDisplayLabel(carClass)
    val c = classColorFor(carClass)
    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(c).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = onBadgeText(c), fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Outlined "P{n}" chip (in the class colour) for the driver's position within their own class. */
@Composable
private fun ClassPosBadge(classPos: Int, carClass: String?) {
    val c = carClass?.takeIf { it.isNotBlank() }?.let { classColorFor(it) } ?: TextMed
    Box(Modifier.clip(RoundedCornerShape(5.dp)).border(1.dp, c, RoundedCornerShape(5.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text("P$classPos", style = MaterialTheme.typography.labelSmall, color = c, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Compact DR/SR pill, same two-tone look as the profile header but tighter for a list row. */
@Composable
private fun RatingMiniBadge(label: String, rating: RatingDto) {
    if (rating.rank.isBlank()) return
    val color = rankColor(rating.rank)
    val letter = rating.rank.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val value = letter + rating.tier.toString()
    // Same official geometry as RankBadge, scaled down for a list row.
    val shape = RoundedCornerShape(3.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(shape).border(1.dp, color, shape),
    ) {
        Box(Modifier.background(RankLight).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color)
        }
        Box(Modifier.background(color).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = RankLight, maxLines = 1)
        }
    }
}

@Composable
private fun RatingDeltaColumn(drChange: Double?, srChange: Double?) {
    if (drChange == null && srChange == null) return
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        RatingDeltaLine("DR:", drChange)
        RatingDeltaLine("SR:", srChange)
    }
}

@Composable
private fun RatingDeltaLine(label: String, delta: Double?) {
    if (delta == null) return
    val color = when {
        delta > 0 -> PosGreen
        delta < 0 -> NegRed
        else -> TextMed
    }
    val arrow = when {
        delta > 0 -> "▲"
        delta < 0 -> "▼"
        else -> "•"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.sp)
        Text(label, style = style, color = TextLow, maxLines = 1)
        Text(
            "$arrow ${formatRatingDelta(delta)}",
            style = style,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun formatRatingDelta(delta: Double): String {
    val abs = if (delta < 0) -delta else delta
    val rounded = (abs * 10).toLong()
    return "${rounded / 10}.${rounded % 10}"
}

// Official LMU rank-tier colors (from the in-game DR/SR badge SVGs).
private val Gold = Color(0xFFE1A01F)
private val Silver = Color(0xFF8F9499)
private val Bronze = Color(0xFF977548)
private val Platinum = Color(0xFF89B2DD)

private fun rankColor(rank: String): Color = when (rank.trim().firstOrNull()?.lowercaseChar()) {
    'b' -> Bronze
    's' -> Silver
    'g' -> Gold
    'p' -> Platinum
    else -> TextMed
}


@Composable
private fun FlagCircle(url: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape),
    )
}

@Composable
private fun ClassPill(carClass: String) {
    val label = classDisplayLabel(carClass)
    val c = classColorFor(carClass)
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(c).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onBadgeText(c), fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun AvailableCarsSection(availableCars: Map<String, List<AvailableCarDto>>) {
    if (availableCars.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Available Cars",
            style = MaterialTheme.typography.titleMedium,
            color = TextHigh,
            fontWeight = FontWeight.Bold,
        )

        availableCars.forEach { (className, cars) ->
            if (cars.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        className,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMed,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        cars.forEach { car ->
                            AvailableCarCard(car)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableCarCard(car: AvailableCarDto) {
    Row(
        modifier = Modifier
            .width(220.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (car.carImageUrl != null) {
            AsyncImage(
                model = car.carImageUrl,
                contentDescription = car.friendly,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(86.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (car.manufacturerLogoUrl != null) {
                    AsyncImage(
                        model = car.manufacturerLogoUrl,
                        contentDescription = car.manufacturer,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    car.manufacturer,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                car.friendly,
                style = MaterialTheme.typography.labelSmall,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun sessionLabel(key: String): String = when (key) {
    "race" -> "Race"
    "qualifying" -> "Qualifying"
    "practice" -> "Practice"
    else -> key.replaceFirstChar(Char::uppercaseChar)
}

private fun flagFor(value: String?): String? {
    val cc = value?.trim()?.lowercase() ?: return null
    if (cc.length != 2 || cc.any { it !in 'a'..'z' }) return null
    return "https://cdn.jsdelivr.net/gh/HatScripts/circle-flags/flags/$cc.svg"
}
