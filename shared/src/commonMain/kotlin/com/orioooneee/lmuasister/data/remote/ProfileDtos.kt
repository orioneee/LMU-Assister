package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Decoded with [ProfileJson] (no naming strategy): snake_case wire keys are stated explicitly
// via @SerialName; the camelCase `stats.total` counters match the Kotlin name verbatim.
// Non-null scalars are required (absence/null is a real decode error we want to see); only
// genuinely-optional data is nullable, and boolean flags keep `false` as "absent == off".

/** Player profile from GET /api/v2/profile. */
@Serializable
data class SteamProfile(
    val uid: String,
    // Real LMU in-game name (backend may send any of these) — shown instead of the Steam nick.
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val username: String? = null,
    val nationality: String? = null,
    val badge: String? = null,
    val badges: List<String> = emptyList(),
    val email: String? = null,
    @SerialName("driver_rating") val driverRating: RatingDto? = null,
    @SerialName("safety_rating") val safetyRating: RatingDto? = null,
    // Kept defaulted: a live /profile response omits these when there's nothing to report.
    @SerialName("active_suspensions") val activeSuspensions: Int = 0,
    @SerialName("total_suspensions") val totalSuspensions: Int = 0,
    // Full ban/warning history (newest first); each carries its reason + window + active flag.
    val suspensions: List<SuspensionDto> = emptyList(),
    @SerialName("recent_races") val recentRaces: List<RecentRaceDto> = emptyList(),
    // Career stats (profile_get_stats) — totals + per-class/manufacturer breakdown.
    val stats: ProfileStatsDto? = null,
    // DR/SR progression: one point per race, oldest→newest, for the profile chart.
    @SerialName("rating_history") val ratingHistory: RatingHistoryDto? = null,
    // Total distance driven across all races, in km (float; shown rounded as a header badge).
    @SerialName("total_distance_km") val totalDistanceKm: Double = 0.0,
    // Per-track distance breakdown (km desc) — shown when the distance badge is tapped.
    @SerialName("track_breakdown") val trackBreakdown: List<TrackBreakdownDto> = emptyList(),
)

/** One track's slice of the career distance breakdown (GET /profile → `track_breakdown`). */
@Serializable
data class TrackBreakdownDto(
    @SerialName("track_id") val trackId: String? = null,   // per-layout asset id → opens track detail
    val track: String = "—",
    @SerialName("length_km") val lengthKm: Double? = null,
    @SerialName("num_turns") val numTurns: Int? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val races: Int = 0,
    val laps: Int = 0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
)

/** GET /api/v2/profile → `stats`. Only the headline `total` block is used by the UI;
 *  the per-class / per-manufacturer breakdowns are kept for future drill-downs. */
@Serializable
data class ProfileStatsDto(
    val total: StatTotalsDto? = null,
)

/** Headline career counters. The backend sends these keys in camelCase — under [ProfileJson]
 *  (no naming strategy) the Kotlin property names match the wire keys verbatim, no override. */
@Serializable
data class StatTotalsDto(
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val top5: Int,
    val dnfs: Int,
    val polePositions: Int,
    val lapsCompleted: Int,
    val lapsLead: Int,
    val fastestLaps: Int,
    val grandSlams: Int = 0,     // ours (pole + win + fastest lap + led every lap); default for older payloads
    val polesConverted: Int = 0, // started on pole AND won
    val winsNoPole: Int = 0,     // won without starting on pole
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
    val rank: String,
    val tier: Int,
    val progress: Double? = null,
    val elo: Double? = null,
    val rating: Double? = null,
)

/** One component of an SR/DR change for a race: a signed weight + the human reason it moved.
 *  e.g. impact +2.0 "Clean racing" (positive) or -3.2 "Contact with another car" (negative). */
@Serializable
data class ReasonDto(
    val impact: Double? = null,  // signed weight of this component
    val positive: Boolean = false,
    val reason: String? = null,  // human-readable explanation
)

/** One lap from the player's race: time + sectors + whether it was a pit lap. Drives the
 *  average-pace stat (valid laps = not the opening lap and not pit laps). */
