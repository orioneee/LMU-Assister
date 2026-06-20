package com.orioooneee.lmuasister.data

import com.orioooneee.lmuasister.data.model.CarGroup
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.data.model.ClassInfo
import com.orioooneee.lmuasister.data.model.ClassLeaderboard
import com.orioooneee.lmuasister.data.model.Hotlap
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.data.model.RaceLeaderboards
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceSettings
import com.orioooneee.lmuasister.data.model.RaceType
import com.orioooneee.lmuasister.data.model.RaceWeather
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.data.model.WeatherSegment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.AppJson
import com.orioooneee.lmuasister.data.remote.AppTokenHolder
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.CarDto
import com.orioooneee.lmuasister.data.remote.ClassInfoDto
import com.orioooneee.lmuasister.data.remote.ClassLeaderboardDto
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

/** On-disk per-race detail snapshots (leaderboard / hot-laps). */
@Serializable
private data class LbCache(
    val ts: Long = 0,
    val overall: ClassLeaderboardDto? = null,
    val byClass: List<ClassLeaderboardDto> = emptyList(),
)

@Serializable
private data class HlCache(val ts: Long = 0, val data: List<HotlapDto> = emptyList())

@Serializable
private data class CarsCache(val ts: Long = 0, val data: List<CarDto> = emptyList())

private const val CARS_KEY = "cars_v2"

/** Distinct car models (collapse liveries/teams), sorted by class → manufacturer → model. */
private fun dedupCars(dtos: List<CarDto>): List<CarModel> =
    dtos.map { CarModel(it.id, it.name, it.manufacturer, it.model, it.carClass, it.series, it.engine) }
        .distinctBy { "${it.manufacturer}|${it.model}|${it.carClass}" }
        .sortedWith(compareBy({ it.carClass }, { it.manufacturer ?: "" }, { it.model }))

/**
 * Thin client over the LmuAssister backend. The backend already merges every
 * source into the unified race model, so this just fetches, caches the one
 * `/schedule` payload, and maps DTOs → [Race]/[Schedule].
 */
class RaceRepository(
    private val api: BackendApi,
    private val tokenHolder: AppTokenHolder,
) {

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
    suspend fun refreshSchedule(): Result<Unit> = runCatching { network(refresh = true) }.map { }

    /** Week keys (current + upcoming), for the week picker. */
    suspend fun availableWeeks(refresh: Boolean = false): List<String> =
        runCatching { full(refresh).weeks.map { it.key } }.getOrDefault(emptyList())

    suspend fun load(weekKeyOverride: String? = null, refresh: Boolean = false): Result<Schedule> = runCatching {
        val resp = full(refresh)
        val key = weekKeyOverride ?: resp.weeks.firstOrNull()?.key ?: error("No race weeks available")
        Schedule(resp.schedules[key].orEmpty().map { it.toModel(api::imageUrl) })
    }

    // Per-race detail caches (offline-first, like the schedule): instant on reopen,
    // and they survive restarts via disk so hot-laps don't rebuild every time.
    private val lbMem = mutableMapOf<String, RaceLeaderboards>()
    private val hlMem = mutableMapOf<String, List<Hotlap>>()

    /** In-memory only (safe to call during composition) — null if not loaded this session. */
    fun peekLeaderboards(raceId: String): RaceLeaderboards? = lbMem[raceId]
    fun peekHotlaps(raceId: String): List<Hotlap>? = hlMem[raceId]

    /** Memory, then disk — for the first paint before the network refresh. */
    fun cachedLeaderboards(raceId: String): RaceLeaderboards? =
        lbMem[raceId] ?: diskList<LbCache>("lb_$raceId")?.let { c ->
            RaceLeaderboards(c.overall?.toModel(), c.byClass.map { it.toModel() })
        }?.also { lbMem[raceId] = it }

    fun cachedHotlaps(raceId: String): List<Hotlap>? =
        hlMem[raceId] ?: diskList<HlCache>("hl_$raceId")?.data?.map { it.toModel() }?.also { hlMem[raceId] = it }

    /**
     * Fastest-lap boards — the fast `/race/<id>` call. Returns the overall board plus
     * one board per car class (for the detail-screen tabs). Caches mem + disk; falls
     * back to the legacy flat `leaderboard` field when the backend predates `leaderboards`.
     */
    suspend fun leaderboards(raceId: String): Result<RaceLeaderboards> = runCatching {
        val resp = api.race(raceId)
        val overall = resp.leaderboards?.overall
            ?: ClassLeaderboardDto(leaderboardId = resp.race?.leaderboardId, entries = resp.leaderboard)
        val byClass = resp.leaderboards?.byClass.orEmpty()
        lbMem[raceId] = RaceLeaderboards(overall.toModel(), byClass.map { it.toModel() })
        runCatching { LocalCache.write("lb_$raceId", AppJson.encodeToString(LbCache(nowMs(), overall, byClass))) }
        lbMem.getValue(raceId)
    }

    /**
     * YouTube hot-laps for the race's track. The endpoint builds them asynchronously,
     * so we poll until "ready"; if it's still pending we force a synchronous build.
     * Independent of [leaderboard] so the two load in parallel. Caches mem + disk.
     */
    suspend fun hotlaps(raceId: String): Result<List<Hotlap>> = runCatching {
        val dtos = pollHotlaps(raceId)
        hlMem[raceId] = dtos.map { it.toModel() }
        runCatching { LocalCache.write("hl_$raceId", AppJson.encodeToString(HlCache(nowMs(), dtos))) }
        hlMem.getValue(raceId)
    }

    private suspend fun pollHotlaps(raceId: String): List<HotlapDto> {
        repeat(HOTLAP_POLLS) {
            val r = api.hotlaps(raceId)
            if (r.status == STATUS_READY) return r.hotlaps
            delay(HOTLAP_POLL_DELAY_MS)
        }
        return api.hotlaps(raceId, wait = true).hotlaps
    }

    private inline fun <reified T> diskList(key: String): T? =
        runCatching { LocalCache.read(key)?.let { AppJson.decodeFromString<T>(it) } }.getOrNull()

    private var carsMem: List<CarModel>? = null

    /** Memory, then disk — for an instant first paint of the car carousel. */
    fun cachedCars(): List<CarModel>? =
        carsMem ?: diskList<CarsCache>(CARS_KEY)?.data?.let { dedupCars(it) }?.also { carsMem = it }

    /** Fetch + dedup the roster to distinct models; caches mem + disk. */
    suspend fun cars(): Result<List<CarModel>> = runCatching {
        val resp = api.cars()
        val models = dedupCars(resp.cars)
        carsMem = models
        runCatching { LocalCache.write(CARS_KEY, AppJson.encodeToString(CarsCache(nowMs(), resp.cars))) }
        models
    }

    /** Cursor-paginated full leaderboard, for the dedicated screen (Paging 3). */
    fun leaderboardPager(leaderboardId: String): Pager<String, LapEntry> =
        Pager(
            PagingConfig(
                pageSize = LB_PAGE_SIZE,
                initialLoadSize = LB_PAGE_SIZE,
                // load the next page well before the user reaches the edge → no visible loader
                prefetchDistance = LB_PAGE_SIZE,
                enablePlaceholders = false,
            ),
        ) {
            LeaderboardPagingSource(api, leaderboardId)
        }

    /**
     * The signed-in player's own row on a board (rank + lap), or null. Waits a short
     * moment for the app token (the session restores async at launch); if it isn't there
     * in time, returns null and the screen just renders the public board.
     */
    suspend fun leaderboardMe(leaderboardId: String): LapEntry? {
        val token = tokenHolder.await(LB_TOKEN_WAIT_MS) ?: return null
        return runCatching {
            api.leaderboardPage(leaderboardId, limit = 1, token = token).me?.toModel()
        }.getOrNull()
    }
}

