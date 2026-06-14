package com.orioooneee.lmuasister.ui.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.Hotlap
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceSettings
import com.orioooneee.lmuasister.data.model.RaceWeather
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.ui.components.ClassChip
import com.orioooneee.lmuasister.ui.components.CoverImage
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.components.SkillBadge
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.SrBadge
import com.orioooneee.lmuasister.ui.components.TimesGrid
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.components.accentColor
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassGte
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassLmp3
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.util.formatLap
import com.orioooneee.lmuasister.ui.util.skyColor
import com.orioooneee.lmuasister.ui.util.skyEmoji
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.col_best_lap
import lmuassister.shared.generated.resources.col_driver
import lmuassister.shared.generated.resources.col_gap
import lmuassister.shared.generated.resources.col_pos
import lmuassister.shared.generated.resources.duration_race
import lmuassister.shared.generated.resources.fastest_laps
import lmuassister.shared.generated.resources.format
import lmuassister.shared.generated.resources.hotlaps
import lmuassister.shared.generated.resources.length_km
import lmuassister.shared.generated.resources.next_start_times
import lmuassister.shared.generated.resources.no
import lmuassister.shared.generated.resources.no_lap_times
import lmuassister.shared.generated.resources.session_practice
import lmuassister.shared.generated.resources.session_qualifying
import lmuassister.shared.generated.resources.session_race
import lmuassister.shared.generated.resources.set_assists
import lmuassister.shared.generated.resources.set_damage
import lmuassister.shared.generated.resources.set_driver_rank
import lmuassister.shared.generated.resources.set_driver_swaps
import lmuassister.shared.generated.resources.set_fuel_usage
import lmuassister.shared.generated.resources.set_limited_tires
import lmuassister.shared.generated.resources.set_practice
import lmuassister.shared.generated.resources.set_qualifying
import lmuassister.shared.generated.resources.set_safety_rank
import lmuassister.shared.generated.resources.set_setup
import lmuassister.shared.generated.resources.set_split_size
import lmuassister.shared.generated.resources.set_tire_warmers
import lmuassister.shared.generated.resources.set_tire_wear
import lmuassister.shared.generated.resources.set_track_limits
import lmuassister.shared.generated.resources.full_leaderboard
import lmuassister.shared.generated.resources.track_city
import lmuassister.shared.generated.resources.track_name
import lmuassister.shared.generated.resources.track_official_name
import lmuassister.shared.generated.resources.track_country
import lmuassister.shared.generated.resources.track_length
import lmuassister.shared.generated.resources.track_turns
import lmuassister.shared.generated.resources.weather
import lmuassister.shared.generated.resources.weather_rain
import lmuassister.shared.generated.resources.yes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceDetailsScreen(
    race: Race,
    onBack: () -> Unit,
    onOpenLeaderboard: (leaderboardId: String, title: String) -> Unit = { _, _ -> },
) {
    val now = remember { Clock.System.now() }
    val upcoming = remember(race) { race.times.filter { it >= now } }

    val repo = koinInject<RaceRepository>()
    // Leaderboard (fast) and hot-laps (async build) load in parallel — each resolves
    // its own skeleton independently. Offline-first: paint cached (memory/disk) instantly,
    // then refresh from the network. null = nothing cached yet → skeleton.
    val leaderboard by produceState<List<LapEntry>?>(
        if (race.leaderboardId != null) repo.peekLeaderboard(race.id) else emptyList(),
        race.id,
    ) {
        if (race.leaderboardId == null) {
            value = emptyList()
            return@produceState
        }
        if (value == null) repo.cachedLeaderboard(race.id)?.let { value = it }
        val fresh = repo.leaderboard(race.id).getOrNull()
        if (fresh != null) value = fresh else if (value == null) value = emptyList()
    }
    val hotlaps by produceState<List<Hotlap>?>(repo.peekHotlaps(race.id), race.id) {
        if (value == null) repo.cachedHotlaps(race.id)?.let { value = it }
        val fresh = repo.hotlaps(race.id).getOrNull()
        if (fresh != null) value = fresh else if (value == null) value = emptyList()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize().background(Carbon),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header image with title + back (full width) ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)) {
                CoverImage(
                    url = race.imageUrl,
                    contentDescription = race.title,
                    modifier = Modifier.fillMaxSize().background(Surface2),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Carbon.copy(alpha = 0.35f),
                            0.5f to Color.Transparent,
                            1f to Carbon.copy(alpha = 0.95f),
                        ),
                    ),
                )
                CircleButton("‹", Modifier.align(Alignment.TopStart).padding(12.dp), onBack)
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(race.type.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = race.accentColor())
                    Spacer(Modifier.height(6.dp))
                    Text(race.title, style = MaterialTheme.typography.headlineMedium, color = TextHigh, fontWeight = FontWeight.Black)
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                race.classInfos.take(6).forEach { ClassChip(it) }
                if (race.raceLength > 0) MetaChip(stringResource(Res.string.duration_race, race.raceLength))
                race.settings.safetyRank?.let { SrBadge(it) }
            }
        }

        val lbId = race.leaderboardId
        if (lbId != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val lb = leaderboard
                if (lb == null) LeaderboardSkeletonCard()
                else LeaderboardCard(lb, onOpenFull = { onOpenLeaderboard(lbId, race.title) })
            }
        }
        race.track?.let {
            item {
                TrackCard(
                    it,
                    hotlaps.orEmpty(),
                    hotlapsLoading = hotlaps == null,
                    hotlapsSkeletonCount = (race.carClasses.size * 2).coerceAtLeast(2),
                )
            }
        }
        race.weather?.let { item { WeatherCard(it) } }
        item {
            Card(stringResource(Res.string.format)) { DetailRows(settingRows(race.settings)) }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Card(stringResource(Res.string.next_start_times)) {
                    TimesGrid(race.times)
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: TrackInfo,
    hotlaps: List<Hotlap> = emptyList(),
    hotlapsLoading: Boolean = false,
    hotlapsSkeletonCount: Int = 6,
) {
    val flag = track.countryCode?.let { flagUrlFromCode(it) } ?: track.country?.let { flagUrl(it) }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Emblem (circuit logo) stands in for the track name; the country flag sits in
            // the top-right corner of it — no title row needed.
            if (track.logoUrl != null || flag != null) {
                Box(Modifier.fillMaxWidth()) {
                    track.logoUrl?.let { TrackEmblem(it) }
                    flag?.let { Box(Modifier.align(Alignment.TopEnd)) { FlagCircle(it) } }
                }
            }
            if (!track.mapUrl.isNullOrBlank()) {
                CoverImage(
                    url = track.mapUrl,
                    contentDescription = "${track.name} map",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium),
                )
            }
            val officialName = track.name.takeIf { it.isNotBlank() }
            val primaryName = track.simpleName?.takeIf { it.isNotBlank() } ?: officialName
            DetailRows(
                listOfNotNull(
                    primaryName?.let { stringResource(Res.string.track_name) to it },
                    officialName?.takeIf { it != primaryName }?.let { stringResource(Res.string.track_official_name) to it },
                    track.country?.let { stringResource(Res.string.track_country) to it },
                    track.town?.let { stringResource(Res.string.track_city) to it },
                    track.lengthKm?.let { stringResource(Res.string.track_length) to stringResource(Res.string.length_km, it.toString()) },
                    track.numTurns?.let { stringResource(Res.string.track_turns) to it.toString() },
                ),
            )
            when {
                hotlapsLoading -> HotlapsSkeleton(hotlapsSkeletonCount)
                hotlaps.isNotEmpty() -> HotlapsFlow(hotlaps)
            }
        }
    }
}