@Serializable
data class LapDto(
    val lap: Int? = null,        // 1-based lap number
    val position: Int? = null,   // overall running order this lap
    @SerialName("class_position") val classPosition: Int? = null, // position in own class this lap
    @SerialName("lap_time_ms") val lapTimeMs: Long? = null,
    @SerialName("sectors_ms") val sectorsMs: List<Long?> = emptyList(),
    val pit: Boolean = false,    // in/out lap around a pit stop — excluded from average pace
)

/** Per-session line (practice / qualifying / race) for a recent race. */
@Serializable
data class SessionSummaryDto(
    val position: Int? = null,
    @SerialName("class_position") val classPosition: Int? = null,
    @SerialName("grid_position") val gridPosition: Int? = null,
    @SerialName("best_lap_ms") val bestLapMs: Long? = null,
    @SerialName("best_lap_sectors_ms") val bestLapSectorsMs: List<Long?> = emptyList(),
    @SerialName("best_sectors_ms") val bestSectorsMs: List<Long?> = emptyList(),
    @SerialName("finish_time_ms") val finishTimeMs: Long? = null,
    @SerialName("finish_status") val finishStatus: String? = null,
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
    val title: String,
    @SerialName("event_id") val eventId: String? = null,     // id for the race-detail endpoint
    @SerialName("series_id") val seriesId: String? = null,
    val track: String? = null,
    @SerialName("track_logo") val trackLogo: String? = null, // backend asset path or absolute url
    val tier: String? = null,        // difficulty: beginner / intermediate / …
    @SerialName("event_type") val eventType: String? = null, // daily / weekly / special …
    val official: Boolean = false,
    val split: Int? = null,          // which split the driver raced in
    // total splits for the event — NOT sent by /profile or /profile/races (list endpoints);
    // only the race-detail endpoint fills it, so it stays nullable here.
    @SerialName("total_splits") val totalSplits: Int? = null,
    @SerialName("field_size") val fieldSize: Int,            // overall entries (all classes)
    @SerialName("class_field_size") val classFieldSize: Int? = null, // entries in own class
    // Kept defaulted: a no-result race sends position: null, which coerces to 0 (see [ProfileJson]).
    val position: Int = 0,                                   // overall finishing position
    @SerialName("class_position") val classPosition: Int? = null, // position within own class
    @SerialName("grid_position") val gridPosition: Int? = null,
    @SerialName("car_class") val carClass: String? = null,
    // Car model name, e.g. "McLaren 720S GT3" (backend may send either key).
    val car: String? = null,
    @SerialName("car_name") val carName: String? = null,
    @SerialName("best_lap_ms") val bestLapMs: Long? = null,
    @SerialName("finish_status") val finishStatus: String? = null,
    @SerialName("sr_change") val srChange: Double? = null,
    @SerialName("dr_change") val drChange: Double? = null,
    val sessions: RaceSessionsDto? = null,
)

/**
 * GET /api/v2/profile/races?page=N — one paginated page (5/page) of the full race history,
 * and GET /api/v2/profile/races/<category>?page=N — a category drill-down (30/page).
 * `category` / `total` are only sent by the category endpoint, so both stay optional.
 */
@Serializable
data class RacesPageDto(
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val count: Int,
    val total: Int? = null,            // category endpoint only: full count across all pages
    val category: String? = null,      // category endpoint only: echoes the requested category
    @SerialName("has_more") val hasMore: Boolean = false,
    val races: List<RecentRaceDto> = emptyList(),
)

