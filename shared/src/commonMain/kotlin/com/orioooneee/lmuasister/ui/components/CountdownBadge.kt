package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.ui.theme.Lime
import com.orioooneee.lmuasister.ui.util.startsInLabel
import kotlin.time.Instant

/** Small pill showing the live time-to-start (amber), or "LIVE" (lime) once started. */
@Composable
fun CountdownBadge(next: Instant, now: Instant, modifier: Modifier = Modifier) {
    val live = next <= now
    val accent = if (live) Lime else MaterialTheme.colorScheme.primary
    Box(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            startsInLabel(next, now),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
