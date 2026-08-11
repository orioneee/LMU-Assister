package com.orioooneee.lmuasister.data

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private const val SCHEDULE_CACHE_TTL_MS = 15L * 60 * 1000
private val ScheduleTimeZone = TimeZone.of("Europe/Kyiv")
private val ScheduleRotationTime = LocalTime(13, 0)

/** Latest Tuesday 13:00 Kyiv schedule rotation boundary at [now]. */
internal fun scheduleRotationBoundaryMs(now: Instant): Long {
    val local = now.toLocalDateTime(ScheduleTimeZone)
    val daysSinceTuesday = (local.dayOfWeek.isoDayNumber - 2 + 7) % 7
    var rotationDate = LocalDate.fromEpochDays(local.date.toEpochDays() - daysSinceTuesday)
    var boundary = rotationDate.atTime(ScheduleRotationTime).toInstant(ScheduleTimeZone)
    if (now < boundary) {
        rotationDate = LocalDate.fromEpochDays(rotationDate.toEpochDays() - 7)
        boundary = rotationDate.atTime(ScheduleRotationTime).toInstant(ScheduleTimeZone)
    }
    return boundary.toEpochMilliseconds()
}

internal fun isScheduleCacheFromCurrentRotation(cachedAtMs: Long, now: Instant): Boolean =
    cachedAtMs > 0 && cachedAtMs >= scheduleRotationBoundaryMs(now)

internal fun isScheduleCacheFresh(cachedAtMs: Long, now: Instant): Boolean {
    val ageMs = now.toEpochMilliseconds() - cachedAtMs
    return ageMs in 0..SCHEDULE_CACHE_TTL_MS && isScheduleCacheFromCurrentRotation(cachedAtMs, now)
}
