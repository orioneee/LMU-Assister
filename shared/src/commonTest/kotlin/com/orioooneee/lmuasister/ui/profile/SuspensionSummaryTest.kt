package com.orioooneee.lmuasister.ui.profile

import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.data.remote.SuspensionDto
import kotlin.test.Test
import kotlin.test.assertEquals

class SuspensionSummaryTest {

    @Test
    fun usesEnforcementCountersWhenDetailedRowsAreMissing() {
        val summary = suspensionSummary(
            SteamProfile(
                uid = "abc",
                activeSuspensions = 1,
                totalSuspensions = 3,
            ),
        )

        assertEquals(false, summary.hasList)
        assertEquals(1, summary.activeCount)
        assertEquals(2, summary.pastCount)
        assertEquals(false, summary.banned)
    }

    @Test
    fun detailedRowsStillDrivePermanentBanState() {
        val summary = suspensionSummary(
            SteamProfile(
                uid = "abc",
                suspensions = listOf(
                    SuspensionDto(active = true, permanent = true),
                    SuspensionDto(active = false),
                ),
            ),
        )

        assertEquals(true, summary.hasList)
        assertEquals(1, summary.activeCount)
        assertEquals(1, summary.pastCount)
        assertEquals(true, summary.banned)
    }
}
