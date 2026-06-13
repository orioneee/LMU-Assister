package com.orioooneee.lmuasister.data

import com.orioooneee.lmuasister.data.model.ClassInfo
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceSettings
import com.orioooneee.lmuasister.data.model.RaceType
import com.orioooneee.lmuasister.data.model.RaceWeather
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.data.model.WeatherSegment
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.ClassInfoDto
import com.orioooneee.lmuasister.data.remote.LeaderboardEntryDto
import com.orioooneee.lmuasister.data.remote.RaceDto
import com.orioooneee.lmuasister.data.remote.ScheduleResponse
import com.orioooneee.lmuasister.data.remote.SessionWeatherDto
import com.orioooneee.lmuasister.data.remote.SettingsDto
import com.orioooneee.lmuasister.data.remote.TrackDto
import com.orioooneee.lmuasister.data.remote.WeatherDto
import kotlin.time.Instant

/**
 * Thin client over the LmuAssister backend. The backend already merges every
 * source into the unified race model, so this just fetches, caches the one
 * `/schedule` payload, and maps DTOs → [Race]/[Schedule].
 */
class RaceRepository(private val api: BackendApi) {

    /** The whole schedule arrives in one call; cache it so week switches are free. */
    private var cache: ScheduleResponse? = null

    private suspend fun full(refresh: Boolean): ScheduleResponse {
        if (!refresh) cache?.let { return it }
        return api.schedule(refresh = refresh).also { cache = it }
    }

    /** Week keys (current + upcoming), for the week picker. */
    suspend fun availableWeeks(): List<String> =
        runCatching { full(refresh = false).weeks.map { it.key } }.getOrDefault(emptyList())

    suspend fun load(weekKeyOverride: String? = null, refresh: Boolean = false): Result<Schedule> = runCatching {
        val resp = full(refresh)
        val key = weekKeyOverride ?: resp.weeks.firstOrNull()?.key ?: error("No race weeks available")
        Schedule(resp.schedules[key].orEmpty().map { it.toModel(api::imageUrl) })
    }

    /** Fastest-lap leaderboard for a race (fetched with its detail payload). */
    suspend fun leaderboard(raceId: String): Result<List<LapEntry>> = runCatching {
        api.race(raceId).leaderboard.map { it.toModel() }
    }
}

// ── mapping: backend DTOs → domain model ──────────────────────────────────────

private fun RaceDto.toModel(resolveImage: (String?) -> String?): Race = Race(
    id = id,
    type = RaceType.from(type),
    series = series,
    circuit = circuit,
    difficulty = difficulty,
    carClasses = carClasses,
    classInfos = classInfos.map { it.toModel() },
    times = times.mapNotNull { runCatching { Instant.parse(it) }.getOrNull() },
    raceLength = raceLength,
    settings = settings.toModel(),
    track = track?.toModel(resolveImage),
    imageUrl = resolveImage(imageUrl),
    weather = weather?.toModel(),
    leaderboardId = leaderboardId,
    completed = completed,
)

private fun ClassInfoDto.toModel() = ClassInfo(id = id, name = name, colorHex = colorHex)

private fun SettingsDto.toModel() = RaceSettings(
    setup = setup,
    assists = assists,
    damage = damage,
    tireWear = tireWear,
    fuelUsage = fuelUsage,
    safetyRank = safetyRank,
    driverRank = driverRank,
    splitSize = splitSize,
    qualifyingLength = qualifyingLength,
    practiceLength = practiceLength,
    driverSwaps = driverSwaps,
    trackLimits = trackLimits,
    tireWarmers = tireWarmers,
    limitedTires = limitedTires,
)

private fun TrackDto.toModel(resolveImage: (String?) -> String?) = TrackInfo(
    name = name,
    shortName = shortName,
    town = town,
    country = country,
    lengthKm = lengthKm,
    numTurns = numTurns,
    mapUrl = resolveImage(mapUrl),
)

private fun WeatherDto.toModel(): RaceWeather? = RaceWeather(
    practice = practice?.toModel(),
    qualifying = qualifying?.toModel(),
    race = race?.toModel(),
).takeUnless { it.isEmpty }

private fun SessionWeatherDto.toModel() = SessionWeather(
    timeOfDay = timeOfDay,
    segments = segments.map {
        WeatherSegment(
            sky = it.sky,
            tempC = it.tempC,
            humidity = it.humidity,
            windKmh = it.windKmh,
            rainChance = it.rainChance,
            durationMin = it.durationMin,
        )
    },
)

private fun LeaderboardEntryDto.toModel() = LapEntry(
    rank = rank,
    initials = initials,
    bestLapMs = bestLapMs,
    sectors = sectors,
    car = car,
    carClass = carClass,
    drRank = drRank,
    srRank = srRank,
)
