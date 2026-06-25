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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.TrackAttemptDto
import com.orioooneee.lmuasister.data.remote.TrackFullDto
import com.orioooneee.lmuasister.data.remote.TrackPersonalDto
import com.orioooneee.lmuasister.ui.components.BlockSkeleton
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.components.classColorFor
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.profile.SteamLoginUiState
import com.orioooneee.lmuasister.ui.profile.SteamLoginViewModel
import com.orioooneee.lmuasister.ui.profile.versionFullLabel
import com.orioooneee.lmuasister.ui.profile.versionPatchWildcardLabel
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatIsoDateTime
import com.orioooneee.lmuasister.ui.util.formatKm
import com.orioooneee.lmuasister.ui.util.formatLap
import org.koin.compose.koinInject

@Composable
fun TrackDetailScreen(
    viewModel: SteamLoginViewModel,
    insets: PaddingValues,
    trackId: String,
    onBack: () -> Unit,
    onOpenRace: (eventId: String, split: Int?) -> Unit,
) {
    val repo = koinInject<RaceRepository>()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val auth = uiState.authAvailability()
    // Re-keyed on auth so the personal record loads in once auth lands (e.g. after a cold-start restore).
    val result by produceState<Result<TrackDetailData>?>(null, trackId, auth) {
        fun paint(track: TrackFullDto, personal: PersonalRecordsState) {
            value = Result.success(TrackDetailData(track, personal))
        }

        // 1) Paint instantly: cached personal detail for an active/restoring session, else the public
        // track block from the roster. While auth is restoring, keep personal records in loading state.
        viewModel.cachedTrackDetail(trackId)?.let { cached ->
            paint(
                cached.track,
                when (auth) {
                    AuthAvailability.SignedOut -> PersonalRecordsState.SignedOut
                    else -> PersonalRecordsState.Ready(cached.personal)
                },
            )
        }
        if (value == null) {
            publicTrack(repo, trackId)?.let {
                paint(
                    it,
                    when (auth) {
                        AuthAvailability.SignedOut -> PersonalRecordsState.SignedOut
                        else -> PersonalRecordsState.Loading
                    },
                )
            }
        }
        // 2) Only hit the personal endpoint when signed in — avoids withReauth's 60s token wait when signed out.
        if (auth == AuthAvailability.SignedIn) {
            val fresh = runCatching { viewModel.trackDetail(trackId) }
            when {
                fresh.isSuccess -> fresh.getOrNull()?.let { paint(it.track, PersonalRecordsState.Ready(it.personal)) }
                value == null -> value = Result.failure(fresh.exceptionOrNull() ?: IllegalStateException("Couldn't load this track"))
                value?.getOrNull()?.personal is PersonalRecordsState.Loading -> {
                    val message = fresh.exceptionOrNull()?.message ?: "Couldn't load your records"
                    value?.getOrNull()?.track?.let { paint(it, PersonalRecordsState.Error(message)) }
                }
            }
        } else if (auth == AuthAvailability.SignedOut && value == null) {
            // Signed out and no public block available → show an error instead of a forever loader.
            value = Result.failure(IllegalStateException("Couldn't load this track"))
        }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        val track = result?.getOrNull()?.track
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                track?.let { trackTitle(it) } ?: "Track",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val res = result) {
            null -> TrackDetailSkeleton()
            else -> res.fold(
                onSuccess = { TrackContent(it, insets.calculateBottomPadding(), onOpenRace) },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(it.message ?: "Couldn't load this track", style = MaterialTheme.typography.bodyMedium, color = TextMed)
                    }
                },
            )
        }
    }
}

private enum class AuthAvailability { Pending, SignedIn, SignedOut }

private fun SteamLoginUiState.authAvailability(): AuthAvailability = when (this) {
    SteamLoginUiState.Restoring,
    SteamLoginUiState.Loading -> AuthAvailability.Pending
    is SteamLoginUiState.SignedIn -> AuthAvailability.SignedIn
    SteamLoginUiState.Idle,
    is SteamLoginUiState.Error,
    is SteamLoginUiState.GuardRequired,
    is SteamLoginUiState.DeviceConfirmationPending -> AuthAvailability.SignedOut
}

private data class TrackDetailData(
    val track: TrackFullDto,
    val personal: PersonalRecordsState,
)

