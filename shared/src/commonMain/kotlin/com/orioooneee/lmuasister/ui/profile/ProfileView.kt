package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.remote.RaceSessionsDto
import com.orioooneee.lmuasister.data.remote.RatingDto
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
import com.orioooneee.lmuasister.data.remote.SessionSummaryDto
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.TrackLogoIndex
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.SectionHeader
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.classColorFor
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import androidx.compose.material3.Icon
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.SkillBeginner
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.susp_active_count
import lmuassister.shared.generated.resources.susp_banned
import lmuassister.shared.generated.resources.susp_license_clean
import lmuassister.shared.generated.resources.susp_no_active
import lmuassister.shared.generated.resources.susp_past_count
import org.jetbrains.compose.resources.stringResource

private val PosGreen = SkillBeginner
private val NegRed = ClassHyper
private val Gold = Color(0xFFE6B422)
private val Silver = Color(0xFFC9D1DA)
private val Bronze = Color(0xFFCD7F32)
private val Platinum = Color(0xFF6FE3F0)

/** Metal colour for a rating rank (Bronze/Silver/Gold/Platinum) by first letter. */
private fun rankColor(rank: String): Color = when (rank.trim().firstOrNull()?.lowercaseChar()) {
    'b' -> Bronze
    's' -> Silver
    'g' -> Gold
    'p' -> Platinum
    else -> TextMed
}

private fun roman(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
    else -> n.toString()
}

private fun prettyBadge(badge: String): String =
    badge.split('-', '_').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

/** How many recent races to show inline before the "See more" button. */
private const val RECENT_PREVIEW = 3

@Composable
fun ProfileView(
    profile: SteamProfile,
    accountName: String,
    onSeeAllRaces: () -> Unit = {},
    onOpenRace: (eventId: String, split: Int?) -> Unit = { _, _ -> },
    onOpenSuspensions: (active: Boolean) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileHeader(profile, accountName, onOpenSuspensions)
        RatingsRow(profile.driverRating, profile.safetyRating)

        if (profile.recentRaces.isNotEmpty()) {
            SectionHeader("Recent races")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.recentRaces.take(RECENT_PREVIEW).forEach { race ->
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = race.eventId != null) {
                                race.eventId?.let { onOpenRace(it, race.split) }
                            },
                    ) {
                        RaceHistoryRow(race)
                    }
                }
                if (profile.recentRaces.size > RECENT_PREVIEW) {
                    SeeMoreButton(onSeeAllRaces)
                }
            }
        }
    }
}

/** "See all" CTA under the recent-races preview → opens the full paginated history. */
@Composable
private fun SeeMoreButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "See all races",
            style = MaterialTheme.typography.labelLarge,
            color = Amber,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Shimmer placeholder shown while the profile loads (and no cache to paint). */
