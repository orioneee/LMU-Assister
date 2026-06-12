package com.orioooneee.lmuasister.ui.details

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceSettings
import com.orioooneee.lmuasister.data.model.RaceWeather
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.ui.components.ClassChip
import com.orioooneee.lmuasister.ui.components.CoverImage
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.SkillBadge
import com.orioooneee.lmuasister.ui.components.SrBadge
import com.orioooneee.lmuasister.ui.components.accentColor
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.util.formatLap
import com.orioooneee.lmuasister.ui.util.formatStart
import com.orioooneee.lmuasister.ui.util.skyColor
import com.orioooneee.lmuasister.ui.util.skyEmoji
import org.koin.compose.koinInject
import kotlin.time.Clock

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceDetailsScreen(race: Race, onBack: () -> Unit) {
    val now = remember { Clock.System.now() }
    val upcoming = remember(race) { race.times.filter { it >= now } }

    val repo = koinInject<RaceRepository>()
    val leaderboard by produceState<List<LapEntry>?>(null, race.leaderboardId) {
        value = race.leaderboardId?.let { repo.leaderboard(it).getOrDefault(emptyList()) } ?: emptyList()
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
                if (race.raceLength > 0) MetaChip("${race.raceLength}m race")
                race.settings.safetyRank?.let { SrBadge(it) }
            }
        }

        if (race.leaderboardId != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { LeaderboardCard(leaderboard) }
        }
        race.track?.let { item { TrackCard(it) } }
        race.weather?.let { item { WeatherCard(it) } }
        item {
            Card("Format") { DetailRows(settingRows(race.settings)) }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Card("Next start times") {
                    DetailRows(upcoming.mapIndexed { i, t -> "Race ${i + 1}" to t.formatStart() })
                }
            }
        }
    }
}

@Composable
private fun TrackCard(track: TrackInfo) {
    Card(track.name) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!track.mapUrl.isNullOrBlank()) {
                CoverImage(
                    url = track.mapUrl,
                    contentDescription = "${track.name} map",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium),
                )
            }
            DetailRows(
                listOfNotNull(
                    track.country?.let { "Country" to it },
                    track.town?.let { "City" to it },
                    track.lengthKm?.let { "Length" to "$it km" },
                    track.numTurns?.let { "Turns" to it.toString() },
                ),
            )
        }
    }
}

@Composable
private fun WeatherCard(w: RaceWeather) {
    Card("Weather") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            w.race?.let { WeatherSession("Race", it) }
            w.qualifying?.let { WeatherSession("Qualifying", it) }
            w.practice?.let { WeatherSession("Practice", it) }
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
                Text("💧 up to $rainPeak%", style = MaterialTheme.typography.labelSmall, color = ClassLmp2)
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

private fun gapLabel(deltaMs: Long): String =
    "+${deltaMs / 1000}.${(deltaMs % 1000).toString().padStart(3, '0')}"

private val SHOW_STEPS = listOf(1, 5, 30)

@Composable
private fun LeaderboardCard(entries: List<LapEntry>?) {
    Card("Fastest laps") {
        when {
            entries == null -> Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TextLow)
                Spacer(Modifier.width(10.dp))
                Text("Loading times…", style = MaterialTheme.typography.bodyMedium, color = TextLow)
            }

            entries.isEmpty() -> Text(
                "No lap times yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLow,
            )

            else -> {
                var shown by remember(entries) { mutableStateOf(1) }
                val best = entries.first().bestLapMs
                val visible = entries.take(shown)

                Column(Modifier.fillMaxWidth().animateContentSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("#", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = TextLow)
                        Text("DRIVER", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextLow)
                        Text("BEST LAP", Modifier.width(84.dp), style = MaterialTheme.typography.labelSmall, color = TextLow, textAlign = TextAlign.End)
                        Text("GAP", Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = TextLow, textAlign = TextAlign.End)
                    }
                    visible.forEachIndexed { i, e -> LeaderboardRow(i, e, best) }

                    if (shown < entries.size) {
                        val next = (SHOW_STEPS.firstOrNull { it > shown } ?: entries.size).coerceAtMost(entries.size)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { shown = next }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Show top $next  ▾",
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
}

@Composable
private fun LeaderboardRow(i: Int, e: LapEntry, best: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (i % 2 == 1) Surface2 else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${e.rank}", Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium, color = TextMed)
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

private fun settingRows(s: RaceSettings): List<Pair<String, String>> = listOfNotNull(
    s.qualifyingLength?.let { "Qualifying" to "${it}m" },
    s.practiceLength?.let { "Practice" to "${it}m" },
    s.setup?.let { "Setup" to it },
    s.assists?.let { "Assists" to it },
    s.damage?.let { "Damage" to it },
    s.tireWear?.let { "Tire wear" to "${it}x" },
    s.fuelUsage?.let { "Fuel usage" to "${it}x" },
    s.safetyRank?.let { "Safety rank" to it },
    s.driverRank?.let { "Driver rank" to it },
    s.splitSize?.let { "Split size" to it.toString() },
    s.driverSwaps?.let { "Driver swaps" to if (it) "Yes" else "No" },
    s.trackLimits?.let { "Track limits" to it },
    s.tireWarmers?.let { "Tire warmers" to it },
    s.limitedTires?.let { "Limited tires" to it },
)

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large).padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        content()
    }
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
private fun CircleButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(38.dp).clip(CircleShape).background(Carbon.copy(alpha = 0.55f))
            .border(1.dp, Outline, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = TextHigh)
    }
}
