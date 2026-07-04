package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.data.remote.TrackBreakdownDto
import com.orioooneee.lmuasister.ui.tracks.TrackPreview
import com.orioooneee.lmuasister.ui.tracks.trackFlagUrl
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatKm

private val DistAccent = Color(0xFF7FB2E8)

/** Per-track career distance, shown as a grid of track cards (adaptive 2+ columns). */
@Composable
fun TrackBreakdownScreen(
    viewModel: SteamLoginViewModel,
    insets: PaddingValues,
    onBack: () -> Unit,
    onOpenTrack: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = (state as? SteamLoginUiState.SignedIn)?.backend
        ?.let { it as? BackendState.Ok }?.profile

    TrackBreakdownView(
        profile = profile,
        insets = insets,
        onBack = onBack,
        onOpenTrack = onOpenTrack,
    )
}

/** Shared per-track distance grid. Used by both private and public profile flows. */
@Composable
fun TrackBreakdownView(
    profile: SteamProfile?,
    insets: PaddingValues,
    onBack: () -> Unit,
    onOpenTrack: (String) -> Unit = {},
) {
    val tracks = profile?.trackBreakdown

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Column(Modifier.weight(1f)) {
                Text(
                    "Distance by track",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                profile?.let {
                    Text(
                        "${formatKm(it.totalDistanceKm)} km total · ${tracks?.size ?: 0} tracks",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMed,
                        maxLines = 1,
                    )
                }
            }
        }

        when {
            tracks == null -> TrackGrid(insets) { items(6) { TrackBreakdownSkeleton() } }
            tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No track distance yet.", style = MaterialTheme.typography.bodyMedium, color = TextLow)
            }
            else -> TrackGrid(insets) {
                items(tracks, key = { it.trackId ?: it.track }) { TrackBreakdownCard(it, onOpenTrack) }
            }
        }
    }
}

@Composable
private fun TrackGrid(insets: PaddingValues, content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 360.dp -> 1
            maxWidth < 720.dp -> 2
            maxWidth < 1080.dp -> 3
            else -> 4
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackBreakdownCard(t: TrackBreakdownDto, onOpenTrack: (String) -> Unit) {
    // All four assets arrive as absolute CDN (R2) URLs straight in the breakdown row.
    val trackId = t.trackId?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .clickable(enabled = trackId != null) { trackId?.let(onOpenTrack) },
    ) {
        TrackPreview(
            backgroundUrl = t.background, mapUrl = t.scheme, logoUrl = t.logo,
            flagUrl = trackFlagUrl(t.countryCode),
            height = 96.dp, emblemHeight = 24.dp,
        )
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                t.track,
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${formatKm(t.distanceKm)} km",
                style = MaterialTheme.typography.titleMedium,
                color = DistAccent,
                fontWeight = FontWeight.Bold,
            )
            // Wrap so the "laps" chip never gets clipped on narrow cards.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaChip("${t.races} races")
                MetaChip("${t.laps} laps")
            }
        }
    }
}

@Composable
private fun TrackBreakdownSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Outline, RoundedCornerShape(14.dp)),
    ) {
        Box(Modifier.fillMaxWidth().height(92.dp).background(brush))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBar(Modifier.fillMaxWidth(0.9f).height(13.dp), brush)
            ShimmerBar(Modifier.fillMaxWidth(0.5f).height(18.dp), brush)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBar(Modifier.size(width = 54.dp, height = 16.dp), brush, corner = 8.dp)
                ShimmerBar(Modifier.size(width = 46.dp, height = 16.dp), brush, corner = 8.dp)
            }
        }
    }
}