@Composable
fun ProfileSkeleton() {
    val brush = shimmerBrush()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ShimmerBar(Modifier.size(68.dp), brush, corner = 34.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBar(Modifier.fillMaxWidth(0.6f).height(22.dp), brush)
                ShimmerBar(Modifier.fillMaxWidth(0.35f).height(13.dp), brush)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBar(Modifier.weight(1f).height(92.dp), brush, corner = 14.dp)
            ShimmerBar(Modifier.weight(1f).height(92.dp), brush, corner = 14.dp)
        }
        ShimmerBar(Modifier.fillMaxWidth(0.4f).height(18.dp), brush)
        repeat(3) {
            ShimmerBar(Modifier.fillMaxWidth().height(72.dp), brush, corner = 12.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader(profile: SteamProfile, accountName: String, onOpenSuspensions: (active: Boolean) -> Unit) {
    val accent = rankColor(profile.driverRating?.rank.orEmpty())
    val name = profile.displayName ?: profile.name ?: profile.username ?: accountName.ifBlank { "Driver" }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CountryFlag(profile.nationality, accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val badges = profile.badges.ifEmpty { listOfNotNull(profile.badge) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                badges.take(3).forEach { OutlinePill(prettyBadge(it), Amber) }
                RatingPill("DR", profile.driverRating)
                RatingPill("SR", profile.safetyRating)
            }
            SuspensionFlags(profile, onOpenSuspensions)
        }
    }
}

/** Round flag avatar — the player's country, cropped into a circle. */
@Composable
private fun CountryFlag(nationality: String?, accent: Color) {
    val box = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(Surface2)
        .border(2.dp, accent.copy(alpha = 0.7f), CircleShape)
    val cc = nationality?.takeIf { it.length == 2 && it.all(Char::isLetter) }?.lowercase()
    if (cc == null) {
        Box(box, contentAlignment = Alignment.Center) {
            Icon(IconFlag, contentDescription = null, tint = TextLow, modifier = Modifier.size(26.dp))
        }
        return
    }
    AsyncImage(
        model = "https://flagcdn.com/w160/$cc.png",
        contentDescription = nationality,
        contentScale = ContentScale.Crop, // fill the circle
        modifier = box,
    )
}

/** Two-segment rating badge in the same style as the schedule's SrBadge: "DR | Bronze III". */
@Composable
private fun RatingPill(label: String, rating: RatingDto?) {
    if (rating == null || rating.rank.isBlank()) return
    val color = rankColor(rating.rank)
    // Compact in-game style: first letter of the rank + tier number, e.g. "B3", "S2".
    val letter = rating.rank.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val value = letter + if (rating.tier > 0) rating.tier.toString() else ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, Outline, RoundedCornerShape(6.dp)),
    ) {
        Box(Modifier.background(Surface3).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMed)
        }
        Box(Modifier.background(color).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(
                value.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = onBadgeText(color),
                maxLines = 1,
            )
        }
    }
}

private val FlagGray = Color(0xFF8A8F98)

/**
 * Licence-status flags. Active sanctions → red (the most severe state, shown first);
 * past/expired sanctions → gray; a clean licence → green. Tapping the active or past
 * flag opens the history filtered to just that subset. Falls back to
 * [SteamProfile.activeSuspensions] when the detailed list is missing (older cached profile).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuspensionFlags(profile: SteamProfile, onOpen: (active: Boolean) -> Unit) {
    val all = profile.suspensions
    val active = all.filter { it.active }
    val past = all.filter { !it.active }

    // No detailed list (e.g. legacy cache): keep the simple count-based status.
    if (all.isEmpty()) {
        if (profile.activeSuspensions > 0) {
            ClickablePill(stringResource(Res.string.susp_active_count, profile.activeSuspensions)) { onOpen(true) }
        } else {
            ClickablePill(stringResource(Res.string.susp_license_clean), PosGreen, onClick = null)
        }
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (active.isNotEmpty()) {
            val label = if (active.any { it.permanent }) stringResource(Res.string.susp_banned)
            else stringResource(Res.string.susp_active_count, active.size)
            ClickablePill(label, NegRed) { onOpen(true) }
        } else {
            // No active sanction right now → green status, nothing to drill into.
            ClickablePill(stringResource(Res.string.susp_no_active), PosGreen, onClick = null)
        }
        if (past.isNotEmpty()) {
            ClickablePill(stringResource(Res.string.susp_past_count, past.size), FlagGray) { onOpen(false) }
        }
    }
}

/** Outline pill that opens the detail screen on tap (non-clickable when [onClick] is null). */
@Composable
private fun ClickablePill(text: String, color: Color = NegRed, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.clip(shape).background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun RatingsRow(driver: RatingDto?, safety: RatingDto?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RatingCard("Driver Rating", driver, Modifier.weight(1f))
        RatingCard("Safety Rating", safety, Modifier.weight(1f))
    }
}