/** Circuit text logo (vector emblem) — shown only once Coil has it (missing asset = invisible). */
@Composable
private fun TrackEmblem(url: String) {
    val painter = rememberAsyncImagePainter(model = url)
    val state by painter.state.collectAsState()
    if (state is AsyncImagePainter.State.Success) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(80.dp),
        )
    }
}

/** Hot-laps as a wrapping flow of equal-height cards (thumbnail + all the info). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotlapsFlow(hotlaps: List<Hotlap>) {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(Res.string.hotlaps),
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            hotlaps.forEach { h -> HotlapCard(h) { runCatching { uri.openUri(h.url) } } }
        }
    }
}

private val HOTLAP_CARD_W = 152.dp
private val HOTLAP_CARD_H = 198.dp
private val HOTLAP_THUMB_H = 86.dp

@Composable
private fun HotlapCard(h: Hotlap, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .width(HOTLAP_CARD_W)
            .height(HOTLAP_CARD_H)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().height(HOTLAP_THUMB_H).background(Carbon)) {
            CoverImage(url = h.thumbnail, contentDescription = h.title, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Carbon.copy(alpha = 0.55f)),
                ),
            )
            Box(
                Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape).background(Carbon.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", style = MaterialTheme.typography.labelMedium, color = TextHigh)
            }
            h.lapTime?.let {
                Box(
                    Modifier.align(Alignment.BottomStart).padding(6.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Carbon.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClassBadgeChip(h.classBadge ?: h.carClass?.uppercase() ?: "—")
                h.gameVersion?.let {
                    Text("v$it", style = MaterialTheme.typography.labelSmall, color = TextMed, maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                h.car ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                h.driver ?: h.author ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = TextMed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun classBadgeColor(label: String): Color {
    val b = label.lowercase()
    return when {
        "hyper" in b || b == "hy" -> ClassHyper
        "gt3" in b -> ClassGt3
        "gte" in b -> ClassGte
        "lmp2" in b -> ClassLmp2
        "lmp3" in b -> ClassLmp3
        else -> ClassMixed
    }
}

/** Solid class badge (HY / GT3 / LMP2 / LMP3) in its class colour. */
@Composable
private fun ClassBadgeChip(label: String) {
    val c = classBadgeColor(label)
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(c).padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = onBadgeText(c), maxLines = 1)
    }
}

