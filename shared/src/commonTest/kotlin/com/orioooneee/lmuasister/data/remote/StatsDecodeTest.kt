package com.orioooneee.lmuasister.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

/** Guards the camelCase career-stat keys. The profile decodes with [ProfileJson] (no naming
 *  strategy), so these match by Kotlin property name; under the old global SnakeCase strategy
 *  they'd have looked for `pole_positions` and silently zeroed out. */
class StatsDecodeTest {

    @Test
    fun decodesCamelCaseCareerStats() {
        // Shape taken verbatim from a live GET /api/v2/profile response.
        val json = """
            {
              "uid": "abc",
              "rating_history": {"dr": [], "sr": []},
              "stats": {
                "total": {
                  "races": 169, "wins": 15, "polePositions": 13, "podiums": 46,
                  "top5": 63, "lapsCompleted": 2185, "lapsLead": 217, "dnfs": 84,
                  "fastestLaps": 3
                }
              }
            }
        """.trimIndent()

        val total = ProfileJson.decodeFromString<SteamProfile>(json).stats?.total
        assertEquals(169, total?.races)
        assertEquals(15, total?.wins)
        assertEquals(13, total?.polePositions)   // camelCase — the regression-prone one
        assertEquals(46, total?.podiums)
        assertEquals(63, total?.top5)
        assertEquals(2185, total?.lapsCompleted)  // camelCase
        assertEquals(217, total?.lapsLead)        // camelCase
        assertEquals(84, total?.dnfs)
        assertEquals(3, total?.fastestLaps)       // camelCase
    }
}
