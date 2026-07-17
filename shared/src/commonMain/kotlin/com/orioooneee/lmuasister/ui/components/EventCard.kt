package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
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
import com.orioooneee.lmuasister.ui.util.hhmm
import com.orioooneee.lmuasister.ui.util.rememberNow
import com.orioooneee.lmuasister.ui.util.startsInLabel
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.next_up
import org.jetbrains.compose.resources.stringResource

private fun Race.trackLabel(): String = track?.shortName?.takeIf { it.isNotBlank() } ?: circuit
private fun Race.durationLabel(): String = if (raceLength > 0) "${raceLength}m" else ""
private fun Race.scheduleCoverUrl(useTrackBackgroundCover: Boolean): String? =
    if (useTrackBackgroundCover) track?.backgroundUrl?.takeIf { it.isNotBlank() } ?: imageUrl else imageUrl

private val CARD_TEXT_SHADOW = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(0f, 2f), blurRadius = 6f)

@Composable
private fun Race.nextLabel(): String {
    val now = rememberNow()
    return nextStart(now)?.formatStart().orEmpty()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceCard(
    race: Race,
    modifier: Modifier = Modifier,
    showCountdown: Boolean = true,
    showTimer: Boolean = true,
    timeColumns: Int = 3,
    useTrackBackgroundCover: Boolean = false,
    onClick: () -> Unit = {},
) {
    val diff = difficultyColor(race.difficulty)
    val coverUrl = race.scheduleCoverUrl(useTrackBackgroundCover)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Surface1)
            .background(diff.copy(alpha = 0.07f))
            .border(1.5.dp, diff.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(175.dp).background(Surface2)) {
            CoverImage(coverUrl, Modifier.fillMaxSize(), race.title)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            Carbon.copy(alpha = 0.88f),
                            Carbon.copy(alpha = 0.58f),
                            Carbon.copy(alpha = 0.32f),
                        ),
                    ),
                ),
            )
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    FlowRow(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        race.classInfos.take(4).forEach { ClassChip(it) }
                    }
                    race.track?.logoUrl?.let {
                        Spacer(Modifier.width(8.dp))
                        CardTrackEmblem(it)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    race.title,
                    style = MaterialTheme.typography.titleSmall.copy(shadow = CARD_TEXT_SHADOW),
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    race.trackLabel(),
                    style = MaterialTheme.typography.bodySmall.copy(shadow = CARD_TEXT_SHADOW),
                    color = TextMed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    race.settings.safetyRank?.let { SrBadge(it) }
                    Spacer(Modifier.weight(1f))
                    val duration = race.durationLabel()
                    if (duration.isNotBlank()) MetaChip(duration)
                }
            }
        }
        if (race.times.isNotEmpty()) {
            Column(Modifier.padding(12.dp)) {
                TimesGrid(
                    race.times,
                    columns = timeColumns,
                    showCountdown = showCountdown,
                    showChrome = showTimer,
                )
            }
        }
    }
}

@Composable
private fun CardTrackEmblem(url: String) {
    val painter = rememberAsyncImagePainter(model = url)
    val state by painter.state.collectAsState()
    if (state is AsyncImagePainter.State.Success) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(24.dp).widthIn(max = 68.dp),
        )
    }
}

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

/**
 * Featured "NEXT UP" hero. [maxHeight] caps it to (e.g.) a third of the screen.
 *
 * @param showLabel show the "NEXT UP" caption — hide it when this is the only event.
 * @param showCountdown show the live time-to-start — hide it for non-current weeks.
 */
@Composable
fun HeroRaceCard(
    race: Race,
    maxHeight: Dp,
    showLabel: Boolean = true,
    showCountdown: Boolean = true,
    onClick: () -> Unit = {},
) {
    val accent = race.accentColor()
    HeroContent(
        race = race,
        showLabel = showLabel,
        showCountdown = showCountdown,
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(1.dp, accent.copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
    )
}

@Composable
fun HeroRaceTimesCard(
    race: Race,
    heroHeight: Dp,
    showCountdown: Boolean = true,
    showTimer: Boolean = true,
    useTrackBackgroundCover: Boolean = false,
    onClick: () -> Unit = {},
) {
    val accent = race.accentColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Surface1)
            .border(1.dp, accent.copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
    ) {
        HeroContent(
            race = race,
            showLabel = false,
            showCountdown = showCountdown && showTimer,
            useTrackBackgroundCover = useTrackBackgroundCover,
            modifier = Modifier.fillMaxWidth().height(heroHeight),
        )
        if (race.times.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                // No countdown here — the hero above already shows it.
                TimesGrid(
                    race.times,
                    showCountdown = false,
                    showChrome = showTimer,
                    centered = true,
                )
            }
        }
    }
}

@Composable
private fun HeroContent(
    race: Race,
    showLabel: Boolean,
    showCountdown: Boolean,
    useTrackBackgroundCover: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accent = race.accentColor()
    Box(modifier) {
        CoverImage(
            url = race.scheduleCoverUrl(useTrackBackgroundCover),
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
            if (showLabel) {
                Text(stringResource(Res.string.next_up), style = MaterialTheme.typography.labelMedium, color = accent)
                Spacer(Modifier.height(8.dp))
            }
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
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaChip(race.durationLabel())
                    race.settings.safetyRank?.let {
                        Spacer(Modifier.width(8.dp))
                        SrBadge(it)
                    }
                }
                val now = rememberNow()
                val next = race.nextStart(now)
                when {
                    race.completed -> CompletedBadge(race)
                    next != null && showCountdown -> Column(horizontalAlignment = Alignment.End) {
                        CountdownBadge(next, now)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(next.hhmm(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1)
                        }
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
