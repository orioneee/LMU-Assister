package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.ui.IconChevronRight
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatStart
import kotlin.time.Clock

private fun Race.trackLabel(): String = track?.shortName?.takeIf { it.isNotBlank() } ?: circuit
private fun Race.durationLabel(): String = if (raceLength > 0) "${raceLength}m" else ""

@Composable
private fun Race.nextLabel(): String {
    val now = remember { Clock.System.now() }
    return nextStart(now)?.formatStart().orEmpty()
}

/** Full-width card for the schedule + Home lists, with the day's full times grid. */
@Composable
fun RaceCard(race: Race, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(race.imageUrl, race.accentColor(), size = 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (race.classInfos.isNotEmpty()) {
                    ClassChips(race.classInfos)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    race.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    race.trackLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaChip(race.durationLabel())
                    if (race.difficulty.isNotBlank()) {
                        DotSeparator()
                        SkillBadge(race.difficulty)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(IconChevronRight, contentDescription = "Open", tint = TextLow, modifier = Modifier.size(18.dp))
        }

        if (race.times.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            TimesGrid(race.times)
        }
    }
}

/**
 * Compact card for the Home "Featured" carousel.
 * Fixed width AND height so every card in the row is identical — no top class strip.
 */
@Composable
fun RaceCardCompact(race: Race, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(230.dp)
            .height(248.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = race.imageUrl,
            contentDescription = race.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(118.dp).background(Surface2),
        )
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                race.classInfos.firstOrNull()?.let { ClassChip(it) }
                val next = race.nextLabel()
                if (next.isNotBlank()) {
                    Text(next, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                race.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                race.trackLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = TextMed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaChip(race.durationLabel())
                if (race.difficulty.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    SkillBadge(race.difficulty)
                }
            }
        }
    }
}

/** Featured "NEXT UP" hero. [maxHeight] caps it to (e.g.) a third of the screen. */
@Composable
fun HeroRaceCard(race: Race, maxHeight: Dp, onClick: () -> Unit = {}) {
    val accent = race.accentColor()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(1.dp, accent.copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
    ) {
        CoverImage(
            url = race.imageUrl,
            contentDescription = race.title,
            modifier = Modifier.fillMaxSize().background(Surface2),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Carbon.copy(alpha = 0.35f),
                    1f to Carbon.copy(alpha = 0.92f),
                ),
            ),
        )
        Column(Modifier.fillMaxWidth().padding(18.dp).align(Alignment.BottomStart)) {
            Text("NEXT UP", style = MaterialTheme.typography.labelMedium, color = accent)
            Spacer(Modifier.height(8.dp))
            Text(
                race.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextHigh,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(race.trackLabel(), style = MaterialTheme.typography.bodyLarge, color = TextMed, maxLines = 1)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaChip(race.durationLabel())
                    if (race.difficulty.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        SkillBadge(race.difficulty)
                    }
                }
                val next = race.nextLabel()
                if (next.isNotBlank()) {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(next, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(url: String?, accent: Color, size: Dp) {
    CoverImage(
        url = url,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
    )
}