// Backend caps a page at 100 rows; big page + big prefetch keeps the loader off-screen.
private const val LB_PAGE_SIZE = 100

// How long to wait for the app token before fetching the board without "me".
private const val LB_TOKEN_WAIT_MS = 2500L

/** Backs the full-leaderboard screen — keyed by the backend's opaque cursor. */
private class LeaderboardPagingSource(
    private val api: BackendApi,
    private val leaderboardId: String,
) : PagingSource<String, LapEntry>() {

    override fun getRefreshKey(state: PagingState<String, LapEntry>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, LapEntry> = try {
        val page = api.leaderboardPage(leaderboardId, cursor = params.key, limit = params.loadSize)
        LoadResult.Page(
            data = page.entries.map { it.toModel() },
            prevKey = null,                 // forward-only cursor
            nextKey = page.nextCursor?.takeIf { it.isNotBlank() },
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
}

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()


private fun RaceDto.toModel(resolveImage: (String?) -> String?): Race = Race(
    id = id,
    type = RaceType.from(type),
    series = series,
    circuit = circuit,
    difficulty = difficulty,
    carClasses = carClasses,
    classInfos = classInfos.map { it.toModel() },
    carsByClass = carsByClass.map { CarGroup(it.carClass, it.cars) },
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

private fun TrackDto.toModel(resolveImage: (String?) -> String?): TrackInfo {
    // Assets live at the same /track/<id>/ path as the minimap; if the backend doesn't
    // send them yet, derive from map_url (…/map.svg → …/logo.svg | …/card.webp).
    fun sibling(file: String): String? =
        mapUrl?.takeIf { it.endsWith("/map.svg") }?.removeSuffix("map.svg")?.plus(file)
    return TrackInfo(
        name = name,
        shortName = shortName,
        simpleName = simpleName,
        town = town,
        country = country,
        lengthKm = lengthKm?.toDoubleOrNull(),
        numTurns = numTurns,
        mapUrl = resolveImage(mapUrl),
        logoUrl = resolveImage(logoUrl ?: sibling("logo.svg")),
        cardUrl = resolveImage(cardUrl ?: sibling("card.webp")),
        countryCode = countryCode,
    )
}

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

private fun ClassLeaderboardDto.toModel() = ClassLeaderboard(
    carClass = carClass ?: "—",
    leaderboardId = leaderboardId,
    entries = entries.map { it.toModel() },
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
