package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// JSON is snake_case; AppJson uses JsonNamingStrategy.SnakeCase, so property
// names here are plain camelCase (carClasses ↔ car_classes, etc.).

/** GET /schedule — every week + championships, fully merged. */
@Serializable
data class ScheduleResponse(
    val weeks: List<WeekDto> = emptyList(),
    val schedules: Map<String, List<RaceDto>> = emptyMap(),
)

@Serializable
data class WeekDto(
    val key: String = "",
    val label: String = "",
)

/** GET /race/<id> — the race + its fastest-lap leaderboards (fast; no hot-laps). */
@Serializable
data class RaceDetailResponse(
    val race: RaceDto? = null,
    // Legacy flat top-5 (the overall board) — kept for backward compatibility.
    val leaderboard: List<LeaderboardEntryDto> = emptyList(),
    // New: overall + per-class boards, each with its own leaderboard_id.
    val leaderboards: LeaderboardsDto? = null,
)

@Serializable
data class LeaderboardsDto(
    val overall: ClassLeaderboardDto? = null,
    val byClass: List<ClassLeaderboardDto> = emptyList(),
)

/** One class's board: top-N entries plus the id to open its full leaderboard. */
@Serializable
data class ClassLeaderboardDto(
    val classId: String? = null,
    @SerialName("class") val carClass: String? = null,
    val leaderboardId: String? = null,
    val entries: List<LeaderboardEntryDto> = emptyList(),
)

/** GET /race/<id>/hotlaps — async: "ready" with results, or "pending" (still building). */
@Serializable
data class HotlapsResponse(
    val status: String = "ready",
    val hotlaps: List<HotlapDto> = emptyList(),
)

@Serializable
data class HotlapDto(
    val title: String = "",
    val videoId: String = "",
    val url: String = "",
    val thumbnail: String = "",
    val author: String? = null,
    val driver: String? = null,
    val lapTime: String? = null,
    val car: String? = null,
    val carClass: String? = null,
    val classBadge: String? = null,
    val gameVersion: String? = null,
    val views: Long = 0,
)

@Serializable
data class RaceDto(
    val id: String = "",
    val type: String = "",
    val series: String = "",
    val circuit: String = "",
    val difficulty: String = "",
    val carClasses: List<String> = emptyList(),
    val classInfos: List<ClassInfoDto> = emptyList(),
    val carsByClass: List<CarsByClassDto> = emptyList(),
    val times: List<String> = emptyList(),
    val raceLength: Int = 0,
    val settings: SettingsDto = SettingsDto(),
    val track: TrackDto? = null,
    val imageUrl: String? = null,
    val weather: WeatherDto? = null,
    val leaderboardId: String? = null,
    val completed: Boolean = false,
)

@Serializable
data class ClassInfoDto(
    val id: String = "",
    val name: String = "",
    val colorHex: String? = null,
)

@Serializable
data class CarsByClassDto(
    // JSON key is the Kotlin keyword "class"; @SerialName overrides the snake-case strategy.
    @SerialName("class") val carClass: String = "",
    val cars: List<String> = emptyList(),
)

@Serializable
data class SettingsDto(
    val setup: String? = null,
    val assists: String? = null,
    val damage: String? = null,
    // Backend sends these as either a multiplier int (1) or a label ("Realistic"),
    // so they must be strings — a strict Int? here breaks the whole schedule decode.
    val tireWear: String? = null,
    val fuelUsage: String? = null,
    val safetyRank: String? = null,
    val driverRank: String? = null,
    val splitSize: Int? = null,
    val qualifyingLength: Int? = null,
    val practiceLength: Int? = null,
    val driverSwaps: Boolean? = null,
    val trackLimits: String? = null,
    val tireWarmers: String? = null,
    val limitedTires: String? = null,
)

@Serializable
data class TrackDto(
    val name: String = "",
    val shortName: String = "",
    val simpleName: String? = null,
    val town: String? = null,
    val country: String? = null,
    // v2 sends length as a string ("13.626"); parsed to Double in the mapper.
    val lengthKm: String? = null,
    val numTurns: Int? = null,
    val mapUrl: String? = null,
    // v2 asset URLs — sent by the backend once race.track is enriched; until then
    // they're derived client-side from map_url (same /track/<id>/ path).
    val logoUrl: String? = null,
    val cardUrl: String? = null,
    val backgroundUrl: String? = null,
    val countryCode: String? = null,
)

/** GET /cars — the v2 roster (manufacturer / class / model / series). */
@Serializable
data class CarsResponse(
    val count: Int = 0,
    val classes: List<CarClassCountDto> = emptyList(),
    val cars: List<CarDto> = emptyList(),
)

@Serializable
data class CarClassCountDto(
    val name: String = "",
    val count: Int = 0,
)

@Serializable
data class CarDto(
    val id: String = "",
    val name: String = "",
    val manufacturer: String? = null,
    val model: String = "",
    val carClass: String = "",
    val series: String? = null,
    val engine: String? = null,
    val dlcAppId: Long? = null,
    val owned: Boolean = false,
)

@Serializable
data class WeatherDto(
    val practice: SessionWeatherDto? = null,
    val qualifying: SessionWeatherDto? = null,
    val race: SessionWeatherDto? = null,
)

@Serializable
data class SessionWeatherDto(
    val timeOfDay: String? = null,
    val segments: List<WeatherSegmentDto> = emptyList(),
)

@Serializable
data class WeatherSegmentDto(
    val sky: Int = 0,
    val tempC: Int? = null,
    val humidity: Int? = null,
    val windKmh: Int? = null,
    val rainChance: Int? = null,
    val durationMin: Int? = null,
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int = 0,
    val initials: String = "—",
    val bestLapMs: Long = 0,
    val sectors: List<Double> = emptyList(),
    val car: String? = null,
    val carClass: String? = null,
    val drRank: String? = null,
    val srRank: String? = null,
)

/** GET /leaderboard/<id>?limit=&cursor= — one paginated page of the full board. */
@Serializable
data class LeaderboardPageResponse(
    val leaderboardId: String = "",
    val total: Int? = null,
    val nextCursor: String? = null,
    val entries: List<LeaderboardEntryDto> = emptyList(),
    // The caller's own row + rank — present only when a valid app token was sent.
    val me: LeaderboardEntryDto? = null,
)