@Composable
private fun RatingCard(label: String, rating: RatingDto?, modifier: Modifier = Modifier) {
    val color = rankColor(rating?.rank.orEmpty())
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLow)
        if (rating == null) {
            Text("—", style = MaterialTheme.typography.titleLarge, color = TextMed)
            return@Column
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                rating.rank.ifBlank { "Unranked" },
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Black,
            )
            if (rating.tier > 0) {
                Text(roman(rating.tier), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            }
        }
        rating.progress?.let { ProgressBar(it.toFloat() / 100f, color) }
        rating.elo?.let {
            Text("ELO ${it.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextMed)
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Surface3),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(color),
            )
        }
        Text(
            "${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = TextMed,
        )
    }
}

@Composable
internal fun RaceHistoryRow(race: RecentRaceDto) {
    // Non-finish (DNF/DQ/DNS): the race result itself is meaningless, so hide the
    // race-session line and the grid→finish delta (quali + rating deltas still apply).
    val finished = statusLabel(race.finishStatus) == null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PositionBadge(race.position, race.fieldSize, race.finishStatus)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                race.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().basicMarquee(), // scroll long titles instead of truncating
            )
            CarLine(race.carClass, race.carName ?: race.car)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TrackLogo(race.trackLogo, race.track)
                race.track?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = TextMed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Lap chip only when there's no per-session breakdown to show it in.
                if (race.sessions == null) race.bestLapMs?.let { MetaChip("⏱ ${formatLap(it)}") }
            }
            SessionsBreakdown(race.sessions, includeRace = finished)
        }
        // Right rail, in-flow (no overlap with the times block): event-type on top,
        // rating deltas in the middle, split at the bottom.
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            race.eventType?.takeIf { it.isNotBlank() }?.let { MetaChip(it.replaceFirstChar(Char::uppercaseChar)) }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (finished) GridToFinish(race.gridPosition, race.position)
                DeltaText("DR", race.drChange)
                DeltaText("SR", race.srChange)
            }
            race.split?.let { MetaChip(splitLabel(it, race.totalSplits)) }
        }
    }
}

/** Car-class badge first, then the model name (scrolls when it doesn't fit). */
@Composable
private fun CarLine(carClass: String?, carName: String?) {
    val name = carName?.takeIf { it.isNotBlank() }
    if (carClass == null && name == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        carClass?.let { ClassPill(it) }
        name?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = TextMed,
                maxLines = 1,
                modifier = Modifier.weight(1f).basicMarquee(),
            )
        }
    }
}

/** Per-session breakdown: Quali / Race (and Practice) — position + best lap. */
@Composable
private fun SessionsBreakdown(sessions: RaceSessionsDto?, includeRace: Boolean) {
    if (sessions == null) return
    // Practice intentionally omitted — only Quali + Race matter on the card.
    val rows = listOfNotNull(
        sessions.qualifying?.let { "Quali" to it },
        if (includeRace) sessions.race?.let { "Race" to it } else null,
    ).filter { (_, s) -> s.position != null || s.bestLapMs != null || s.finishTimeMs != null }
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { (label, s) -> SessionLine(label, s) }
    }
}

@Composable
private fun SessionLine(label: String, s: SessionSummaryDto) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(46.dp),
        )
        Text(
            s.position?.toString() ?: "—",
            style = MaterialTheme.typography.labelMedium,
            color = TextHigh,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(32.dp), // fixed so lap times line up
        )
        (s.bestLapMs ?: s.finishTimeMs)?.let {
            Text(
                formatLap(it),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = TextMed,
            )
        }
    }
}

/** Track emblem (SVG) — same approach as the race-details screen: only shown once Coil
 *  has it (a missing/failed asset just leaves no gap). */
@Composable
private fun TrackLogo(url: String?, trackName: String? = null) {
    // Profile cards often come back with no track_logo → fall back to the schedule's
    // cached emblem for the same track (matched by normalised name).
    val abs = absUrl(url) ?: TrackLogoIndex.lookup(trackName) ?: return
    val painter = rememberAsyncImagePainter(model = abs)
    val state by painter.state.collectAsState()
    if (state is AsyncImagePainter.State.Success) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(20.dp).widthIn(max = 44.dp),
        )
    }
}

