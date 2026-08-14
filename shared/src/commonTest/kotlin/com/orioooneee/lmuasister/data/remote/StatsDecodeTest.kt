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

    @Test
    fun decodesFrontendReadyTyreAndFuelStrategy() {
        val detail = ProfileJson.decodeFromString<RaceDetailDto>(
            """
            {
              "title": "Strategy test",
              "features": {
                "session_strategy": true,
                "physical_fuel": true,
                "tyre_compounds": true
              },
              "sessions": {
                "race": {
                  "strategy": {
                    "lap_count": 3,
                    "tyres": {
                      "lap_count": 3,
                      "stint_count": 2,
                      "stints": [
                        {
                          "number": 1,
                          "start_lap": 1,
                          "end_lap": 2,
                          "lap_count": 2,
                          "pit_after": true,
                          "label": "MEDIUM",
                          "front_compound": "medium",
                          "rear_compound": "medium",
                          "front_color": "#F2C94C",
                          "rear_color": "#F2C94C"
                        },
                        {
                          "number": 2,
                          "start_lap": 3,
                          "end_lap": 3,
                          "lap_count": 1,
                          "pit_after": false,
                          "label": "HARD",
                          "front_compound": "hard",
                          "rear_compound": "hard",
                          "front_color": "#E33B32",
                          "rear_color": "#E33B32"
                        }
                      ],
                      "compounds": [
                        {"key":"medium","label":"MEDIUM","short_label":"M","color":"#F2C94C"}
                      ]
                    },
                    "fuel": {
                      "source": "physical_fuel",
                      "label": "FUEL",
                      "unit": "percent",
                      "accent_color": "#F1667E",
                      "estimated": true,
                      "used_pct": 30.0,
                      "remaining_pct": 70.0,
                      "average_per_lap_pct": 10.0,
                      "lap_count": 3,
                      "refuel_count": 0
                    }
                  },
                  "classification": [{
                    "is_me": true,
                    "badge": "sr-clean",
                    "badge_url": "https://example.com/sr-clean.svg",
                    "lap_progress": [{
                      "lap": 1,
                      "front_compound": "Medium",
                      "rear_compound": "Medium",
                      "front_compound_key": "medium",
                      "rear_compound_key": "medium",
                      "fuel_remaining": 0.9,
                      "fuel_remaining_pct": 90.0,
                      "tyre_wear_remaining": {
                        "front_left": 0.98,
                        "front_right": 0.97,
                        "rear_left": 0.96,
                        "rear_right": 0.95
                      }
                    }]
                  }]
                }
              }
            }
            """.trimIndent(),
        )

        val race = detail.sessions.getValue("race")!!
        val strategy = race.strategy!!
        val firstStint = strategy.tyres!!.stints.first()
        val lap = race.classification.single().lapProgress.single()
        assertEquals(true, detail.features?.sessionStrategy)
        assertEquals(2, strategy.tyres.stintCount)
        assertEquals("MEDIUM", firstStint.label)
        assertEquals(true, firstStint.pitAfter)
        assertEquals(30.0, strategy.fuel?.usedPct)
        assertEquals("#F1667E", strategy.fuel?.accentColor)
        assertEquals("medium", lap.frontCompoundKey)
        assertEquals(90.0, lap.fuelRemainingPct)
        assertEquals(0.95, lap.tyreWearRemaining?.rearRight)
        assertEquals("sr-clean", race.classification.single().badge)
    }
}
