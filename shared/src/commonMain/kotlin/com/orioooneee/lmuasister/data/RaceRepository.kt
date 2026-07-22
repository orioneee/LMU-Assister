package com.orioooneee.lmuasister.data

import com.orioooneee.lmuasister.data.model.CarGroup
import com.orioooneee.lmuasister.data.model.AvailableCar
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
import com.orioooneee.lmuasister.data.model.ScheduleCategory
import com.orioooneee.lmuasister.data.model.SchedulePeriod
import com.orioooneee.lmuasister.data.model.ScheduleSlice
import com.orioooneee.lmuasister.data.model.ScheduleWeek
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.data.model.TopCar
import com.orioooneee.lmuasister.data.model.TopCarLivery
import com.orioooneee.lmuasister.data.model.TopCarsAvailableClass
import com.orioooneee.lmuasister.data.model.TopCarsCache
import com.orioooneee.lmuasister.data.model.TopCarsResult
import com.orioooneee.lmuasister.data.model.TopCarsScope
import com.orioooneee.lmuasister.data.model.WeatherSegment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.AppJson
import com.orioooneee.lmuasister.data.remote.AppTokenHolder
import com.orioooneee.lmuasister.data.remote.BackendApiException
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.CarDetailedDto
import com.orioooneee.lmuasister.data.remote.CarDto
import com.orioooneee.lmuasister.data.remote.TrackFullDto
import com.orioooneee.lmuasister.data.remote.ClassInfoDto
import com.orioooneee.lmuasister.data.remote.ClassLeaderboardDto
import com.orioooneee.lmuasister.data.remote.HotlapDto
import com.orioooneee.lmuasister.data.remote.LeaderboardEntryDto
import com.orioooneee.lmuasister.data.remote.RaceDto
import com.orioooneee.lmuasister.data.remote.ScheduleResponse
import com.orioooneee.lmuasister.data.remote.ScheduleNotificationResponse
import com.orioooneee.lmuasister.data.remote.ScheduleUpdateSubscription
import com.orioooneee.lmuasister.data.remote.SessionWeatherDto
import com.orioooneee.lmuasister.data.remote.SettingsDto
import com.orioooneee.lmuasister.data.remote.TrackDto
import com.orioooneee.lmuasister.data.remote.TopCarDto
import com.orioooneee.lmuasister.data.remote.TopCarLiveryDto
import com.orioooneee.lmuasister.data.remote.TopCarsAvailableClassDto
import com.orioooneee.lmuasister.data.remote.TopCarsCacheDto
import com.orioooneee.lmuasister.data.remote.TopCarsResponse
import com.orioooneee.lmuasister.data.remote.TopCarsScopeDto
import com.orioooneee.lmuasister.data.remote.WeatherDto
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

// Keep schedule cache keys versioned so older full-schedule payloads do not masquerade
// as the new per-week/per-tab slices.
private const val SCHEDULE_CACHE_PREFIX = "schedule_v4"
private const val STATUS_READY = "ready"
private const val HOTLAP_POLLS = 6
private const val HOTLAP_POLL_DELAY_MS = 1500L

@Serializable
private data class CachedScheduleSlice(val ts: Long = 0, val data: ScheduleResponse = ScheduleResponse())

@Serializable
private data class LbCache(
    val ts: Long = 0,
    val overall: ClassLeaderboardDto? = null,
    val byClass: List<ClassLeaderboardDto> = emptyList(),
)

@Serializable
private data class HlCache(val ts: Long = 0, val data: List<HotlapDto> = emptyList())

@Serializable
private data class TcCache(val ts: Long = 0, val data: TopCarsResponse = TopCarsResponse())

@Serializable
private data class CarsCache(val ts: Long = 0, val data: List<CarDto> = emptyList())

private const val CARS_KEY = "cars_v3"

@Serializable
private data class DetailedCarsCache(val ts: Long = 0, val data: List<CarDetailedDto> = emptyList())

private const val DETAILED_CARS_KEY = "cars_detailed_v3"

@Serializable
private data class TracksCache(val ts: Long = 0, val data: List<TrackFullDto> = emptyList())

private const val TRACKS_KEY = "tracks_v3"

private fun dedupCars(dtos: List<CarDto>): List<CarModel> =
    dtos.map { CarModel(it.id, it.name, it.manufacturer, it.model, it.carClass, it.series, it.engine) }
        .distinctBy { "${it.manufacturer}|${it.model}|${it.carClass}" }
        .sortedWith(compareBy({ it.carClass }, { it.manufacturer ?: "" }, { it.model }))

