package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextMed

/**
 * Friendly centered placeholder for "nothing here" pages — an accent-ringed icon,
 * a title, a muted subtitle, and an optional action button. The caller supplies
 * the sizing modifier (e.g. `fillParentMaxSize()` in a LazyColumn item) so the
 * content vertically centers in the available space.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = IconFlag,
    accent: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // concentric rings: soft accent glow behind a bordered medallion
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(116.dp).clip(CircleShape).background(accent.copy(alpha = 0.06f)))
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(1.dp, accent.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(38.dp))
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextHigh,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMed,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 24.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Carbon,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
