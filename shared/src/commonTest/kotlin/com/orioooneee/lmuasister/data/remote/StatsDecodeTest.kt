package com.orioooneee.lmuasister.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

/** Guards the camelCase career-stat keys. The profile decodes with [ProfileJson] (no naming
 *  strategy), so these match by Kotlin property name; under the old global SnakeCase strategy
 *  they'd have looked for `pole_positions` and silently zeroed out. */
class StatsDecodeTest {

    @Test
    fun decodesCamelCaseCareerStats() {
        // Shape taken verbatim from a live GET /api/v3/profile response.
        val json = """
            {
              "uid": "abc",
              "next_profile_update_at": "2026-07-09T16:45:00Z",
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

        val profile = ProfileJson.decodeFromString<SteamProfile>(json)
        val total = profile.stats?.total
        assertEquals("2026-07-09T16:45:00Z", profile.nextProfileUpdateAt)
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

    @Test
    fun decodesRaceOsRankAdjustmentsAcrossRaceDetailRows() {
        val json = """
            {
              "title": "Test race",
              "driver_rank_adjustment": 2,
              "safety_rank_adjustment": -1,
              "joker_used": true,
              "sessions": {
                "race": {
                  "classification": [
                    {
                      "driver_rank_adjustment": 3,
                      "safety_rank_adjustment": -2,
                      "joker_used": false,
                      "team_members": [
                        {
                          "driver_rank_adjustment": 1,
                          "safety_rank_adjustment": -4,
                          "joker_used": true
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val detail = ProfileJson.decodeFromString<RaceDetailDto>(json)
        val row = detail.sessions.getValue("race")!!.classification.single()
        val member = row.teamMembers.single()
        assertEquals(2, detail.driverRankAdjustment)
        assertEquals(-1, detail.safetyRankAdjustment)
        assertEquals(true, detail.jokerUsed)
        assertEquals(3, row.driverRankAdjustment)
        assertEquals(-2, row.safetyRankAdjustment)
        assertEquals(false, row.jokerUsed)
        assertEquals(1, member.driverRankAdjustment)
        assertEquals(-4, member.safetyRankAdjustment)
        assertEquals(true, member.jokerUsed)
    }

    @Test
    fun decodesSuspensionDetailAvailabilityAndTrackDisplayName() {
        val profile = ProfileJson.decodeFromString<SteamProfile>(
            """{"uid":"abc","suspensions_detail_available":false}""",
        )
        val track = ProfileJson.decodeFromString<TrackDto>(
            """{"name":"Official","display_name":"Daytona","official_name":"Daytona International Speedway"}""",
        )

        assertEquals(false, profile.suspensionsDetailAvailable)
        assertEquals("Daytona", track.displayName)
        assertEquals("Daytona International Speedway", track.officialName)
    }
}