private data class ScheduleKey(
    val period: SchedulePeriod,
    val category: ScheduleCategory,
)

/**
 * Thin client over the LmuAssister backend. The backend already merges every
 * source into the unified race model, so this fetches schedule slices on demand
 * and maps DTOs → [Race]/[Schedule].
 */
class RaceRepository(
    private val api: BackendApi,
    private val tokenHolder: AppTokenHolder,
) {

    private val scheduleMem = mutableMapOf<ScheduleKey, ScheduleResponse>()

    fun cachedSchedule(period: SchedulePeriod, category: ScheduleCategory): ScheduleSlice? {
        val key = ScheduleKey(period, category)
        return cachedSlice(key)
    }

    suspend fun loadSchedule(
        period: SchedulePeriod,
        category: ScheduleCategory,
        refresh: Boolean = false,
    ): Result<ScheduleSlice> = runCatching {
        val key = ScheduleKey(period, category)
        if (refresh) {
            network(key, refresh = true).toSlice()
        } else {
            cachedSlice(key) ?: network(key, refresh = false).toSlice()
        }
    }

    private fun cached(key: ScheduleKey): ScheduleResponse? =
        scheduleMem[key] ?: disk(key)?.also { scheduleMem[key] = it }

    private fun cachedSlice(key: ScheduleKey): ScheduleSlice? =
        cached(key)?.let { resp ->
            runCatching { resp.toSlice() }
                .onFailure { scheduleMem.remove(key) }
                .getOrNull()
        }

    private fun disk(key: ScheduleKey): ScheduleResponse? = runCatching {
        LocalCache.read(key.cacheKey)?.let { AppJson.decodeFromString<CachedScheduleSlice>(it).data }
    }.getOrNull()

    private suspend fun network(key: ScheduleKey, refresh: Boolean): ScheduleResponse =
        api.schedule(key.period, key.category, refresh = refresh).also { resp ->
            scheduleMem[key] = resp
            runCatching {
                val wrapped = CachedScheduleSlice(Clock.System.now().toEpochMilliseconds(), resp)
                LocalCache.write(key.cacheKey, AppJson.encodeToString(wrapped))
            }
        }

    private val ScheduleKey.cacheKey: String
        get() = "${SCHEDULE_CACHE_PREFIX}_${period.name.lowercase()}_${category.name.lowercase()}"

    private val lbMem = mutableMapOf<String, RaceLeaderboards>()
    private val hlMem = mutableMapOf<String, List<Hotlap>>()
    private val tcMem = mutableMapOf<String, TopCarsResult>()

    fun peekLeaderboards(raceId: String): RaceLeaderboards? = lbMem[raceId]
    fun peekHotlaps(raceId: String): List<Hotlap>? = hlMem[raceId]
    fun peekTopCars(raceId: String, carClass: String? = null): TopCarsResult? =
        tcMem[topCarsCacheKey(raceId, carClass)]

    fun cachedLeaderboards(raceId: String): RaceLeaderboards? =
        lbMem[raceId] ?: diskList<LbCache>("lb_$raceId")?.let { c ->
            RaceLeaderboards(c.overall?.toModel(), c.byClass.map { it.toModel() })
        }?.also { lbMem[raceId] = it }

    fun cachedHotlaps(raceId: String): List<Hotlap>? =
        hlMem[raceId] ?: diskList<HlCache>("hl_$raceId")?.data?.map { it.toModel() }?.also { hlMem[raceId] = it }

    fun cachedTopCars(raceId: String, carClass: String? = null): TopCarsResult? {
        val key = topCarsCacheKey(raceId, carClass)
        return tcMem[key] ?: diskList<TcCache>(key)?.data?.toModel()
            ?.takeIf { it.isReady }
            ?.also { tcMem[key] = it }
    }

    suspend fun leaderboards(raceId: String): Result<RaceLeaderboards> = runCatching {
        val resp = api.race(raceId)
        val overall = resp.leaderboards?.overall
            ?: ClassLeaderboardDto(leaderboardId = resp.race?.leaderboardId, entries = resp.leaderboard)
        val byClass = resp.leaderboards?.byClass.orEmpty()
        lbMem[raceId] = RaceLeaderboards(overall.toModel(), byClass.map { it.toModel() })
        runCatching { LocalCache.write("lb_$raceId", AppJson.encodeToString(LbCache(nowMs(), overall, byClass))) }
        lbMem.getValue(raceId)
    }

    suspend fun topCars(raceId: String, carClass: String? = null, fetch: Boolean = false): Result<TopCarsResult> = runCatching {
        val resp = api.topCars(raceId, carClass = carClass, fetch = fetch)
        val result = resp.toModel()
        if (result.isReady) {
            val key = topCarsCacheKey(raceId, carClass)
            tcMem[key] = result
            runCatching { LocalCache.write(key, AppJson.encodeToString(TcCache(nowMs(), resp))) }
        }
        result
    }

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

    fun cachedCars(): List<CarModel>? =
        carsMem ?: diskList<CarsCache>(CARS_KEY)?.data?.let { dedupCars(it) }?.also { carsMem = it }

    suspend fun cars(): Result<List<CarModel>> = runCatching {
        val resp = api.cars()
        val models = dedupCars(resp.cars)
        carsMem = models
        runCatching { LocalCache.write(CARS_KEY, AppJson.encodeToString(CarsCache(nowMs(), resp.cars))) }
        models
    }

    private var detailedCarsMem: List<CarDetailedDto>? = null

    fun cachedDetailedCars(): List<CarDetailedDto>? =
        detailedCarsMem ?: diskList<DetailedCarsCache>(DETAILED_CARS_KEY)?.data
            ?.let { sortDetailedCars(it) }
            ?.also { detailedCarsMem = it }

    suspend fun detailedCars(): Result<List<CarDetailedDto>> = runCatching {
        val raw = api.carsDetailed().cars
        val list = sortDetailedCars(raw)
        detailedCarsMem = list
        runCatching { LocalCache.write(DETAILED_CARS_KEY, AppJson.encodeToString(DetailedCarsCache(nowMs(), raw))) }
        list
    }

    suspend fun detailedCar(id: String): CarDetailedDto? {
        fun List<CarDetailedDto>?.findCar(): CarDetailedDto? = this?.firstOrNull {
            it.id == id || it.slug == id
        }
        return cachedDetailedCars().findCar() ?: detailedCars().getOrNull().findCar()
    }

    private var tracksMem: List<TrackFullDto>? = null

    fun cachedTracks(): List<TrackFullDto>? =
        tracksMem ?: diskList<TracksCache>(TRACKS_KEY)?.data?.let { dedupTracks(it) }?.also { tracksMem = it }

    suspend fun tracks(): Result<List<TrackFullDto>> = runCatching {
        val raw = api.tracks().tracks
        val list = dedupTracks(raw)
        tracksMem = list
        runCatching { LocalCache.write(TRACKS_KEY, AppJson.encodeToString(TracksCache(nowMs(), raw))) }
        list
    }

    // The /tracks roster has true duplicates (same physical layout under several event codes,
    // e.g. Spa ×3, Silverstone ELMS+WEC). Collapse by physical signature, keep distinct configs
    // (Bahrain GP/Endurance/Outer/Paddock differ in length/corners, so they survive).
    private fun dedupTracks(list: List<TrackFullDto>): List<TrackFullDto> {
        val seen = HashSet<String>()
        return list.filter { seen.add("${it.base}|${it.lengthKm}|${it.corners}") }
    }

    private fun sortDetailedCars(list: List<CarDetailedDto>): List<CarDetailedDto> =
        list.sortedWith(compareBy({ it.category }, { it.manufacturer }, { it.name }))

    private var liveryModelsMem: Map<String, String>? = null

    suspend fun liveryToModel(): Map<String, String> {
        liveryModelsMem?.let { return it }
        val raw = diskList<CarsCache>(CARS_KEY)?.data?.takeIf { it.isNotEmpty() }
            ?: runCatching {
                val resp = api.cars()
                carsMem = dedupCars(resp.cars)
                runCatching { LocalCache.write(CARS_KEY, AppJson.encodeToString(CarsCache(nowMs(), resp.cars))) }
                resp.cars
            }.getOrNull().orEmpty()
        return raw.asSequence()
            .filter { it.name.isNotBlank() && it.model.isNotBlank() }
            .associate { it.name to it.model }
            .also { if (it.isNotEmpty()) liveryModelsMem = it }
    }

    fun leaderboardPager(leaderboardId: String): Pager<String, LapEntry> =
        Pager(
            PagingConfig(
                pageSize = LB_PAGE_SIZE,
                initialLoadSize = LB_PAGE_SIZE,
                prefetchDistance = LB_PAGE_SIZE,
                enablePlaceholders = false,
            ),
        ) {
            LeaderboardPagingSource(api, leaderboardId)
        }

    val appToken: StateFlow<String?> get() = tokenHolder.token

    suspend fun registerFcmToken(uuid: String, token: String): Result<Unit> =
        runCatching { api.registerFcmToken(uuid, token) }

    suspend fun createDevicePushScheduleNotification(
        deviceId: String,
        eventName: String,
        notifInSeconds: Int,
        notifTime: String,
    ): Result<ScheduleNotificationResponse> = runCatching {
        api.createDevicePushScheduleNotification(deviceId, eventName, notifInSeconds, notifTime)
    }

    suspend fun createEmailScheduleNotification(
        eventName: String,
        notifInSeconds: Int,
        notifTime: String,
    ): Result<ScheduleNotificationResponse> = runCatching {
        val token = tokenHolder.await(NOTIFICATION_TOKEN_WAIT_MS)
            ?: throw BackendApiException(401, "unauthorized", "Session token is not ready.")
        api.createEmailScheduleNotification(token, eventName, notifInSeconds, notifTime)
    }

    suspend fun scheduleUpdateSubscriptions(target: String?, includeEmail: Boolean): Result<List<ScheduleUpdateSubscription>> =
        runCatching {
            val token = if (includeEmail) tokenHolder.token.value?.takeIf { it.isNotBlank() } else null
            api.scheduleUpdateSubscriptions(target = target, token = token).subscriptions
        }

    suspend fun subscribeScheduleUpdatesDevicePush(target: String): Result<Unit> =
        runCatching {
            check(api.subscribeScheduleUpdatesDevicePush(target).ok)
        }

    suspend fun unsubscribeScheduleUpdatesDevicePush(target: String): Result<Unit> =
        runCatching {
            check(api.unsubscribeScheduleUpdatesDevicePush(target).ok)
        }

    suspend fun subscribeScheduleUpdatesEmail(): Result<Unit> = runCatching {
        val token = tokenHolder.await(NOTIFICATION_TOKEN_WAIT_MS)
            ?: throw BackendApiException(401, "unauthorized", "Session token is not ready.")
        check(api.subscribeScheduleUpdatesEmail(token).ok)
    }

    suspend fun unsubscribeScheduleUpdatesEmail(): Result<Unit> = runCatching {
        val token = tokenHolder.await(NOTIFICATION_TOKEN_WAIT_MS)
            ?: throw BackendApiException(401, "unauthorized", "Session token is not ready.")
        check(api.unsubscribeScheduleUpdatesEmail(token).ok)
    }

    suspend fun leaderboardMe(leaderboardId: String): LapEntry? {
        val token = tokenHolder.await(LB_TOKEN_WAIT_MS) ?: return null
        return runCatching {
            api.leaderboardPage(leaderboardId, limit = 1, token = token).me?.toModel()
        }.getOrNull()
    }
}

