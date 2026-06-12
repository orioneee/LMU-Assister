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

/** Countdown label like "STARTS IN 39M" / "STARTS IN 2H 5M" / "LIVE". */
fun startsInLabel(next: Instant, now: Instant): String {
    val m = (next - now).inWholeMinutes
    return when {
        m < 1 -> "LIVE"
        m < 60 -> "STARTS IN ${m}M"
        else -> "STARTS IN ${m / 60}H ${m % 60}M"
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
