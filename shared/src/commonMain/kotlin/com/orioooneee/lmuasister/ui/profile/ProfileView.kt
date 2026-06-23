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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.orioooneee.lmuasister.data.remote.RaceSessionsDto
import com.orioooneee.lmuasister.data.remote.RatingDto
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.TrackLogoIndex
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.RankBadge
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
import com.orioooneee.lmuasister.ui.util.formatIsoTimeAndDate
import com.orioooneee.lmuasister.ui.util.formatKm
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.susp_active_count
import lmuassister.shared.generated.resources.susp_banned
import lmuassister.shared.generated.resources.susp_license_clean
import lmuassister.shared.generated.resources.susp_no_active
import lmuassister.shared.generated.resources.susp_past_count
import org.jetbrains.compose.resources.stringResource

private val PosGreen = SkillBeginner
private val NegRed = ClassHyper
// Official LMU rank-tier colors (from the in-game DR/SR badge SVGs).
private val Gold = Color(0xFFE1A01F)
private val Silver = Color(0xFF8F9499)
private val Bronze = Color(0xFF977548)
private val Platinum = Color(0xFF89B2DD)

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

private const val RECENT_PREVIEW = 3

@Composable
fun ProfileView(
    profile: SteamProfile,
    accountName: String,
    onSeeAllRaces: () -> Unit = {},
    onOpenRace: (eventId: String, split: Int?) -> Unit = { _, _ -> },
    onOpenSuspensions: (active: Boolean) -> Unit = {},
    onOpenCategory: (StatCategory) -> Unit = {},
    onOpenTracks: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileHeader(profile, accountName, onOpenSuspensions, onOpenTracks)
        RatingsRow(profile.driverRating, profile.safetyRating)

        profile.ratingHistory?.takeIf { it.dr.isNotEmpty() || it.sr.isNotEmpty() }?.let {
            RatingProgressionCard(it)
        }
        profile.stats?.total?.let { CareerStatsGrid(it, onOpenCategory = onOpenCategory) }

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
private fun ProfileHeader(
    profile: SteamProfile,
    accountName: String,
    onOpenSuspensions: (active: Boolean) -> Unit,
    onOpenTracks: () -> Unit,
) {
    val accent = rankColor(profile.driverRating?.rank.orEmpty())
    val name = profile.displayName ?: profile.name ?: profile.username ?: accountName.ifBlank { "Driver" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Avatar sized to the name line, sitting inline with it.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CountryFlag(profile.nationality, accent, size = 28.dp)
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        // Ratings + distance first, flush to the left edge (not indented under the avatar).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RatingPill("DR", profile.driverRating)
            RatingPill("SR", profile.safetyRating)
            // Tap the distance badge to open the per-track breakdown screen.
            if (profile.totalDistanceKm > 0) {
                ClickablePill("${formatKm(profile.totalDistanceKm)} km", DistAccent, onClick = onOpenTracks)
            }
        }
        // Status badges (e.g. "Sr Probation") sit on the SAME row as the suspensions, after them.
        val badges = profile.badges.ifEmpty { listOfNotNull(profile.badge) }
        SuspensionFlags(profile, onOpenSuspensions) {
            badges.take(3).forEach { OutlinePill(prettyBadge(it), Amber) }
        }
    }

}


@Composable
private fun CountryFlag(nationality: String?, accent: Color, size: Dp = 68.dp) {
    val box = Modifier
        .size(size)
        .clip(CircleShape)
        .background(Surface2)
        .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape)
    val cc = nationality?.takeIf { it.length == 2 && it.all(Char::isLetter) }?.lowercase()
    if (cc == null) {
        Box(box, contentAlignment = Alignment.Center) {
            Icon(IconFlag, contentDescription = null, tint = TextLow, modifier = Modifier.size(size * 0.5f))
        }
        return
    }
    AsyncImage(
        model = "https://flagcdn.com/w160/$cc.png",
        contentDescription = nationality,
        contentScale = ContentScale.Crop,
        modifier = box,
    )
}

@Composable
private fun RatingPill(label: String, rating: RatingDto?) {
    if (rating == null || rating.rank.isBlank()) return
    val letter = rating.rank.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    RankBadge(label, (letter + rating.tier.toString()).uppercase(), rankColor(rating.rank))
}