private const val LB_PAGE_SIZE = 100

private const val LB_TOKEN_WAIT_MS = 2500L

private const val NOTIFICATION_TOKEN_WAIT_MS = 10_000L

private fun topCarsCacheKey(raceId: String, carClass: String?): String =
    "topcars_v3_${raceId}_${topCarsScopeKey(carClass)}"

private fun topCarsScopeKey(carClass: String?): String =
    carClass?.takeIf { it.isNotBlank() }
        ?.lowercase()
        ?.filter { it.isLetterOrDigit() }
        ?.takeIf { it.isNotBlank() }
        ?: "overall"

private class LeaderboardPagingSource(
    private val api: BackendApi,
    private val leaderboardId: String,
) : PagingSource<String, LapEntry>() {

    override fun getRefreshKey(state: PagingState<String, LapEntry>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, LapEntry> = try {
        val page = api.leaderboardPage(leaderboardId, cursor = params.key, limit = params.loadSize)
        LoadResult.Page(
            data = page.entries.map { it.toModel() },
            prevKey = null,
            nextKey = page.nextCursor?.takeIf { it.isNotBlank() },
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
}

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

private fun ScheduleResponse.toSlice(): ScheduleSlice {
    val weekDto = weeks.firstOrNull()
    val weekKey = weekDto?.key?.takeIf { it.isNotBlank() }
        ?: schedules.keys.firstOrNull()
        ?: error("No race weeks available")
    val label = weekDto?.label?.takeIf { it.isNotBlank() } ?: weekKey
    return ScheduleSlice(
        week = ScheduleWeek(weekKey, label),
        schedule = Schedule(schedules[weekKey].orEmpty().map { it.toModel() }),
    )
}

private fun RaceDto.toModel(): Race = Race(
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
    track = track?.toModel(),
    imageUrl = cover,
    weather = weather?.toModel(),
    leaderboardId = leaderboardId,
    completed = completed,
    availableCars = availableCars.mapValues { (_, cars) ->
        cars.map { AvailableCar(it.friendly, it.manufacturer, it.carImageUrl, it.manufacturerLogoUrl) }
    },
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
    startIntervalMin = startIntervalMin,
    qualifyingLength = qualifyingLength,
    practiceLength = practiceLength,
    driverSwaps = driverSwaps,
    trackLimits = trackLimits,
    tireWarmers = tireWarmers,
    limitedTires = limitedTires,
    privateQualifying = privateQualifying,
    multiFormationLap = multiFormationLap,
    mechanicalFailures = mechanicalFailures,
    raceTimeScale = raceTimeScale,
    realRoadScale = realRoadScale,
    trackLimitsPointsAllowed = trackLimitsPointsAllowed,
)

private fun TrackDto.toModel(): TrackInfo = TrackInfo(
    name = name,
    shortName = shortName,
    simpleName = simpleName,
    town = town,
    country = country,
    lengthKm = lengthKm?.toDoubleOrNull(),
    numTurns = numTurns,
    mapUrl = scheme,
    logoUrl = logo,
    cardUrl = cover,
    backgroundUrl = background,
    countryCode = countryCode,
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
            skyLabel = it.skyLabel,
            kind = it.kind,
            icon = it.icon,
            tempC = it.tempC,
            humidity = it.humidity,
            windKmh = it.windKmh,
            rainChance = it.rainChance,
            durationMin = it.durationMin,
        )
    },
)

private fun ClassLeaderboardDto.toModel() = ClassLeaderboard(
    carClass = carClass ?: classId ?: "-",
    leaderboardId = leaderboardId,
    entries = entries.map { it.toModel() },
)

private fun TopCarsResponse.toModel() = TopCarsResult(
    status = status,
    eventId = eventId,
    reason = reason,
    message = message,
    leaderboardLimit = leaderboardLimit,
    leaderboardRecords = leaderboardRecords,
    cachedAt = cachedAt,
    expiresAt = expiresAt,
    scope = scope?.toModel(),
    classes = classes.map { it.toModel() },
    cache = cache.toModel(),
    topCars = topcars.map { it.toModel() },
)

private fun TopCarsScopeDto.toModel() = TopCarsScope(
    type = type,
    carClass = carClass,
    classKey = classKey,
)

private fun TopCarsAvailableClassDto.toModel() = TopCarsAvailableClass(
    carClass = carClass,
    classKey = classKey,
    leaderboardId = leaderboardId,
)

private fun TopCarsCacheDto.toModel() = TopCarsCache(
    hit = hit,
    ttlSeconds = ttlSeconds,
    cachedAt = cachedAt,
    expiresAt = expiresAt,
)

private fun TopCarDto.toModel() = TopCar(
    rank = rank,
    car = car,
    model = model,
    manufacturer = manufacturer,
    manufacturerLogoUrl = manufacturerLogoUrl,
    carClass = carClass,
    count = count,
    bestRank = bestRank,
    topLapMs = topLapMs,
    bestLapMs = bestLapMs,
    firstLiveryName = firstLiveryName,
    firstLivery = firstLivery?.toModel(),
)

private fun TopCarLiveryDto.toModel() = TopCarLivery(
    id = id,
    name = name,
    series = series,
    imageUrl = imageUrl.takeIf { it.isNotBlank() } ?: url.takeIf { it.isNotBlank() },
    model = model,
    manufacturer = manufacturer,
    manufacturerLogoUrl = manufacturerLogoUrl,
    carClass = carClass,
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
    fasterThanPct = fasterThanPct,
    rankUnstable = rankUnstable,
    carImageUrl = carImageUrl,
    manufacturer = manufacturer,
    manufacturerLogoUrl = manufacturerLogoUrl,
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