/** Positions gained/lost: "P5→P2" green if moved up, red if dropped. */
@Composable
private fun GridToFinish(grid: Int?, finish: Int) {
    if (grid == null || finish <= 0) return
    val gained = grid - finish
    val color = when {
        gained > 0 -> PosGreen
        gained < 0 -> NegRed
        else -> TextMed
    }
    Text("$grid→$finish", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
}

private fun absUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http") -> path
    else -> BuildConfig.BACKEND_URL.substringBefore("/api/", BuildConfig.BACKEND_URL).trimEnd('/') + path
}

/** "Split 14/29" when the total is known, else "Split 14". */
private fun splitLabel(split: Int, total: Int?): String =
    if (total != null && total > 0) "Split $split/$total" else "Split $split"

/** Lap milliseconds → "M:SS.mmm". */
private fun formatLap(ms: Long): String {
    val m = ms / 60000
    val s = (ms % 60000) / 1000
    val mil = ms % 1000
    return "$m:${s.toString().padStart(2, '0')}.${mil.toString().padStart(3, '0')}"
}

@Composable
private fun PositionBadge(position: Int, fieldSize: Int, finishStatus: String?) {
    val dnf = statusLabel(finishStatus)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (dnf != null) {
            // Did-not-finish / disqualified etc. — a dark badge with the status instead of a place.
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)
                    .border(1.dp, NegRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(dnf, style = MaterialTheme.typography.titleSmall, color = NegRed, fontWeight = FontWeight.Black)
            }
            return@Column
        }
        val podium = when (position) {
            1 -> Gold; 2 -> Silver; 3 -> Bronze
            else -> Surface3
        }
        val onPodium = position in 1..3
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(if (onPodium) podium else Surface2)
                .border(1.dp, if (onPodium) podium else Outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (position > 0) position.toString() else "—",
                style = MaterialTheme.typography.titleLarge,
                color = if (onPodium) onBadgeText(podium) else TextHigh,
                fontWeight = FontWeight.Black,
            )
        }
        if (fieldSize > 0) {
            Spacer(Modifier.height(2.dp))
            Text("of $fieldSize", style = MaterialTheme.typography.labelSmall, color = TextLow)
        }
    }
}

/** Short badge label for a non-finish (DNF/DQ/DNS), or null for a normal finish (→ show position). */
private fun statusLabel(status: String?): String? {
    val s = status?.trim()?.lowercase().orEmpty()
    if (s.isEmpty()) return null
    return when {
        "disq" in s || s == "dq" -> "DQ"
        "dns" in s || "didnotstart" in s -> "DNS"
        // check DNF before the generic "finish" rule (DidNotFinish contains "finish")
        "dnf" in s || "didnotfinish" in s || "retired" in s -> "DNF"
        "finish" in s || s in setOf("classified", "running", "completed", "ok", "ok ") -> null
        else -> s.uppercase().take(4)
    }
}

@Composable
private fun ClassPill(carClass: String) {
    val c = classColorFor(carClass)
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(c).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            carClass.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = onBadgeText(c),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun OutlinePill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun DeltaText(label: String, delta: Double?) {
    if (delta == null) return
    val color = when {
        delta > 0 -> PosGreen
        delta < 0 -> NegRed
        else -> TextMed
    }
    val arrow = when {
        delta > 0 -> "▲"
        delta < 0 -> "▼"
        else -> "•"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow)
        Text(
            "$arrow ${formatDelta(delta)}",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** One decimal, no trailing ".0" noise beyond that. */
private fun formatDelta(d: Double): String {
    val abs = if (d < 0) -d else d
    val rounded = (abs * 10).toLong()
    return "${rounded / 10}.${rounded % 10}"
}
