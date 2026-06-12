package com.orioooneee.lmuasister.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.ui.IconChampionship
import com.orioooneee.lmuasister.ui.IconChevronRight
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.IconTools
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed

private data class ToolLink(
    val title: String,
    val subtitle: String,
    val url: String,
    val icon: ImageVector,
    val tint: Color,
)

private val tools = listOf(
    ToolLink(
        "FOV Calculator",
        "Dial in your field of view",
        "https://dinex86.github.io/FOV-Calculator/",
        IconTools, ClassLmp2,
    ),
    ToolLink(
        "Pitstop Calculator",
        "Fuel & strategy planner",
        "https://ultimatesetuphub.com/pitstop-calculator",
        IconTools, ClassGt3,
    ),
    ToolLink(
        "Online Championships · S7",
        "Standings & calendar",
        "https://community.lemansultimate.com/index.php?threads/le-mans-ultimate-online-championships-season-7.13109/",
        IconChampionship, ClassMixed,
    ),
    ToolLink(
        "RaceControl Schedule Post",
        "Source forum thread",
        "https://community.lemansultimate.com/index.php?threads/racecontrol-events-schedule.3158/",
        IconFlag, Amber,
    ),
)

@Composable
fun ToolsScreen(onOpenUrl: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Carbon),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Tools",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextHigh,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Calculators & community links",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMed,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(tools.size) { i ->
            ToolRow(tools[i]) { onOpenUrl(tools[i].url) }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolLink, onClick: () -> Unit) {
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
        Box(
            Modifier
                .size(42.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(tool.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tool.title, style = MaterialTheme.typography.titleMedium, color = TextHigh)
            Spacer(Modifier.height(2.dp))
            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMed)
        }
        Spacer(Modifier.width(8.dp))
        Icon(IconChevronRight, contentDescription = null, tint = TextLow, modifier = Modifier.size(18.dp))
    }
}