private sealed interface PersonalRecordsState {
    data object Loading : PersonalRecordsState
    data object SignedOut : PersonalRecordsState
    data class Ready(val personal: TrackPersonalDto?) : PersonalRecordsState
    data class Error(val message: String) : PersonalRecordsState
}

/** The public reference block for a track (cached roster first, then a network refresh). No auth. */
private suspend fun publicTrack(repo: RaceRepository, trackId: String): TrackFullDto? {
    fun List<TrackFullDto>?.find() = this?.firstOrNull { it.id == trackId || it.base == trackId || it.code == trackId }
    return repo.cachedTracks().find()
        ?: runCatching { repo.tracks().getOrNull() }.getOrNull().find()
}

@Composable
private fun TrackContent(
    d: TrackDetailData,
    bottomInset: androidx.compose.ui.unit.Dp,
    onOpenRace: (String, Int?) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TrackCard(d.track) }
        when (val personalState = d.personal) {
            PersonalRecordsState.Loading -> item { PersonalRecordsSkeleton() }
            PersonalRecordsState.SignedOut -> item { Hint("Sign in to see your records on this track.") }
            is PersonalRecordsState.Error -> item {
                Hint(personalState.message.takeIf { it.isNotBlank() } ?: "Couldn't load your records right now.")
            }
            is PersonalRecordsState.Ready -> {
                val personal = personalState.personal
                if (personal == null || personal.races <= 0) {
                    item { Hint("No lap times recorded on this track yet.") }
                } else {
                    item { RacesHeader(personal.races, personal.laps, personal.distanceKm) }
                    val bestEver = personal.bestLapEver ?: personal.bestLap
                    val bestCurrentPatch = personal.bestLapCurrentPatch
                    val currentIsEver = bestCurrentPatch != null && bestEver != null && sameAttempt(bestCurrentPatch, bestEver)
                    if (!currentIsEver) {
                        bestCurrentPatch?.takeIf { it.bestLapMs != null }?.let { current ->
                            item { SectionLabel("BEST CURRENT PATCH") }
                            item {
                                BestLapCard(
                                    best = current,
                                    byClass = personal.bestByClass,
                                    versionLabel = (versionFullLabel(current.gameVersion) ?: versionFullLabel(personal.currentPatch))?.let { "v$it" },
                                    onOpenRace = onOpenRace,
                                )
                            }
                        }
                    }
                    bestEver?.takeIf { it.bestLapMs != null }?.let { ever ->
                        item { SectionLabel("BEST EVER") }
                        item {
                            BestLapCard(
                                best = ever,
                                byClass = personal.bestByClass,
                                versionLabel = versionPatchWildcardLabel(ever.gameVersion),
                                onOpenRace = onOpenRace,
                            )
                        }
                    }
                    if (personal.bestByClass.isNotEmpty()) {
                        item { SectionLabel("BEST BY CLASS") }
                        item { ByClassCard(personal.bestByClass, onOpenRace) }
                    }
                    if (personal.recent.isNotEmpty()) {
                        item { SectionLabel("RECENT") }
                        item { RecentCard(personal.recent, onOpenRace) }
                    }
                }
            }
        }
    }
}

/** Opens the race-detail screen for an attempt's event, when it carries an eventId. */
private fun Modifier.openRaceOf(a: TrackAttemptDto, onOpenRace: (String, Int?) -> Unit): Modifier =
    a.eventId?.takeIf { it.isNotBlank() }?.let { id -> this.clickable { onOpenRace(id, a.split) } } ?: this

private fun sameAttempt(a: TrackAttemptDto, b: TrackAttemptDto): Boolean =
    a.bestLapMs == b.bestLapMs &&
        a.eventId == b.eventId &&
        a.session == b.session &&
        a.date == b.date

@Composable
private fun TrackCard(t: TrackFullDto) {
    val logo = trackAsset(t, "logo.svg")
    val map = trackAsset(t, "map.svg")
    val bg = trackAsset(t, "background.webp")
    val flag = trackFlagUrl(t.countryCode)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Outline, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TrackPreview(
            backgroundUrl = bg, mapUrl = map, logoUrl = logo, flagUrl = flag,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            height = 180.dp, emblemHeight = 30.dp, flagSize = 26.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(trackTitle(t), style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().basicMarquee(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                t.type?.let { MetaChip(it) }
                t.lengthKm?.let { MetaChip("$it km") }
                t.corners?.let { MetaChip("$it turns") }
                t.openingYear?.let { MetaChip("est. $it") }
            }
        }
    }
}

