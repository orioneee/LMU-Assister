package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.orioooneee.lmuasister.data.remote.ClassificationRowDto
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.TrackDto
import com.orioooneee.lmuasister.ui.TrackLogoIndex
import com.orioooneee.lmuasister.ui.components.BlockSkeleton
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.components.classColorFor
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatLap

private val PosGreen = Color(0xFF53D769)
private val NegRed = Color(0xFFE5484D)

/** How many classification rows to show above/below the player before "show all". */
private const val WINDOW = 3

/**
 * Full detail of a single past race — track card + the player's result + per-session
 * classification (qualifying then race; practice hidden). Each table shows a window
 * around the player and expands to the full field. Data: GET /profile/race/<eventId>.
 */
@Composable
fun RaceProfileDetailScreen(
    viewModel: SteamLoginViewModel,
    eventId: String,
    split: Int?,
    onBack: () -> Unit,
) {
    val result by produceState<Result<RaceDetailDto>?>(null, eventId, split) {
        value = runCatching { viewModel.raceDetail(eventId, split, null) }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                result?.getOrNull()?.title ?: "Race",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val res = result) {
            null -> DetailSkeleton()
            else -> res.fold(
                onSuccess = { DetailContent(it) },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            it.message ?: "Couldn't load this race",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMed,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlockSkeleton(brush, 180.dp) // track card
        BlockSkeleton(brush, 120.dp) // summary
        BlockSkeleton(brush, 160.dp) // qualifying
        BlockSkeleton(brush, 200.dp) // race
    }
}

@Composable
private fun DetailContent(d: RaceDetailDto) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        d.trackInfo?.let { item { TrackCard(it) } }
        item { SummaryCard(d) }
        // Qualifying first, then the race — practice intentionally hidden.
        for (key in listOf("qualifying", "race")) {
            val session = d.sessions[key] ?: continue
            if (session.classification.isEmpty()) continue
            item { SessionCard(sessionLabel(key), session.classification) }
        }
    }
}

// ── Track card (mirrors the schedule's track card, from the detail's trackInfo) ──────

@Composable
private fun TrackCard(t: TrackDto) {
    val flag = flagFor(t.countryCode)
    val logo = absUrl(t.logoUrl ?: t.mapUrl?.let { it.substringBeforeLast("/") + "/logo.svg" })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (logo != null || flag != null) {
            Box(Modifier.fillMaxWidth()) {
                logo?.let { SvgImage(it, Modifier.fillMaxWidth().height(56.dp)) }
                flag?.let { FlagCircle(it, 24.dp, Modifier.align(Alignment.TopEnd)) }
            }
        }
        absUrl(t.mapUrl)?.let { map ->
            AsyncImage(
                model = map,
                contentDescription = "${t.name} map",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)),
            )
        }
        val name = t.simpleName?.takeIf { it.isNotBlank() } ?: t.name.takeIf { it.isNotBlank() }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            name?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                t.country?.let { MetaChip(it) }
                t.lengthKm?.let { MetaChip("$it km") }
                t.numTurns?.let { MetaChip("${it} turns") }
            }
        }
    }
}

// ── Summary: car, meta, start/finish, deltas ─────────────────────────────────────────

@Composable
private fun SummaryCard(d: RaceDetailDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            d.carClass?.let { ClassPill(it) }
            (d.carName ?: d.car)?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextHigh, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            d.eventType?.let { MetaChip(it.replaceFirstChar(Char::uppercaseChar)) }
            d.split?.let { MetaChip(if (d.totalSplits != null) "Split $it/${d.totalSplits}" else "Split $it") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            d.gridPosition?.takeIf { it > 0 }?.let { Stat("Start", "P$it") }
            d.position?.takeIf { it > 0 }?.let { Stat("Finish", "P$it" + if (d.fieldSize > 0) " / ${d.fieldSize}" else "") }
            gainLost(d.gridPosition, d.position)?.let { (txt, color) -> StatColored("+/-", txt, color) }
            DeltaStat("DR", d.drChange)
            DeltaStat("SR", d.srChange)
        }
    }
}