/**
 * Full-card leaderboard skeleton matching the first-open state: a shimmer title,
 * the column header, a single row, and the "show more" button — all shimmer.
 */
@Composable
private fun LeaderboardSkeletonCard() {
    val brush = shimmerBrush()
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large).padding(16.dp),
    ) {
        // title
        ShimmerBar(Modifier.width(130.dp).height(16.dp), brush)
        Spacer(Modifier.height(16.dp))
        // column header
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBar(Modifier.width(14.dp).height(8.dp), brush)
            Spacer(Modifier.width(12.dp))
            ShimmerBar(Modifier.width(46.dp).height(8.dp), brush)
            Spacer(Modifier.weight(1f))
            ShimmerBar(Modifier.width(52.dp).height(8.dp), brush)
            Spacer(Modifier.width(12.dp))
            ShimmerBar(Modifier.width(34.dp).height(8.dp), brush)
        }
        Spacer(Modifier.height(12.dp))
        // single row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBar(Modifier.width(16.dp).height(13.dp), brush)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBar(Modifier.fillMaxWidth(0.45f).height(13.dp), brush)
                Spacer(Modifier.height(6.dp))
                ShimmerBar(Modifier.fillMaxWidth(0.28f).height(9.dp), brush)
            }
            Spacer(Modifier.width(12.dp))
            ShimmerBar(Modifier.width(70.dp).height(13.dp), brush)
            Spacer(Modifier.width(10.dp))
            ShimmerBar(Modifier.width(44.dp).height(13.dp), brush)
        }
        Spacer(Modifier.height(12.dp))
        // "show more" button
        ShimmerBar(Modifier.fillMaxWidth().height(38.dp), brush, corner = 8.dp)
    }
}

