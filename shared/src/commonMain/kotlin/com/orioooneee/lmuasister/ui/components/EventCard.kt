package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.RaceEvent
import com.orioooneee.lmuasister.ui.IconBolt
import com.orioooneee.lmuasister.ui.IconChevronRight
import com.orioooneee.lmuasister.ui.theme.Lime
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed

private fun durationLabel(min: Int): String =
    if (min >= 60 && min % 60 == 0) "${min / 60} h" else "$min min"

/** Full-width card used in the schedule list. */
@Composable
fun EventCard(event: RaceEvent, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        // Left color rail keyed to the car class
        Box(
            Modifier
                .width(4.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(event.carClass.color()),
        )
        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClassChip(event.carClass)
                if (event.startingSoon) {
                    Spacer(Modifier.width(8.dp))
                    LivePill()
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${event.countryFlag}  ${event.track}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaChip(durationLabel(event.durationMinutes))
                Spacer(Modifier.width(8.dp))
                MetaChip(event.format.label)
                if (event.rounds > 1) {
                    Spacer(Modifier.width(8.dp))
                    MetaChip("${event.rounds} rounds")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkillBadge(event.skill)
                DotSeparator()
                Text(
                    event.scheduleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                event.nextStartLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (event.startingSoon) Lime else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Icon(IconChevronRight, contentDescription = "Open", tint = TextLow, modifier = Modifier.size(18.dp))
        }
    }
}

/** Compact card for horizontal carousels on Home. */
@Composable
fun EventCardCompact(event: RaceEvent, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClassChip(event.carClass)
            Text(
                event.nextStartLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            event.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextHigh,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${event.countryFlag}  ${event.track}",
            style = MaterialTheme.typography.bodySmall,
            color = TextMed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetaChip(durationLabel(event.durationMinutes))
            Spacer(Modifier.width(8.dp))
            SkillBadge(event.skill)
        }
    }
}

/** Pulsing-style "starting soon" pill. */
@Composable
fun LivePill() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Lime.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(IconBolt, contentDescription = null, tint = Lime, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("Soon", style = MaterialTheme.typography.labelMedium, color = Lime)
    }
}

/** Big featured "Next up" card for the top of Home. */
@Composable
fun HeroEventCard(event: RaceEvent, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        event.carClass.color().copy(alpha = 0.28f),
                        Surface1,
                    ),
                ),
            )
            .border(1.dp, event.carClass.color().copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "NEXT UP",
                style = MaterialTheme.typography.labelMedium,
                color = TextMed,
            )
            if (event.startingSoon) {
                Spacer(Modifier.width(8.dp))
                LivePill()
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            event.title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextHigh,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${event.countryFlag}  ${event.track}",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMed,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetaChip(durationLabel(event.durationMinutes))
            Spacer(Modifier.width(8.dp))
            MetaChip(event.format.label)
            Spacer(Modifier.width(8.dp))
            SkillBadge(event.skill)
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text("STARTS", style = MaterialTheme.typography.labelMedium, color = TextLow)
                Spacer(Modifier.height(2.dp))
                Text(
                    event.nextStartLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "View schedule",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
