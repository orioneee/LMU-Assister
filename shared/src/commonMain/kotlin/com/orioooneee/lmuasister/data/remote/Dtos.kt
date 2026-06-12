package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── lmuportal Supabase: GET https://db.lmuportal.space/rest/v1/<table> ─────────
// Anon key reads these reference + schedule tables (RLS allows anon read).

/** race_series — lmuportal's own weekly schedule (daily / weekly / special). */
@Serializable
data class RaceSeriesDto(
    @SerialName("series_id") val seriesId: String = "",
    @SerialName("week_key") val weekKey: String = "",
    val type: String = "",
    @SerialName("tier_name") val tierName: String = "",
    @SerialName("series_name") val seriesName: String = "",
    @SerialName("leaderboard_id") val leaderboardId: String? = null,
    @SerialName("sr_requirement") val srRequirement: String? = null,
    @SerialName("track_friendly") val trackFriendly: String = "",
    @SerialName("track_config") val trackConfig: String? = null,
    @SerialName("track_slug") val trackSlug: String? = null,
    @SerialName("car_classes") val carClasses: List<String> = emptyList(),
    @SerialName("race_duration_minutes") val raceDuration: Int = 0,
    @SerialName("practice_duration_minutes") val practiceDuration: Int? = null,
    @SerialName("qualifying_duration_minutes") val qualifyingDuration: Int? = null,
    val setup: String? = null,
    @SerialName("split_size") val splitSize: Int? = null,
    @SerialName("fuel_multiplier") val fuelMultiplier: Double? = null,
    @SerialName("tyre_allowance") val tyreAllowance: Int? = null,
    @SerialName("tyre_warmers") val tyreWarmers: Boolean? = null,
    @SerialName("driver_swap_enabled") val driverSwap: Boolean? = null,
    @SerialName("todays_start_times_utc") val todaysStartTimes: List<String> = emptyList(),
    @SerialName("race_days") val raceDays: List<Int> = emptyList(),
    @SerialName("times_utc") val timesUtc: List<String> = emptyList(),
    @SerialName("race_weather") val raceWeather: WeatherDto? = null,
    @SerialName("qualifying_weather") val qualifyingWeather: WeatherDto? = null,
    @SerialName("practice_weather") val practiceWeather: WeatherDto? = null,
)

@Serializable
data class WeatherDto(
    val weather: List<WeatherSegmentDto> = emptyList(),
    @SerialName("time_of_day") val timeOfDay: String? = null,
    @SerialName("real_road") val realRoad: String? = null,
)

@Serializable
data class WeatherSegmentDto(
    val sky: String? = null,
    val humidity: String? = null,
    val temperature: String? = null,
    @SerialName("wind_speed") val windSpeed: String? = null,
    @SerialName("rain_change") val rainChange: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
)

/** championship_series — recurring championships (the 4th category). */
@Serializable
data class ChampionshipSeriesDto(
    @SerialName("championship_series_id") val id: String = "",
    @SerialName("season_id") val seasonId: String = "",
    @SerialName("series_name") val seriesName: String = "",
    @SerialName("car_classes") val carClasses: List<String> = emptyList(),
    @SerialName("race_duration_minutes") val raceDuration: Int = 0,
    @SerialName("practice_duration_minutes") val practiceDuration: Int? = null,
    @SerialName("qualifying_duration_minutes") val qualifyingDuration: Int? = null,
    val setup: String? = null,
    @SerialName("sr_requirement") val srRequirement: String? = null,
    @SerialName("split_size") val splitSize: Int? = null,
    @SerialName("drop_rounds") val dropRounds: Int? = null,
    @SerialName("driver_swap_enabled") val driverSwap: Boolean? = null,
    @SerialName("race_days") val raceDays: List<Int> = emptyList(),
    @SerialName("race_times_utc") val raceTimesUtc: List<String> = emptyList(),
    @SerialName("race_time_utc_next_day") val raceTimeNextDay: List<String> = emptyList(),
)

@Serializable
data class ChampionshipEventDto(
    @SerialName("championship_series_id") val seriesId: String = "",
    @SerialName("round_name") val roundName: String = "",
    @SerialName("week_number") val weekNumber: Int = 0,
    @SerialName("race_starts_at") val raceStartsAt: String = "",
)

@Serializable
data class ChampionshipSeasonDto(
    @SerialName("season_id") val seasonId: String = "",
    @SerialName("season_name") val seasonName: String = "",
    val status: String = "",
    @SerialName("end_date") val endDate: String? = null,
)

// ── leaderboard (race_series_leaderboard_entry) ───────────────────────────────
@Serializable
data class LeaderboardEntryDto(
    val rank: Int = 0,
    @SerialName("display_name_public") val displayName: String? = null,
    @SerialName("display_initials") val initials: String? = null,
    @SerialName("score_ms") val scoreMs: Long = 0,
    val metadata: LeaderboardMetaDto? = null,
)

@Serializable
data class LeaderboardMetaDto(
    val car: String? = null,
    @SerialName("class") val carClass: String? = null,
    val sectors: List<Double> = emptyList(),
    val dr: RankDto? = null,
    val sr: RankDto? = null,
)

@Serializable
data class RankDto(val rank: String? = null)

@Serializable
data class PortalTrackDto(
    val slug: String = "",
    @SerialName("track_name") val trackName: String = "",
    @SerialName("short_name") val shortName: String = "",
    val town: String? = null,
    val country: String? = null,
    @SerialName("length_km") val lengthKm: Double? = null,
    @SerialName("num_turns") val numTurns: Int? = null,
    val aliases: List<String> = emptyList(),
    @SerialName("track_map_url") val trackMapUrl: String? = null,
)

@Serializable
data class PortalCarClassDto(
    @SerialName("class_id") val classId: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("portal_name") val portalName: String = "",
    @SerialName("badge_colour") val badgeColour: String? = null,
)
