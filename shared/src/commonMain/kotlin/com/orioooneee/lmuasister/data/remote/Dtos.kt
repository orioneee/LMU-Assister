package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.Serializable

// ── LmuAssister backend: GET {BACKEND_URL}/{schedule,race/<id>} ────────────────
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

/** GET /race/<id> — the race + its fastest-lap leaderboard (fast; no hot-laps). */
@Serializable
data class RaceDetailResponse(
    val race: RaceDto? = null,
    val leaderboard: List<LeaderboardEntryDto> = emptyList(),
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
data class SettingsDto(
    val setup: String? = null,
    val assists: String? = null,
    val damage: String? = null,
    val tireWear: Int? = null,
    val fuelUsage: Int? = null,
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
    val town: String? = null,
    val country: String? = null,
    val lengthKm: Double? = null,
    val numTurns: Int? = null,
    val mapUrl: String? = null,
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
