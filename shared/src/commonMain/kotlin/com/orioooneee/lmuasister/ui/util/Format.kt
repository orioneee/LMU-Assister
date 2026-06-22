package com.orioooneee.lmuasister.ui.util

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** "#6366f1" / "#FF6366f1" -> Compose Color (null if unparseable). */
fun parseHexColor(hex: String?): Color? {
    val clean = hex?.trim()?.removePrefix("#") ?: return null
    val v = clean.toLongOrNull(16) ?: return null
    return when (clean.length) {
        6 -> Color(0xFF000000L or v)
        8 -> Color(v)
        else -> null
    }
}

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
private val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun Instant.local() = toLocalDateTime(TimeZone.currentSystemDefault())

private fun two(n: Int) = n.toString().padStart(2, '0')

/** 24-hour local format, e.g. "12 Jun 19:45". */
fun Instant.formatStart(): String {
    val dt = local()
    val mon = MONTHS.getOrElse(dt.month.ordinal) { "?" }
    return "${dt.day} $mon ${two(dt.hour)}:${two(dt.minute)}"
}

/** Epoch-ms -> "12 Jun 2026, 14:30" (local, 24h). */
fun formatEpochDateTime(ms: Long): String {
    val dt = Instant.fromEpochMilliseconds(ms).local()
    val mon = MONTHS.getOrElse(dt.month.ordinal) { "?" }
    return "${dt.day} $mon ${dt.year}, ${two(dt.hour)}:${two(dt.minute)}"
}

/** ISO-8601 instant (e.g. "2026-06-21T15:50:00Z") -> "21 Jun 2026, 17:50" (local).
 *  null when the string is absent or unparseable. */
fun formatIsoDateTime(iso: String?): String? =
    iso?.takeIf { it.isNotBlank() }
        ?.let { runCatching { formatEpochDateTime(Instant.parse(it).toEpochMilliseconds()) }.getOrNull() }

/** ISO-8601 instant -> ("18:50", "01.01.2026") local (time, date), or null if unparseable. */
fun formatIsoTimeAndDate(iso: String?): Pair<String, String>? =
    iso?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Instant.parse(it).local() }.getOrNull() }
        ?.let { dt -> "${two(dt.hour)}:${two(dt.minute)}" to "${two(dt.day)}.${two(dt.month.ordinal + 1)}.${dt.year}" }

/** Distance in km, rounded to a whole number with space-grouped thousands. Works for any
 *  magnitude: 11000.4 -> "11 000", 980.6 -> "981", 1234567.0 -> "1 234 567". */
fun formatKm(km: Double): String {
    val digits = km.roundToLong().toString()
    val sb = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.toString()
}

/** Lap time in ms -> "1:42.623". */
fun formatLap(ms: Long): String {
    if (ms <= 0) return "—"
    val m = ms / 60_000
    val s = (ms % 60_000) / 1000
    val mmm = ms % 1000
    return "$m:${two(s.toInt())}.${mmm.toString().padStart(3, '0')}"
}

/** Sector seconds -> "32.789". */
fun formatSector(sec: Double): String {
    val total = (sec * 1000).toLong()
    return "${total / 1000}.${(total % 1000).toString().padStart(3, '0')}"
}

/** Weather sky code (+ rain chance) -> emoji. */
fun skyEmoji(sky: Int, rainChance: Int): String = when {
    rainChance >= 40 || sky >= 8 -> "🌧"
    sky >= 6 -> "☁️"
    sky >= 4 -> "⛅"
    sky >= 2 -> "🌤"
    else -> "☀️"
}

/** Background colour for a weather segment (clearer = bluer, rainy = dark slate). */
fun skyColor(sky: Int, rainChance: Int): Color = when {
    rainChance >= 40 || sky >= 8 -> Color(0xFF273B52)
    sky >= 6 -> Color(0xFF323A47)
    sky >= 4 -> Color(0xFF3C4A5E)
    sky >= 2 -> Color(0xFF37597F)
    else -> Color(0xFF3E6EA5)
}

/** Week key "2026-06-09" -> "9 Jun". */
fun weekKeyShort(key: String): String {
    val p = key.split("-")
    val mon = p.getOrNull(1)?.toIntOrNull()?.let { MONTHS.getOrElse(it - 1) { "?" } } ?: return key
    val day = p.getOrNull(2)?.toIntOrNull() ?: return key
    return "$day $mon"
}

/** Local 24h time only, e.g. "19:45". */
fun Instant.hhmm(): String {
    val dt = local()
    return "${two(dt.hour)}:${two(dt.minute)}"
}

fun Instant.isToday(now: Instant): Boolean = local().date == now.local().date

/** Local weekday short name, e.g. "WED". */
fun Instant.weekdayShort(): String = DAYS.getOrElse(local().dayOfWeek.isoDayNumber - 1) { "" }.uppercase()

/**
 * Live countdown to start: "6d 2h" / "2h 5m" / "39m 12s" / "45s" / "LIVE".
 * Two units max so it never wraps — days past 24h, seconds only under an hour.
 */
fun startsInLabel(next: Instant, now: Instant): String {
    val s = (next - now).inWholeSeconds
    return when {
        s <= 0 -> "LIVE"
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        s < 86_400 -> "${s / 3600}h ${(s % 3600) / 60}m"
        else -> "${s / 86_400}d ${(s % 86_400) / 3600}h"
    }
}

/** Short relative label vs [now]: "live", "in 12m", "in 3h", else the absolute date. */
fun Instant.relativeTo(now: Instant): String {
    val secs = (this - now).inWholeSeconds
    return when {
        secs < -1800 -> formatStart()
        secs < 300 -> "live"
        secs < 3600 -> "in ${secs / 60}m"
        secs < 86_400 -> "in ${secs / 3600}h"
        else -> formatStart()
    }
}

/** "1".."7" (Mon..Sun) -> "Mon".."Sun". */
fun dayName(day: Int): String = DAYS.getOrElse(day - 1) { day.toString() }

/** "11:00:00" UTC time-of-day -> local "HH:mm" (24h), anchored to today's date. */
fun utcTimeToLocal(time: String): String {
    val p = time.split(":")
    val h = p.getOrNull(0)?.toIntOrNull() ?: return time
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    val instant = today.atTime(LocalTime(h, m)).toInstant(TimeZone.UTC)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${two(local.hour)}:${two(local.minute)}"
}
