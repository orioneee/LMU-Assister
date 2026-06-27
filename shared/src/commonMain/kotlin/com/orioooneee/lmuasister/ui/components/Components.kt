package com.orioooneee.lmuasister.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.model.ClassInfo
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassGte
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassLmp3
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.SkillAdv
import com.orioooneee.lmuasister.ui.theme.SkillBeginner
import com.orioooneee.lmuasister.ui.theme.SkillInter
import com.orioooneee.lmuasister.ui.theme.SkillPro
import com.orioooneee.lmuasister.ui.theme.SkillRookie
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.parseHexColor

/** Always the official LMU class colour, mapped from the class id/name. We ignore the
 *  backend's badge colour so badges are consistent across versions (v2 doesn't send one). */
fun ClassInfo.color(): Color = classColorFor("$id $name")

fun classColorFor(key: String): Color {
    val c = key.lowercase()
    return when {
        "hyper" in c -> ClassHyper
        "gt3" in c -> ClassGt3
        "gte" in c -> ClassGte
        "lmp2" in c -> ClassLmp2
        "lmp3" in c -> ClassLmp3
        else -> ClassMixed
    }
}

fun classDisplayLabel(carClass: String): String {
    val clean = carClass.trim()
    val c = clean.lowercase()
    return when {
        c.isBlank() -> ""
        "hyper" in c -> "HY"
        "gt3" in c -> "GT3"
        "gte" in c -> "GTE"
        c == "lmp2_elms" -> "LMP2"
        "lmp2" in c -> "LMP2"
        "lmp3" in c -> "LMP3"
        else -> clean.uppercase()
    }
}

fun Race.accentColor(): Color =
    classInfos.firstOrNull()?.let { classColorFor("${it.id} ${it.name}") } ?: Amber

/** LMU-style class code: only Hypercar -> HY and LMGT3 -> GT3 are shortened; the rest keep their name. */
fun ClassInfo.shortCode(): String {
    val n = "$id $name".lowercase()
    return when {
        "hyper" in n -> "HY"
        "lmgt3" in n -> "GT3"
        else -> name
    }
}

// Light panel color used by the official LMU rank badges (the "DR"/"SR" half).
val RankLight = Color(0xFFF3F4F8)

// Official LMU rank-tier colors, taken from the in-game DR/SR badge SVGs.
// First letter only, so it handles "Bronze" AND "B2"/"B3" etc.
fun rankTierColor(rank: String): Color = when (rank.trim().firstOrNull()?.lowercaseChar()) {
    'b' -> Color(0xFF977548) // bronze
    's' -> Color(0xFF8F9499) // silver
    'g' -> Color(0xFFE1A01F) // gold
    'p' -> Color(0xFF89B2DD) // platinum
    else -> Color(0xFF8A93A6)
}

fun onBadgeText(bg: Color): Color {
    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (lum > 0.6f) Carbon else Color.White
}

fun difficultyColor(difficulty: String): Color = when {
    "rookie" in difficulty.lowercase() -> SkillRookie
    "beginner" in difficulty.lowercase() -> SkillBeginner
    "intermediate" in difficulty.lowercase() -> SkillInter
    "advanced" in difficulty.lowercase() -> SkillAdv
    "pro" in difficulty.lowercase() || "expert" in difficulty.lowercase() -> SkillPro
    else -> SkillRookie
}

@Composable
fun ClassChip(classInfo: ClassInfo) {
    val c = classInfo.color()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            classInfo.shortCode(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = onBadgeText(c),
            maxLines = 1,
        )
    }
}

@Composable
fun SrBadge(sr: String) {
    if (sr.isBlank() || sr.equals("none", ignoreCase = true)) return
    RankBadge("SR", sr.uppercase(), rankTierColor(sr))
}

/**
 * Two-tone rank badge matching the official LMU artwork: a light panel carrying the [label]
 * ("DR"/"SR") in the tier [color], and a colored panel carrying the [value] (e.g. "G2") in light.
 */
@Composable
fun RankBadge(label: String, value: String, color: Color) {
    // Geometry from the official SVG (112x52): outer rx=8 (~0.15·h) and a 3px (~0.06·h)
    // tier-colored stroke around the whole badge.
    val shape = RoundedCornerShape(3.5.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(shape).border(1.5.dp, color, shape),
    ) {
        Box(Modifier.background(RankLight).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color)
        }
        Box(Modifier.background(color).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = RankLight, maxLines = 1)
        }
    }
}

@Composable
fun ClassChips(classes: List<ClassInfo>, max: Int = 3) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        classes.take(max).forEach { ClassChip(it) }
    }
}

@Composable
fun SkillBadge(difficulty: String) {
    if (difficulty.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(difficultyColor(difficulty)))
        Spacer(Modifier.width(6.dp))
        Text(
            difficulty,
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MetaChip(text: String) {
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Surface2)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextMed, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
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

@Composable
fun DotSeparator() {
    Spacer(Modifier.width(8.dp))
    Box(Modifier.size(3.dp).clip(CircleShape).background(TextLow))
    Spacer(Modifier.width(8.dp))
}
