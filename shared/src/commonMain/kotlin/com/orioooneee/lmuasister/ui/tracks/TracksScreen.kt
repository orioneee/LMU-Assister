package com.orioooneee.lmuasister.ui.tracks

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.TrackFullDto
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import org.koin.compose.koinInject

@Composable
fun TracksScreen(insets: PaddingValues, onOpenTrack: (String) -> Unit) {
    val repo = koinInject<RaceRepository>()
    // Offline-first: paint the cached roster instantly, then revalidate over the network.
    val tracks by produceState<List<TrackFullDto>?>(repo.cachedTracks()) {
        repo.cachedTracks()?.let { value = it }
        repo.tracks().getOrNull()?.let { value = it }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Text(
            "Tracks",
            style = MaterialTheme.typography.titleLarge,
            color = TextHigh,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp + insets.calculateTopPadding(), bottom = 12.dp),
        )
        LazyVerticalGrid(
            // ~150dp min → 2 per row on a phone, more on wider screens.
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val list = tracks
            if (list == null) {
                items(6) { TrackCardSkeleton() }
            } else {
                items(list, key = { it.id }) { t -> TrackGridCard(t) { onOpenTrack(t.id) } }
            }
        }
    }
}

@Composable
private fun TrackGridCard(t: TrackFullDto, onClick: () -> Unit) {
    val logo = trackAsset(t, "logo.svg")
    val map = trackAsset(t, "map.svg")
    val bg = trackAsset(t, "background.webp")
    val flag = trackFlagUrl(t.countryCode)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        TrackPreview(backgroundUrl = bg, mapUrl = map, logoUrl = logo, flagUrl = flag, height = 104.dp)
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                trackTitle(t),
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                minLines = 2,   // reserve 2 lines so every card in a row is the same height
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth().basicMarquee(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                t.lengthKm?.let { MetaChip("$it km") }
                t.corners?.let { MetaChip("$it turns") }
            }
        }
    }
}

@Composable
private fun TrackCardSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Outline, RoundedCornerShape(14.dp)),
    ) {
        // Hero fills the top edge-to-edge; the parent clip rounds the corners (no inner rounding).
        Box(Modifier.fillMaxWidth().height(104.dp).background(brush))
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBar(Modifier.fillMaxWidth(0.9f).height(13.dp), brush)   // title line 1
            ShimmerBar(Modifier.fillMaxWidth(0.55f).height(13.dp), brush)  // title line 2
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBar(Modifier.size(width = 54.dp, height = 16.dp), brush, corner = 8.dp)
                ShimmerBar(Modifier.size(width = 46.dp, height = 16.dp), brush, corner = 8.dp)
            }
        }
    }
}

// ── shared track-screen helpers (used by TrackDetailScreen too) ──

/** Display title: full_name carries the layout (e.g. "Sebring School Circuit"); the trailing
 *  series tag (" - WEC"/" - ELMS"…) is dropped since dedup keeps one config per layout. */
internal fun trackTitle(t: TrackFullDto): String =
    (t.fullName?.takeIf { it.isNotBlank() } ?: t.name ?: t.eventName ?: "—")
        .replace(Regex(" - (WEC|ELMS|IMSA|GT)$"), "")
        .trim()

/** Track asset URL — absolute CDN (R2) link straight from the payload, or null.
 *  `file` stays the caller's selector: logo.svg / map.svg / card.webp / background.webp. */
internal fun trackAsset(t: TrackFullDto, file: String): String? = when (file) {
    "logo.svg" -> t.assets?.logo
    "map.svg" -> t.assets?.scheme
    "card.webp" -> t.assets?.cover
    "background.webp" -> t.assets?.background
    else -> null
}

/** ISO country code → circle-flag SVG url (null if not a 2-letter code). */
internal fun trackFlagUrl(cc: String?): String? {
    val c = cc?.trim()?.lowercase() ?: return null
    if (c.length != 2 || c.any { it !in 'a'..'z' }) return null
    return "https://cdn.jsdelivr.net/gh/HatScripts/circle-flags/flags/$c.svg"
}

@Composable
internal fun TrackSvgImage(url: String, modifier: Modifier) {
    // AsyncImage (not rememberAsyncImagePainter + state) → no per-item recomposition churn while
    // scrolling, and the shared singleton ImageLoader memory-caches the rasterised SVG across all
    // track-preview screens. Nothing is drawn until it loads (no placeholder).
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
internal fun FlagCircle(url: String, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape),
    )
}
