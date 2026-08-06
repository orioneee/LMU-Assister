package com.orioooneee.lmuasister.ui.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NotificationTimingTest {
    private val now = Instant.parse("2026-08-05T12:00:00Z")

    @Test
    fun minimumLeadTimeIsOneMinute() {
        assertEquals(1, MIN_NOTIFICATION_LEAD_MINUTES)
        assertNotNull(notificationTimingError(now + 59.seconds, now))
        assertNull(notificationTimingError(now + 60.seconds, now))
    }
}
