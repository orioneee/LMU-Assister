package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.Serializable

/** Player profile from GET /api/v2/profile (snake_case → camelCase via [AppJson]). */
@Serializable
data class SteamProfile(
    val uid: String = "",
    // Real LMU in-game name (backend may send any of these) — shown instead of the Steam nick.
    val name: String? = null,
    val displayName: String? = null,
    val username: String? = null,
    val nationality: String? = null,
    val badge: String? = null,
    val badges: List<String> = emptyList(),
    val email: String? = null,
    val driverRating: RatingDto? = null,
    val safetyRating: RatingDto? = null,
    val activeSuspensions: Int = 0,
    val recentRaces: List<RecentRaceDto> = emptyList(),
)

@Serializable
data class RatingDto(
    val rank: String = "",
    val tier: Int = 0,
    val progress: Double? = null,
    val elo: Double? = null,
    val rating: Double? = null,
)

/** Per-session line (practice / qualifying / race) for a recent race. */
@Serializable
data class SessionSummaryDto(
    val position: Int? = null,
    val classPosition: Int? = null,
    val gridPosition: Int? = null,
    val bestLapMs: Long? = null,
    val finishTimeMs: Long? = null,
    val finishStatus: String? = null,
)

@Serializable
data class RaceSessionsDto(
    val practice: SessionSummaryDto? = null,
    val qualifying: SessionSummaryDto? = null,
    val race: SessionSummaryDto? = null,
)

@Serializable
data class RecentRaceDto(
    val date: String? = null,
    val title: String = "",
    val track: String? = null,
    val trackLogo: String? = null,   // backend asset path or absolute url
    val tier: String? = null,        // difficulty: beginner / intermediate / …
    val eventType: String? = null,   // daily / weekly / special …
    val official: Boolean = false,
    val split: Int? = null,          // which split the driver raced in
    val totalSplits: Int? = null,    // total splits for the event
    val fieldSize: Int = 0,
    val position: Int = 0,
    val gridPosition: Int? = null,
    val carClass: String? = null,
    // Car model name, e.g. "McLaren 720S GT3" (backend may send either key).
    val car: String? = null,
    val carName: String? = null,
    val bestLapMs: Long? = null,
    val finishStatus: String? = null,
    val srChange: Double? = null,
    val drChange: Double? = null,
    val sessions: RaceSessionsDto? = null,
)