/** GET /api/v2/profile/race/<eventId> — full race page: card meta + per-session tables. */
@Serializable
data class RaceDetailDto(
    val date: String? = null,
    val title: String,
    @SerialName("event_id") val eventId: String? = null,
    val car: String? = null,
    @SerialName("car_name") val carName: String? = null,
    @SerialName("car_class") val carClass: String? = null,
    val track: String? = null,
    @SerialName("track_logo") val trackLogo: String? = null,
    @SerialName("track_info") val trackInfo: TrackDto? = null, // full track block (emblem, minimap, length…)
    val tier: String? = null,
    @SerialName("event_type") val eventType: String? = null,
    val official: Boolean = false,
    @SerialName("series_id") val seriesId: String? = null,     // needed to request other splits
    val split: Int? = null,          // YOUR split
    @SerialName("total_splits") val totalSplits: Int? = null,
    @SerialName("splits_available") val splitsAvailable: List<Int> = emptyList(),  // render as tabs
    @SerialName("field_size") val fieldSize: Int,
    val position: Int? = null,
    @SerialName("grid_position") val gridPosition: Int? = null,
    @SerialName("best_lap_ms") val bestLapMs: Long? = null,
    @SerialName("best_lap_sectors_ms") val bestLapSectorsMs: List<Long?> = emptyList(),
    @SerialName("best_sectors_ms") val bestSectorsMs: List<Long?> = emptyList(),
    @SerialName("finish_status") val finishStatus: String? = null,
    @SerialName("sr_change") val srChange: Double? = null,
    @SerialName("dr_change") val drChange: Double? = null,
    // Why SR/DR moved this race — one entry per component (clean racing, contact, off-track…).
    @SerialName("sr_reasons") val srReasons: List<ReasonDto> = emptyList(),
    @SerialName("dr_reasons") val drReasons: List<ReasonDto> = emptyList(),
    // The player's per-lap times (for the average-pace / delta-to-best stat).
    @SerialName("lap_progress") val lapProgress: List<LapDto> = emptyList(),
    @SerialName("hero_image") val heroImage: String? = null,
    // Highlight categories this race earned (keys from RACE_CATEGORIES: wins / poles / grand_slam…),
    // shown as colored chips. Ordered by the backend.
    val categories: List<String> = emptyList(),
    // keyed "practice" / "qualifying" / "race" — YOUR split, fully here
    val sessions: Map<String, RaceSessionDetailDto> = emptyMap(),
)

/** GET /api/v2/profile/race/<eventId>/split/<n> — one foreign split's tables (no `me` row). */
@Serializable
data class SplitDetailDto(
    @SerialName("split_no") val splitNo: Int,
    @SerialName("field_size") val fieldSize: Int,
    // keyed "practice" / "qualifying" / "race"; rows are all is_me=false.
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
    @SerialName("class_position") val classPosition: Int? = null,
    @SerialName("grid_position") val gridPosition: Int? = null,
    val name: String? = null,
    val nationality: String? = null,
    @SerialName("is_me") val isMe: Boolean = false,
    val car: String? = null,
    @SerialName("car_number") val carNumber: String? = null,
    @SerialName("car_class") val carClass: String? = null,
    @SerialName("best_lap_ms") val bestLapMs: Long? = null,
    // Sector splits of this driver's best lap (S1/S2/S3, ms) — shown under the driver row.
    @SerialName("best_lap_sectors_ms") val bestLapSectorsMs: List<Long?> = emptyList(),
    // Theoretical optimum: the fastest each sector across all laps (≤ best lap → potential).
    @SerialName("best_sectors_ms") val bestSectorsMs: List<Long?> = emptyList(),
    @SerialName("finish_time_ms") val finishTimeMs: Long? = null,
    @SerialName("finish_status") val finishStatus: String? = null,
    // Class-relative gaps from the backend: gap_* = to the class leader, interval_* = to the car
    // ahead in class. _ms is a time delta (race: same-lap finish gap / quali: best-lap delta);
    // _laps is "N laps down" (race only). Leader/no-time rows carry null/0.
    @SerialName("gap_ms") val gapMs: Long? = null,
    @SerialName("gap_laps") val gapLaps: Int = 0,
    @SerialName("interval_ms") val intervalMs: Long? = null,
    @SerialName("interval_laps") val intervalLaps: Int = 0,
    @SerialName("sr_change") val srChange: Double? = null,
    @SerialName("dr_change") val drChange: Double? = null,
    // This driver's per-lap progress (carries class_position per lap, unlike the top-level copy).
    @SerialName("lap_progress") val lapProgress: List<LapDto> = emptyList(),
    // Per-driver absolute ratings (driver_rating / safety_rating), same shape as the profile owner's.
    @SerialName("driver_rating") val driverRating: RatingDto? = null,
    @SerialName("safety_rating") val safetyRating: RatingDto? = null,
)
