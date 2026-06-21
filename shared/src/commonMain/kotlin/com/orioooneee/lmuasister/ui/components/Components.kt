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

// FIA driver-category tier from the first letter (handles "Bronze" AND "B2"/"B3" etc.)
private fun srColor(sr: String): Color = when (sr.trim().firstOrNull()?.lowercaseChar()) {
    'b' -> Color(0xFFCD7F32) // bronze
    's' -> Color(0xFFC9D1DA) // silver
    'g' -> Color(0xFFE6B422) // gold
    'p' -> Color(0xFF6FE3F0) // platinum
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
    val tier = srColor(sr)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, Outline, RoundedCornerShape(6.dp)),
    ) {
        Box(Modifier.background(Surface3).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text("SR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMed)
        }
        Box(Modifier.background(tier).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(sr.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = onBadgeText(tier), maxLines = 1)
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
