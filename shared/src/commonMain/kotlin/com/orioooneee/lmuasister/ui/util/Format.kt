package com.orioooneee.lmuasister.ui.util

import androidx.compose.ui.graphics.Color
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
    val mon = MONTHS.getOrElse(dt.monthNumber - 1) { "?" }
    return "${dt.dayOfMonth} $mon ${two(dt.hour)}:${two(dt.minute)}"
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

/** True if this instant is on the system-local current day. */
fun Instant.isToday(now: Instant): Boolean = local().date == now.local().date

/** Local weekday short name, e.g. "WED". */
fun Instant.weekdayShort(): String = DAYS.getOrElse(local().dayOfWeek.isoDayNumber - 1) { "" }.uppercase()

/** Live countdown to start: "2h 5m" / "39m 12s" / "45s" / "LIVE". Seconds shown under an hour. */
fun startsInLabel(next: Instant, now: Instant): String {
    val s = (next - now).inWholeSeconds
    return when {
        s <= 0 -> "LIVE"
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
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
