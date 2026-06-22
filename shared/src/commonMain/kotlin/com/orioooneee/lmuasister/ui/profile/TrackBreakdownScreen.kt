package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.TrackBreakdownDto
import com.orioooneee.lmuasister.data.remote.TrackFullDto
import com.orioooneee.lmuasister.ui.TrackLogoIndex
import com.orioooneee.lmuasister.ui.tracks.TrackPreview
import com.orioooneee.lmuasister.ui.tracks.trackFlagUrl
import org.koin.compose.koinInject
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
    val tracks = profile?.trackBreakdown

    // The breakdown often carries no logo_url, so match each track to the full reference roster
    // (same source the Tracks screen uses) and derive the reliable /track/<id>/logo.svg emblem.
    val repo = koinInject<RaceRepository>()
    val roster by produceState(repo.cachedTracks()) {
        repo.cachedTracks()?.let { value = it }
        repo.tracks().getOrNull()?.let { value = it }
    }
    val logoByName = remember(roster) { rosterLogoMap(roster) }

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
                items(tracks, key = { it.trackId ?: it.track }) { TrackBreakdownCard(it, logoByName, onOpenTrack) }
            }
        }
    }
}

@Composable
private fun TrackGrid(insets: PaddingValues, content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        // ~160dp min → 2 per row on a phone, more on wider screens.
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackBreakdownCard(t: TrackBreakdownDto, logoByName: Map<String, String>, onOpenTrack: (String) -> Unit) {
    // Emblem: backend logo_url if present, else the roster's id-derived logo, else the schedule cache.
    val emblemUrl = absUrl(t.logoUrl) ?: logoByName[normalizeName(t.track)] ?: TrackLogoIndex.lookup(t.track)
    val mapUrl = emblemUrl?.takeIf { "logo.svg" in it }?.replace("logo.svg", "map.svg")
    val bgUrl = emblemUrl?.takeIf { "logo.svg" in it }?.replace("logo.svg", "background.webp")
    // Navigate by the backend track_id; if a stale payload omits it, pull the id out of the asset URL.
    val trackId = t.trackId?.takeIf { it.isNotBlank() }
        ?: emblemUrl?.substringAfter("/track/", "")?.substringBefore("/")?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .clickable(enabled = trackId != null) { trackId?.let(onOpenTrack) },
    ) {
        TrackPreview(
            backgroundUrl = bgUrl, mapUrl = mapUrl, logoUrl = emblemUrl,
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

/** {normalised track name → absolute logo url} built from the reference roster, deriving the
 *  emblem from each track's id exactly like the Tracks screen (the reliable source). */
private fun rosterLogoMap(roster: List<TrackFullDto>?): Map<String, String> {
    val m = HashMap<String, String>()
    roster?.forEach { t ->
        val id = t.id.takeIf { it.isNotBlank() } ?: return@forEach
        val url = t.assets?.logo?.let { absUrl(it) } ?: (BuildConfig.BACKEND_URL.trimEnd('/') + "/track/$id/logo.svg")
        listOfNotNull(t.fullName, t.name, t.eventName).forEach { n ->
            normalizeName(n)?.let { m.putIfAbsent(it, url) }
        }
    }
    return m
}

/** Lowercase + strip everything but a–z/0–9, so the breakdown name matches a roster name. */
private fun normalizeName(s: String?): String? =
    s?.lowercase()?.replace(Regex("[^a-z0-9]"), "")?.ifBlank { null }

private fun absUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http") -> path
    else -> BuildConfig.BACKEND_URL.substringBefore("/api/", BuildConfig.BACKEND_URL).trimEnd('/') + path
}