/** Shimmer placeholder shaped like the hot-laps card flow ([count] cards). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotlapsSkeleton(count: Int) {
    val brush = shimmerBrush()
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        ShimmerBar(Modifier.width(58.dp).height(11.dp), brush)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(count) {
                Column(
                    Modifier.width(HOTLAP_CARD_W).height(HOTLAP_CARD_H)
                        .clip(RoundedCornerShape(12.dp)).background(Surface2),
                ) {
                    ShimmerBar(Modifier.fillMaxWidth().height(HOTLAP_THUMB_H), brush, corner = 0.dp)
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ShimmerBar(Modifier.width(40.dp).height(16.dp), brush)
                            Spacer(Modifier.weight(1f))
                            ShimmerBar(Modifier.width(24.dp).height(10.dp), brush)
                        }
                        Spacer(Modifier.height(8.dp))
                        ShimmerBar(Modifier.fillMaxWidth(0.9f).height(10.dp), brush)
                        Spacer(Modifier.weight(1f))
                        ShimmerBar(Modifier.fillMaxWidth(0.6f).height(9.dp), brush)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(w: RaceWeather) {
    Card(stringResource(Res.string.weather)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            w.race?.let { WeatherSession(stringResource(Res.string.session_race), it) }
            w.qualifying?.let { WeatherSession(stringResource(Res.string.session_qualifying), it) }
            w.practice?.let { WeatherSession(stringResource(Res.string.session_practice), it) }
        }
    }
}

@Composable
private fun WeatherSession(label: String, sw: SessionWeather) {
    val rainPeak = sw.segments.maxOfOrNull { it.rainChance ?: 0 } ?: 0
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.SemiBold)
            if (rainPeak > 0) {
                Text(stringResource(Res.string.weather_rain, rainPeak), style = MaterialTheme.typography.labelSmall, color = ClassLmp2)
            }
        }
        Spacer(Modifier.height(6.dp))
        // proportional, colour-coded forecast timeline
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)),
        ) {
            sw.segments.forEach { seg ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight((seg.durationMin ?: 1).toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(skyColor(seg.sky, seg.rainChance ?: 0)),
                ) {
                    Text(skyEmoji(seg.sky, seg.rainChance ?: 0), style = MaterialTheme.typography.bodyLarge)
                    seg.tempC?.let { Text("$it°", style = MaterialTheme.typography.labelSmall, color = TextHigh) }
                }
            }
        }
    }
}

internal fun gapLabel(deltaMs: Long): String =
    "+${deltaMs / 1000}.${(deltaMs % 1000).toString().padStart(3, '0')}"

/** Wide enough for 4-digit ranks (full leaderboard goes into the thousands). */
private val POS_COL_W = 44.dp

/** Column header shared by the inline card and the full-leaderboard screen. */
@Composable
internal fun LeaderboardHeaderRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(stringResource(Res.string.col_pos), Modifier.width(POS_COL_W), style = MaterialTheme.typography.labelSmall, color = TextLow)
        Text(stringResource(Res.string.col_driver), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextLow)
        Text(stringResource(Res.string.col_best_lap), Modifier.width(84.dp), style = MaterialTheme.typography.labelSmall, color = TextLow, textAlign = TextAlign.End)
        Text(stringResource(Res.string.col_gap), Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = TextLow, textAlign = TextAlign.End)
    }
}

private const val LB_PREVIEW = 5