private val FlagGray = Color(0xFF8A8F98)
private val DistAccent = Color(0xFF7FB2E8) // total distance badge — soft blue

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuspensionFlags(
    profile: SteamProfile,
    onOpen: (active: Boolean) -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val all = profile.suspensions
    val active = all.filter { it.active }
    val past = all.filter { !it.active }

    // One row: suspension status first, then any [trailing] badges (wrap if they don't fit).
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (all.isEmpty()) {
            // No detailed list (e.g. legacy cache): keep the simple count-based status.
            if (profile.activeSuspensions > 0) {
                ClickablePill(stringResource(Res.string.susp_active_count, profile.activeSuspensions)) { onOpen(true) }
            } else {
                ClickablePill(stringResource(Res.string.susp_license_clean), PosGreen, onClick = null)
            }
        } else {
            if (active.isNotEmpty()) {
                val label = if (active.any { it.permanent }) stringResource(Res.string.susp_banned)
                else stringResource(Res.string.susp_active_count, active.size)
                ClickablePill(label, NegRed) { onOpen(true) }
            } else {
                ClickablePill(stringResource(Res.string.susp_no_active), PosGreen, onClick = null)
            }
            if (past.isNotEmpty()) {
                ClickablePill(stringResource(Res.string.susp_past_count, past.size), FlagGray) { onOpen(false) }
            }
        }
        trailing()
    }
}

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
        // History shows the in-class finishing position. The denominator is the CLASS field size;
        // field_size is the overall grid (all classes), so only show "of N" when the backend sends
        // the class count — otherwise no denominator at all.
        val classPos = race.classRacePosition ?: race.classPosition?.takeIf { it > 0 } ?: race.position
        val subtitle = (race.classRaceSize ?: race.classFieldSize)?.takeIf { it > 0 }?.let { "of $it" }
        // Left column: date/time over the position badge, grid→finish delta underneath.
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Time on top, date below: "18:50" / "21.06.2026".
            formatIsoTimeAndDate(race.date)?.let { (time, date) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(time, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp), color = TextMed, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    // Smaller so "21.06.2026" doesn't run much wider than the time above it.
                    Text(date, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp), color = TextLow, maxLines = 1)
                }
            }
            PositionBadge(classPos, subtitle, race.finishStatus)
            if (finished) GridToFinish(race.classQualiPosition, race.classRacePosition ?: race.classPosition)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                race.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().basicMarquee(),
            )
            CarLine(race.carClass, race.carName ?: race.car)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TrackLogo(race.trackLogo, race.track)
                race.track?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = TextMed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (race.sessions == null) race.bestLapMs?.let { MetaChip("⏱ ${formatLap(it)}") }
            }
            SessionsBreakdown(race.sessions, includeRace = finished)
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            race.eventType?.takeIf { it.isNotBlank() }?.let { MetaChip(it.replaceFirstChar(Char::uppercaseChar)) }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                DeltaText("DR", race.drChange)
                DeltaText("SR", race.srChange)
            }
            race.split?.let { MetaChip(splitLabel(it, race.totalSplits)) }
        }
    }
}

// Class tokens to drop from a car name — the class is already shown as a separate pill.
private val CAR_CLASS_TOKENS = Regex("""\b(LMGT3|LMGTE|LMGT|LMP1|LMP2|LMP3|LMDh|LMH|GTE|GT3|GTP|Hypercar)\b""", RegexOption.IGNORE_CASE)

/** "Ford Mustang LMGT3" → "Ford Mustang", "McLaren 720S LMGT3 Evo" → "McLaren 720S Evo". */
private fun stripCarClass(name: String): String =
    name.replace(CAR_CLASS_TOKENS, "").replace(Regex("\\s{2,}"), " ").trim()

@Composable
private fun CarLine(carClass: String?, carName: String?) {
    val name = carName?.takeIf { it.isNotBlank() }?.let { stripCarClass(it) }?.takeIf { it.isNotBlank() }
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

@Composable
private fun SessionsBreakdown(sessions: RaceSessionsDto?, includeRace: Boolean) {
    if (sessions == null) return
    // In-class positions (times dropped) on a single row: "QUALI 11/12   RACE 10/12".
    val quali = sessions.qualifying
    val race = if (includeRace) sessions.race else null
    val qPos = quali?.let { it.classPosition ?: it.position }
    val rPos = race?.let { it.classPosition ?: it.position }
    if (qPos == null && rPos == null) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        qPos?.let { SessionPosBadge("QUALI", it, quali.classSize) }
        rPos?.let { SessionPosBadge("RACE", it, race.classSize) }
    }
}

/** Two-tone "QUALI · 11/12" / "RACE · 10/12" pill for one session's in-class position. */
@Composable
private fun SessionPosBadge(label: String, position: Int, size: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, Outline, RoundedCornerShape(6.dp)),
    ) {
        Box(Modifier.background(Surface2).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) {
            val txt = position.toString() + (size?.takeIf { it > 0 }?.let { "/$it" } ?: "")
            Text(txt, style = MaterialTheme.typography.labelMedium, color = TextHigh, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TrackLogo(url: String?, trackName: String? = null) {
    // track_logo is an absolute CDN URL now; still fall back to the schedule's cached
    // emblem for the same track (matched by normalised name) when it's null.
    val abs = url?.takeIf { it.isNotBlank() } ?: TrackLogoIndex.lookup(trackName) ?: return
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

@Composable
private fun GridToFinish(grid: Int?, finish: Int?) {
    if (grid == null || finish == null || finish <= 0) return
    val gained = grid - finish
    val color = when {
        gained > 0 -> PosGreen
        gained < 0 -> NegRed
        else -> TextMed
    }
    Text("$grid→$finish", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
}


private fun splitLabel(split: Int, total: Int?): String =
    if (total != null && total > 0) "Split $split/$total" else "Split $split"

private fun formatLap(ms: Long): String {
    val m = ms / 60000
    val s = (ms % 60000) / 1000
    val mil = ms % 1000
    return "$m:${s.toString().padStart(2, '0')}.${mil.toString().padStart(3, '0')}"
}

@Composable
private fun PositionBadge(position: Int, subtitle: String?, finishStatus: String?) {
    val dnf = statusLabel(finishStatus)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (dnf != null) {
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
        subtitle?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = TextLow)
        }
    }
}

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

private fun formatDelta(d: Double): String {
    val abs = if (d < 0) -d else d
    val rounded = (abs * 10).toLong()
    return "${rounded / 10}.${rounded % 10}"
}