private fun gainLost(grid: Int?, finish: Int?): Pair<String, Color>? {
    if (grid == null || grid <= 0 || finish == null || finish <= 0) return null
    val g = grid - finish
    val color = if (g > 0) PosGreen else if (g < 0) NegRed else TextMed
    val txt = if (g > 0) "+$g" else g.toString()
    return txt to color
}

@Composable
private fun Stat(label: String, value: String) = StatColored(label, value, TextHigh)

@Composable
private fun StatColored(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLow)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeltaStat(label: String, delta: Double?) {
    if (delta == null) return
    val color = if (delta > 0) PosGreen else if (delta < 0) NegRed else TextMed
    val arrow = if (delta > 0) "▲" else if (delta < 0) "▼" else "•"
    val abs = if (delta < 0) -delta else delta
    val r = (abs * 10).toLong()
    StatColored(label, "$arrow ${r / 10}.${r % 10}", color)
}

// ── Per-session classification with a window around the player + expand/collapse ─────

@Composable
private fun SessionCard(label: String, rows: List<ClassificationRowDto>) {
    var expanded by remember { mutableStateOf(false) }
    val meIndex = rows.indexOfFirst { it.isMe }
    val shown = if (expanded || meIndex < 0) {
        rows
    } else {
        rows.subList((meIndex - WINDOW).coerceAtLeast(0), (meIndex + WINDOW).coerceAtMost(rows.lastIndex) + 1)
    }
    val canToggle = expanded || shown.size < rows.size

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface1)
                .border(1.dp, Outline, RoundedCornerShape(12.dp)),
        ) {
            shown.forEach { ClassificationLine(it) }
            if (canToggle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .background(Surface2)
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (expanded) "Show less ▴" else "Show all ${rows.size} ▾",
                        style = MaterialTheme.typography.labelMedium,
                        color = Amber,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassificationLine(r: ClassificationRowDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (r.isMe) Amber.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            r.position?.toString() ?: "—",
            style = MaterialTheme.typography.labelMedium,
            color = if (r.isMe) Amber else TextHigh,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
        )
        flagFor(r.nationality)?.let { FlagCircle(it, 16.dp) }
        Text(
            r.name ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (r.isMe) TextHigh else TextMed,
            fontWeight = if (r.isMe) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        r.carClass?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLow, maxLines = 1)
        }
        (r.bestLapMs ?: r.finishTimeMs)?.let {
            Text(
                formatLap(it),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = TextMed,
            )
        }
    }
}

// ── small shared bits ────────────────────────────────────────────────────────────────

@Composable
private fun SvgImage(url: String, modifier: Modifier) {
    val painter = rememberAsyncImagePainter(model = url)
    val state by painter.state.collectAsState()
    if (state is AsyncImagePainter.State.Success) {
        Image(painter = painter, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier)
    }
}

@Composable
private fun FlagCircle(url: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape),
    )
}

@Composable
private fun ClassPill(carClass: String) {
    val c = classColorFor(carClass)
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(c).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(carClass.uppercase(), style = MaterialTheme.typography.labelMedium, color = onBadgeText(c), fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun sessionLabel(key: String): String = when (key) {
    "race" -> "Race"
    "qualifying" -> "Qualifying"
    "practice" -> "Practice"
    else -> key.replaceFirstChar(Char::uppercaseChar)
}

private fun absUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http") -> path
    else -> BuildConfig.BACKEND_URL.substringBefore("/api/", BuildConfig.BACKEND_URL).trimEnd('/') + path
}

/** nationality is an ISO-3166 alpha-2 code → circle-flags SVG (same as the rest of the app). */
private fun flagFor(value: String?): String? {
    val cc = value?.trim()?.lowercase() ?: return null
    if (cc.length != 2 || cc.any { it !in 'a'..'z' }) return null
    return "https://cdn.jsdelivr.net/gh/HatScripts/circle-flags/flags/$cc.svg"
}