@Composable
private fun LeaderboardCard(entries: List<LapEntry>, onOpenFull: () -> Unit) {
    Card(stringResource(Res.string.fastest_laps)) {
        when {
            entries.isEmpty() -> Text(
                stringResource(Res.string.no_lap_times),
                style = MaterialTheme.typography.bodyMedium,
                color = TextLow,
            )

            else -> {
                val best = entries.first().bestLapMs
                val visible = entries.take(LB_PREVIEW)

                Column(Modifier.fillMaxWidth()) {
                    LeaderboardHeaderRow()
                    visible.forEachIndexed { i, e -> LeaderboardRow(i, e, best) }

                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenFull)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(Res.string.full_leaderboard),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LeaderboardRow(i: Int, e: LapEntry, best: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (i % 2 == 1) Surface2 else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${e.rank}", Modifier.width(POS_COL_W), style = MaterialTheme.typography.bodyMedium, color = TextMed, maxLines = 1)
        Column(Modifier.weight(1f)) {
            Text(e.initials, style = MaterialTheme.typography.bodyMedium, color = TextHigh, fontWeight = FontWeight.SemiBold, maxLines = 1)
            e.carClass?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextLow, maxLines = 1) }
        }
        Text(
            formatLap(e.bestLapMs),
            Modifier.width(84.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextHigh,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            if (i == 0) "—" else gapLabel(e.bestLapMs - best),
            Modifier.width(60.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextMed,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun settingRows(s: RaceSettings): List<Pair<String, String>> = listOfNotNull(
    s.qualifyingLength?.let { stringResource(Res.string.set_qualifying) to "${it}m" },
    s.practiceLength?.let { stringResource(Res.string.set_practice) to "${it}m" },
    s.setup?.let { stringResource(Res.string.set_setup) to it },
    s.assists?.let { stringResource(Res.string.set_assists) to it },
    s.damage?.let { stringResource(Res.string.set_damage) to it },
    s.tireWear?.let { stringResource(Res.string.set_tire_wear) to "${it}x" },
    s.fuelUsage?.let { stringResource(Res.string.set_fuel_usage) to "${it}x" },
    s.safetyRank?.let { stringResource(Res.string.set_safety_rank) to it },
    s.driverRank?.let { stringResource(Res.string.set_driver_rank) to it },
    s.splitSize?.let { stringResource(Res.string.set_split_size) to it.toString() },
    s.driverSwaps?.let { stringResource(Res.string.set_driver_swaps) to if (it) stringResource(Res.string.yes) else stringResource(Res.string.no) },
    s.trackLimits?.let { stringResource(Res.string.set_track_limits) to it },
    s.tireWarmers?.let { stringResource(Res.string.set_tire_warmers) to it },
    s.limitedTires?.let { stringResource(Res.string.set_limited_tires) to it },
)

@Composable
private fun Card(title: String? = null, leading: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large).padding(16.dp),
    ) {
        if (title != null || leading != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(12.dp))
                }
                title?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/** Round country flag — uses circle-flags (already circular), not a square crop. */
@Composable
private fun FlagCircle(url: String) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(Surface2).border(1.dp, Outline, CircleShape),
    ) {
        CoverImage(url = url, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    }
}

/** ISO-3166 alpha-2 code → circle-flags SVG URL (preferred when the backend sends it). */
private fun flagUrlFromCode(code: String): String? {
    val cc = code.trim().lowercase()
    if (cc.length != 2 || cc.any { it !in 'a'..'z' }) return null
    return "https://cdn.jsdelivr.net/gh/HatScripts/circle-flags/flags/$cc.svg"
}

/** Country name → circle-flags SVG URL (pre-cropped to a circle), null if unknown. */
private fun flagUrl(country: String): String? {
    val code = when (country.trim().lowercase()) {
        "france" -> "fr"
        "united states", "usa", "united states of america" -> "us"
        "united kingdom", "uk", "great britain", "england" -> "gb"
        "portugal" -> "pt"
        "brazil" -> "br"
        "italy" -> "it"
        "belgium" -> "be"
        "germany" -> "de"
        "spain" -> "es"
        "austria" -> "at"
        "netherlands" -> "nl"
        "bahrain" -> "bh"
        "qatar" -> "qa"
        "japan" -> "jp"
        "saudi arabia" -> "sa"
        "united arab emirates", "uae" -> "ae"
        "australia" -> "au"
        "canada" -> "ca"
        "mexico" -> "mx"
        "china" -> "cn"
        else -> return null
    }
    return "https://cdn.jsdelivr.net/gh/HatScripts/circle-flags/flags/$code.svg"
}

/** Zebra-striped key/value rows for readability (alternating row shade). */
@Composable
private fun DetailRows(rows: List<Pair<String, String>>) {
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { i, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (i % 2 == 1) Surface2 else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextLow)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = TextHigh, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun CircleButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(38.dp).clip(CircleShape).background(Carbon.copy(alpha = 0.55f))
            .border(1.dp, Outline, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = TextHigh)
    }
}
