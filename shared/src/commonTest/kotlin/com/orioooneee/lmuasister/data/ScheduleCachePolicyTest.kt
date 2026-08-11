package com.orioooneee.lmuasister.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ScheduleCachePolicyTest {

    @Test
    fun cacheFromPreviousRotationExpiresAtTuesdayKyivCutoff() {
        val beforeCutoff = Instant.parse("2026-08-11T09:59:00Z")
        val atCutoff = Instant.parse("2026-08-11T10:00:00Z")
        val cachedAt = Instant.parse("2026-08-11T09:55:00Z").toEpochMilliseconds()

        assertTrue(isScheduleCacheFresh(cachedAt, beforeCutoff))
        assertFalse(isScheduleCacheFresh(cachedAt, atCutoff))
    }

    @Test
    fun cacheExpiresWithinSameRotationAfterTtl() {
        val cachedAt = Instant.parse("2026-08-10T11:00:00Z").toEpochMilliseconds()

        assertTrue(isScheduleCacheFresh(cachedAt, Instant.parse("2026-08-10T11:14:59Z")))
        assertFalse(isScheduleCacheFresh(cachedAt, Instant.parse("2026-08-10T11:15:01Z")))
        assertTrue(isScheduleCacheFromCurrentRotation(cachedAt, Instant.parse("2026-08-10T11:15:01Z")))
    }

    @Test
    fun oldNextWeekCacheCannotBecomeNewCurrentWeek() {
        val cachedAt = Instant.parse("2026-08-03T18:00:00Z").toEpochMilliseconds()
        val now = Instant.parse("2026-08-10T11:00:00Z")

        assertFalse(isScheduleCacheFromCurrentRotation(cachedAt, now))
        assertFalse(isScheduleCacheFresh(cachedAt, now))
    }
}
