package com.orioooneee.lmuasister.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceSettings
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
import com.orioooneee.lmuasister.ui.util.formatStart
import kotlin.time.Clock

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceDetailsScreen(race: Race, onBack: () -> Unit) {
    val now = remember { Clock.System.now() }
    val upcoming = remember(race) { race.times.filter { it >= now } }

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
                if (race.difficulty.isNotBlank()) SkillBadge(race.difficulty)
                race.settings.safetyRank?.let { SrBadge(it) }
            }
        }

        race.track?.let { item { TrackCard(it) } }
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
                AsyncImage(
                    model = track.mapUrl,
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
