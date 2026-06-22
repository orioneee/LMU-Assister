package com.orioooneee.lmuasister.ui.tracks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.orioooneee.lmuasister.ui.theme.Surface2

/**
 * The shared track preview hero used everywhere a track is shown: a webp background photo with
 * a dark scrim, the minimap centered, the circuit emblem in the top-left and the country flag in
 * the top-right. Pass already-resolved absolute URLs (any may be null → that layer is skipped).
 * The caller owns the outer shape: pass a `Modifier.clip(...)` when the hero isn't already clipped
 * by a parent card.
 */
@Composable
internal fun TrackPreview(
    backgroundUrl: String?,
    mapUrl: String?,
    logoUrl: String?,
    flagUrl: String?,
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
    emblemHeight: Dp = 22.dp,
    flagSize: Dp = 22.dp,
) {
    Box(modifier.fillMaxWidth().height(height).background(Surface2)) {
        backgroundUrl?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }
        mapUrl?.let { TrackSvgImage(it, Modifier.fillMaxSize().padding(10.dp)) }
        logoUrl?.let { TrackSvgImage(it, Modifier.align(Alignment.TopStart).padding(8.dp).height(emblemHeight)) }
        flagUrl?.let { FlagCircle(it, flagSize, Modifier.align(Alignment.TopEnd).padding(8.dp)) }
    }
}
