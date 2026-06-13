package com.orioooneee.lmuasister.data

import com.orioooneee.lmuasister.data.model.ClassInfo
import com.orioooneee.lmuasister.data.model.Hotlap
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceSettings
import com.orioooneee.lmuasister.data.model.RaceType
import com.orioooneee.lmuasister.data.model.RaceWeather
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.data.model.WeatherSegment
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.AppJson
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.ClassInfoDto
import com.orioooneee.lmuasister.data.remote.HotlapDto
import com.orioooneee.lmuasister.data.remote.LeaderboardEntryDto
import com.orioooneee.lmuasister.data.remote.RaceDto
import com.orioooneee.lmuasister.data.remote.ScheduleResponse
import com.orioooneee.lmuasister.data.remote.SessionWeatherDto
import com.orioooneee.lmuasister.data.remote.SettingsDto
import com.orioooneee.lmuasister.data.remote.TrackDto
import com.orioooneee.lmuasister.data.remote.WeatherDto
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private const val CACHE_KEY = "schedule_v1"
private const val STATUS_READY = "ready"
private const val HOTLAP_POLLS = 6
private const val HOTLAP_POLL_DELAY_MS = 1500L

/** On-disk schedule snapshot with the fetch time, for offline-first cold starts. */
@Serializable
private data class CachedSchedule(val ts: Long = 0, val data: ScheduleResponse = ScheduleResponse())

/**
 * Thin client over the LmuAssister backend. The backend already merges every
 * source into the unified race model, so this just fetches, caches the one
 * `/schedule` payload, and maps DTOs → [Race]/[Schedule].
 */
class RaceRepository(private val api: BackendApi) {

    // Offline-first: the whole schedule arrives in one call. We keep it in memory
    // and persist it (1 wrapper with a timestamp) so a cold start can paint instantly
    // from disk, then refresh from the network.
    private var mem: ScheduleResponse? = null

    private fun disk(): ScheduleResponse? = runCatching {
        LocalCache.read(CACHE_KEY)?.let { AppJson.decodeFromString<CachedSchedule>(it).data }
    }.getOrNull()

    private fun cached(): ScheduleResponse? = mem ?: disk()?.also { mem = it }

    private suspend fun network(refresh: Boolean): ScheduleResponse =
        api.schedule(refresh = refresh).also { resp ->
            mem = resp
            runCatching {
                val wrapped = CachedSchedule(Clock.System.now().toEpochMilliseconds(), resp)
                LocalCache.write(CACHE_KEY, AppJson.encodeToString(wrapped))
            }
        }

    private suspend fun full(refresh: Boolean): ScheduleResponse =
        if (refresh) network(refresh = true) else cached() ?: network(refresh = false)

    /** Cache-only week keys (memory/disk, no network) — for an instant first paint. */
    fun cachedWeeks(): List<String>? = cached()?.weeks?.map { it.key }

    /** Force a network refetch; updates the in-memory + on-disk cache. */
    suspend fun refreshSchedule(): Result<Unit> = runCatching { network(refresh = true); Unit }

    /** Week keys (current + upcoming), for the week picker. */
    suspend fun availableWeeks(refresh: Boolean = false): List<String> =
        runCatching { full(refresh).weeks.map { it.key } }.getOrDefault(emptyList())

    suspend fun load(weekKeyOverride: String? = null, refresh: Boolean = false): Result<Schedule> = runCatching {
        val resp = full(refresh)
        val key = weekKeyOverride ?: resp.weeks.firstOrNull()?.key ?: error("No race weeks available")
        Schedule(resp.schedules[key].orEmpty().map { it.toModel(api::imageUrl) })
    }

    /** Fastest-lap leaderboard — the fast `/race/<id>` call. */
    suspend fun leaderboard(raceId: String): Result<List<LapEntry>> = runCatching {
        api.race(raceId).leaderboard.map { it.toModel() }
    }

    /**
     * YouTube hot-laps for the race's track. The endpoint builds them asynchronously,
     * so we poll until "ready"; if it's still pending we force a synchronous build.
     * Independent of [leaderboard] so the two load in parallel.
     */
    suspend fun hotlaps(raceId: String): Result<List<Hotlap>> = runCatching {
        repeat(HOTLAP_POLLS) {
            val r = api.hotlaps(raceId)
            if (r.status == STATUS_READY) return@runCatching r.hotlaps.map { it.toModel() }
            delay(HOTLAP_POLL_DELAY_MS)
        }
        api.hotlaps(raceId, wait = true).hotlaps.map { it.toModel() }
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

private fun HotlapDto.toModel() = Hotlap(
    title = title,
    videoId = videoId,
    url = url,
    thumbnail = thumbnail,
    author = author,
    driver = driver,
    lapTime = lapTime,
    car = car,
    carClass = carClass,
    classBadge = classBadge,
    gameVersion = gameVersion,
    views = views,
)