@Composable
private fun BestLapCard(
    best: TrackAttemptDto,
    byClass: Map<String, TrackAttemptDto>,
    versionLabel: String?,
    onOpenRace: (String, Int?) -> Unit,
) {
    // The absolute best may belong to a faster class than the driver's usual one — flag that.
    val fasterClass = best.carClass?.takeIf {  byClass.size > 1 }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Outline, RoundedCornerShape(14.dp)).openRaceOf(best, onOpenRace).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(lap(best.bestLapMs), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace), color = TextHigh, fontWeight = FontWeight.Bold)
            best.carClass?.let { ClassBadge(it) }
            versionLabel?.let { MetaChip(it) }
        }
        attemptMeta(best, includeVersion = false)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextMed) }
        if (fasterClass != null) {
            Text("Absolute best — set in $fasterClass.", style = MaterialTheme.typography.labelSmall, color = TextLow)
        }
    }
}

@Composable
private fun ByClassCard(byClass: Map<String, TrackAttemptDto>, onOpenRace: (String, Int?) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Outline, RoundedCornerShape(12.dp)),
    ) {
        byClass.entries.sortedBy { it.value.bestLapMs ?: Long.MAX_VALUE }.forEachIndexed { i, (cls, a) ->
            Row(
                modifier = Modifier.fillMaxWidth().background(if (i % 2 == 1) Surface2 else Color.Transparent).openRaceOf(a, onOpenRace).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ClassBadge(cls)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    a.car?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextMed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(lap(a.bestLapMs), style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace), color = TextHigh, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentCard(recent: List<TrackAttemptDto>, onOpenRace: (String, Int?) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Outline, RoundedCornerShape(12.dp)),
    ) {
        recent.forEachIndexed { i, a ->
            Row(
                modifier = Modifier.fillMaxWidth().background(if (i % 2 == 1) Surface2 else Color.Transparent).openRaceOf(a, onOpenRace).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                a.carClass?.let { ClassBadge(it) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    a.eventTitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextHigh, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    attemptMeta(a)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextLow) }
                }
                Text(lap(a.bestLapMs), style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = TextMed)
            }
        }
    }
}

@Composable
private fun RacesHeader(races: Int, laps: Int, distanceKm: Double) {
    val parts = buildList {
        add("$races races")
        if (laps > 0) add("$laps laps")
        if (distanceKm > 0) add("${formatKm(distanceKm)} km")
    }
    Text(
        parts.joinToString(" · ") + " on this track",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMed,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = TextLow, fontWeight = FontWeight.Bold)
}

@Composable
private fun TrackDetailSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlockSkeleton(brush, 230.dp)  // track card
        ShimmerBar(Modifier.fillMaxWidth(0.5f).height(16.dp), brush)
        BlockSkeleton(brush, 90.dp)   // best lap
        BlockSkeleton(brush, 120.dp)  // by class / recent
    }
}

@Composable
private fun PersonalRecordsSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerBar(Modifier.fillMaxWidth(0.45f).height(16.dp), brush)
        BlockSkeleton(brush, 90.dp)
        BlockSkeleton(brush, 120.dp)
    }
}

@Composable
private fun Hint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).border(1.dp, Outline, RoundedCornerShape(12.dp)).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMed)
    }
}

@Composable
private fun ClassBadge(carClass: String) {
    val c = classColorFor(carClass)
    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(c).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(carClass.uppercase(), style = MaterialTheme.typography.labelSmall, color = onBadgeText(c), fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** "session · tier · version · date" for an attempt (skips blanks). */
private fun attemptMeta(a: TrackAttemptDto, includeVersion: Boolean = true): String? {
    val parts = buildList {
        a.session?.takeIf { it.isNotBlank() }?.let { add(it.replaceFirstChar(Char::uppercaseChar)) }
        a.tier?.takeIf { it.isNotBlank() }?.let { add(it.replaceFirstChar(Char::uppercaseChar)) }
        if (includeVersion) versionFullLabel(a.gameVersion)?.let { add("v$it") }
        formatIsoDateTime(a.date)?.let { add(it) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun lap(ms: Long?): String = if (ms != null && ms > 0) formatLap(ms) else "—"
