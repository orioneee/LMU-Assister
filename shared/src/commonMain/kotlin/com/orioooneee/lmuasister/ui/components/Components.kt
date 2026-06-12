package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.CarClass
import com.orioooneee.lmuasister.data.RaceFormat
import com.orioooneee.lmuasister.data.SkillLevel
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.theme.SkillAdv
import com.orioooneee.lmuasister.ui.theme.SkillBeginner
import com.orioooneee.lmuasister.ui.theme.SkillInter
import com.orioooneee.lmuasister.ui.theme.SkillPro
import com.orioooneee.lmuasister.ui.theme.SkillRookie
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed

fun CarClass.color(): Color = when (this) {
    CarClass.HYPERCAR -> ClassHyper
    CarClass.LMP2 -> ClassLmp2
    CarClass.LMGT3 -> ClassGt3
    CarClass.MULTICLASS -> ClassMixed
}

fun SkillLevel.color(): Color = when (this) {
    SkillLevel.ROOKIE -> SkillRookie
    SkillLevel.BEGINNER -> SkillBeginner
    SkillLevel.INTERMEDIATE -> SkillInter
    SkillLevel.ADVANCED -> SkillAdv
    SkillLevel.PRO -> SkillPro
}

/** Solid color pill, used for the car-class tag. */
@Composable
fun ClassChip(carClass: CarClass) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(carClass.color().copy(alpha = 0.16f))
            .border(1.dp, carClass.color().copy(alpha = 0.5f), RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(carClass.color()),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = carClass.label,
            style = MaterialTheme.typography.labelMedium,
            color = carClass.color(),
        )
    }
}

/** Outlined dot+label for the skill level. */
@Composable
fun SkillBadge(skill: SkillLevel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(skill.color()),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = skill.label,
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
        )
    }
}

/** Neutral meta pill, e.g. duration or format. */
@Composable
fun MetaChip(text: String, leadingIcon: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Surface2)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

fun RaceFormat.tint(): Color = when (this) {
    RaceFormat.SPRINT -> SkillBeginner
    RaceFormat.FEATURE -> ClassLmp2
    RaceFormat.ENDURANCE -> SkillAdv
    RaceFormat.CHAMPIONSHIP -> ClassMixed
}

@Composable
fun OutlinedTag(text: String, tint: Color = TextLow) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
fun DotSeparator() {
    Spacer(Modifier.width(8.dp))
    Box(
        Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(TextLow),
    )
    Spacer(Modifier.width(8.dp))
}
