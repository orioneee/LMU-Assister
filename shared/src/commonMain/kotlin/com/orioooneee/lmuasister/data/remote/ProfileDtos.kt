package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    val totalSuspensions: Int = 0,
    // Full ban/warning history (newest first); each carries its reason + window + active flag.
    val suspensions: List<SuspensionDto> = emptyList(),
    val recentRaces: List<RecentRaceDto> = emptyList(),
    // Career stats (profile_get_stats) — totals + per-class/manufacturer breakdown.
    val stats: ProfileStatsDto? = null,
    // DR/SR progression: one point per race, oldest→newest, for the profile chart.
    val ratingHistory: RatingHistoryDto? = null,
)

/** GET /api/v2/profile → `stats`. Only the headline `total` block is used by the UI;
 *  the per-class / per-manufacturer breakdowns are kept for future drill-downs. */
@Serializable
data class ProfileStatsDto(
    val total: StatTotalsDto? = null,
)

/** Headline career counters. The backend sends these keys in camelCase, but [AppJson] runs
 *  a global SnakeCase naming strategy (which also rewrites @SerialName values) — so the real
 *  camelCase keys are matched via @JsonNames alternatives, which the strategy leaves intact. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StatTotalsDto(
    // Single-word keys round-trip through SnakeCase unchanged, so they need no override.
    val races: Int = 0,
    val wins: Int = 0,
    val podiums: Int = 0,
    val top5: Int = 0,
    val dnfs: Int = 0,
    // Multi-word keys: SnakeCase would look for `pole_positions` etc.; match the real
    // camelCase key via @JsonNames (an alternative the strategy doesn't rewrite).
    @JsonNames("polePositions") val polePositions: Int = 0,
    @JsonNames("lapsCompleted") val lapsCompleted: Int = 0,
    @JsonNames("lapsLead") val lapsLead: Int = 0,
    @JsonNames("fastestLaps") val fastestLaps: Int = 0,
)

/** GET /api/v2/profile → `rating_history`: two series of points (driver / safety). */
@Serializable
data class RatingHistoryDto(
    val dr: List<RatingPointDto> = emptyList(),
    val sr: List<RatingPointDto> = emptyList(),
)

/** One chart point: the rank snapshot after a race plus the per-race delta. `score` is a
 *  continuous value (Bronze-1=0 … Platinum-3=1100) so the line plots across rank borders. */
@Serializable
data class RatingPointDto(
    val date: String? = null,
    val rank: String? = null,
    val tier: Int? = null,
    val progress: Double? = null,
    val score: Double? = null,
    val change: Double? = null,
)

/** One licence sanction (ban / suspension / warning) from the player's penalty history. */
@Serializable
data class SuspensionDto(
    val type: Int? = null,           // upstream sanction kind (numeric); semantics not documented
    val reason: String? = null,      // human-readable text (e.g. the SR-ratio auto-ban note)
    val from: Long? = null,          // epoch-ms window start
    val to: Long? = null,            // epoch-ms window end
    val permanent: Boolean = false,  // permanent bans count as active regardless of window
    val redacted: Boolean = false,   // upstream hid the reason text
    val active: Boolean = false,     // in effect right now (computed server-side)
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
    val eventId: String? = null,     // id for the race-detail endpoint
    val seriesId: String? = null,
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

/** GET /api/v2/profile/races?page=N — one paginated page (5/page) of race cards. */
@Serializable
data class RacesPageDto(
    val page: Int = 1,
    val pageSize: Int = 5,
    val count: Int = 0,
    val hasMore: Boolean = false,
    val races: List<RecentRaceDto> = emptyList(),
)

/** GET /api/v2/profile/race/<eventId> — full race page: card meta + per-session tables. */
@Serializable
data class RaceDetailDto(
    val date: String? = null,
    val title: String = "",
    val eventId: String? = null,
    val car: String? = null,
    val carName: String? = null,
    val carClass: String? = null,
    val track: String? = null,
    val trackLogo: String? = null,
    val trackInfo: TrackDto? = null,   // full track block (emblem, minimap, length…), like the schedule
    val tier: String? = null,
    val eventType: String? = null,
    val official: Boolean = false,
    val split: Int? = null,
    val totalSplits: Int? = null,
    val fieldSize: Int = 0,
    val position: Int? = null,
    val gridPosition: Int? = null,
    val bestLapMs: Long? = null,
    val finishStatus: String? = null,
    val srChange: Double? = null,
    val drChange: Double? = null,
    val heroImage: String? = null,
    // keyed "practice" / "qualifying" / "race"
    val sessions: Map<String, RaceSessionDetailDto> = emptyMap(),
)

@Serializable
data class RaceSessionDetailDto(
    val me: SessionSummaryDto? = null,
    val classification: List<ClassificationRowDto> = emptyList(),
)

@Serializable
data class ClassificationRowDto(
    val position: Int? = null,
    val classPosition: Int? = null,
    val gridPosition: Int? = null,
    val name: String? = null,
    val nationality: String? = null,
    val isMe: Boolean = false,
    val car: String? = null,
    val carNumber: String? = null,
    val carClass: String? = null,
    val bestLapMs: Long? = null,
    val finishTimeMs: Long? = null,
    val finishStatus: String? = null,
    val srChange: Double? = null,
    val drChange: Double? = null,
    // Per-driver absolute ratings (driver_rating / safety_rating), same shape as the profile owner's.
    val driverRating: RatingDto? = null,
    val safetyRating: RatingDto? = null,
)
