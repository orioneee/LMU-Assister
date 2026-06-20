package com.orioooneee.lmuasister.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.Surface3

/** A shared, animated shimmer brush — create once per skeleton and pass to its bars. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -250f,
        targetValue = 1250f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = listOf(Surface2, Surface3, Surface2),
        start = Offset(x, 0f),
        end = Offset(x + 250f, 0f),
    )
}

/** One rounded shimmer placeholder bar. Size it via [modifier]. */
@Composable
fun ShimmerBar(modifier: Modifier, brush: Brush, corner: Dp = 6.dp) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(brush))
}

/** A race-row card placeholder (position badge · title/car/track · right rail). */
@Composable
fun RaceRowSkeleton(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBar(Modifier.size(46.dp), brush, corner = 12.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBar(Modifier.fillMaxWidth(0.7f).height(16.dp), brush)
            ShimmerBar(Modifier.fillMaxWidth(0.45f).height(12.dp), brush)
            ShimmerBar(Modifier.fillMaxWidth(0.3f).height(12.dp), brush)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBar(Modifier.width(40.dp).height(12.dp), brush)
            ShimmerBar(Modifier.width(34.dp).height(12.dp), brush)
        }
    }
}

/** A leaderboard-row placeholder: rank · name · time. */
@Composable
fun LeaderboardRowSkeleton(brush: Brush) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBar(Modifier.size(22.dp), brush, corner = 4.dp)
        ShimmerBar(Modifier.weight(1f).height(14.dp), brush)
        ShimmerBar(Modifier.width(64.dp).height(14.dp), brush)
    }
}

/** Generic full-width block placeholder (e.g. a detail card). */
@Composable
fun BlockSkeleton(brush: Brush, height: Dp) {
    ShimmerBar(Modifier.fillMaxWidth().height(height), brush, corner = 14.dp)
}
