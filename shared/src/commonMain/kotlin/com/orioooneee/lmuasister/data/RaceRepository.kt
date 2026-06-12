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
import com.orioooneee.lmuasister.data.remote.ChampionshipEventDto
import com.orioooneee.lmuasister.data.remote.ChampionshipSeriesDto
import com.orioooneee.lmuasister.data.remote.LmuCardImageApi
import com.orioooneee.lmuasister.data.remote.LmuPortalApi
import com.orioooneee.lmuasister.data.remote.PortalCarClassDto
import com.orioooneee.lmuasister.data.remote.PortalTrackDto
import com.orioooneee.lmuasister.data.remote.RaceSeriesDto
import com.orioooneee.lmuasister.data.remote.WeatherDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private fun String.token(): String = lowercase().filter { it.isLetterOrDigit() }
private fun String.baseToken(): String = substringBefore('(').token()

/**
 * Builds one [Schedule] from lmuportal (primary) + the S3 cover bucket:
 *  - race_series      → daily / weekly / special (current week)
 *  - championship_*   → the championship category
 *  - tracks/car_class → track details + class colours
 *  - S3 card bucket   → cover images
 */
class RaceRepository(
    private val portal: LmuPortalApi,
    private val cards: LmuCardImageApi,
) {
    /** Current week + upcoming weeks (keys like "2026-06-09"), for the week picker. */
    /** Fastest-lap leaderboard for a race (by its lmuportal leaderboard_id). */
    suspend fun leaderboard(leaderboardId: String): Result<List<LapEntry>> = runCatching {
        portal.leaderboard(leaderboardId).map { e ->
            LapEntry(
                rank = e.rank,
                initials = e.initials ?: e.displayName ?: "—",
                bestLapMs = e.scoreMs,
                sectors = e.metadata?.sectors.orEmpty(),
                car = e.metadata?.car,
                carClass = e.metadata?.carClass,
                drRank = e.metadata?.dr?.rank,
                srRank = e.metadata?.sr?.rank,
            )
        }
    }

    suspend fun availableWeeks(): List<String> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()
        val current = portal.currentWeekKey(today) ?: return emptyList()
        return runCatching { portal.weekKeys(current) }.getOrDefault(listOf(current)).take(6)
    }

    suspend fun load(weekKeyOverride: String? = null): Result<Schedule> = runCatching {
        val now = Clock.System.now()
        val todayDate = now.toLocalDateTime(TimeZone.UTC).date
        val weekKey = weekKeyOverride
            ?: portal.currentWeekKey(todayDate.toString())
            ?: error("Could not resolve the current race week")

        val s = shared()
        val series = portal.raceSeries(weekKey) // the only per-week network call

        val weekly = series.map { it.toRace(s.tracks, s.classes, s.images, todayDate, now) }
        val champ = s.champSeries.map { it.toRace(s.events, s.tracks, s.classes, s.images, todayDate, now) }
        Schedule(weekly + champ)
    }

    // ── shared, week-independent data (tracks / classes / images / championships) ──
    private class Shared(
        val tracks: TrackIndex,
        val classes: ClassIndex,
        val images: ImageIndex,
        val champSeries: List<ChampionshipSeriesDto>,
        val events: List<ChampionshipEventDto>,
    )

    private var sharedCache: Shared? = null

    private suspend fun shared(): Shared {
        sharedCache?.let { return it }
        return coroutineScope {
            val tracksD = async { runCatching { portal.tracks() }.getOrDefault(emptyList()) }
            val classesD = async { runCatching { portal.carClasses() }.getOrDefault(emptyList()) }
            val imagesD = async { runCatching { cards.fetchCards() }.getOrDefault(emptyList()) }
            val rcD = async { runCatching { cards.fetchRaceControlCovers() }.getOrDefault(emptyMap()) }
            val seasonD = async { runCatching { portal.activeSeason() }.getOrNull() }
            val eventsD = async { runCatching { portal.championshipEvents() }.getOrDefault(emptyList()) }

            val season = seasonD.await()
            val champSeries = season
                ?.let { runCatching { portal.championshipSeries(it.seasonId) }.getOrDefault(emptyList()) }
                .orEmpty()

            Shared(
                tracks = TrackIndex(tracksD.await()),
                classes = ClassIndex(classesD.await()),
                images = ImageIndex(imagesD.await(), rcD.await()),
                champSeries = champSeries,
                events = eventsD.await(),
            )
        }.also { sharedCache = it }
    }
}

// ── mapping ───────────────────────────────────────────────────────────────────

private fun timeAt(date: LocalDate, hhmmss: String): Instant? {
    val p = hhmmss.split(":")
    val h = p.getOrNull(0)?.toIntOrNull() ?: return null
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    return runCatching { date.atTime(LocalTime(h, m)).toInstant(TimeZone.UTC) }.getOrNull()
}

/**
 * The full slate of start times for the event's *current active day* — past AND
 * future, so the UI can show the whole grid with past slots struck through.
 * [days] are 0=Sun..6=Sat (lmuportal convention); [times] run on those days,
 * [nextDayTimes] on the morning after. Returns the first upcoming race day's slate.
 */
private fun activeDaySlots(
    days: List<Int>,
    times: List<String>,
    nextDayTimes: List<String>,
    today: LocalDate,
    now: Instant,
): List<Instant> {
    if (days.isEmpty() || (times.isEmpty() && nextDayTimes.isEmpty())) return emptyList()
    for (offset in 0..8) {
        val date = today.plus(offset, DateTimeUnit.DAY)
        if (date.dayOfWeek.isoDayNumber % 7 !in days) continue
        val slots = buildList {
            times.forEach { t -> timeAt(date, t)?.let(::add) }
            val nd = date.plus(1, DateTimeUnit.DAY)
            nextDayTimes.forEach { t -> timeAt(nd, t)?.let(::add) }
        }.distinct().sorted()
        if (slots.any { it >= now }) return slots
    }
    return emptyList()
}

private fun RaceSeriesDto.toRace(
    tracks: TrackIndex,
    classes: ClassIndex,
    images: ImageIndex,
    today: LocalDate,
    now: Instant,
): Race {
    val displayCircuit = trackConfig?.takeIf { it.isNotBlank() }
        ?.let { "$trackFriendly ($it)" } ?: trackFriendly
    val times = if (todaysStartTimes.isNotEmpty()) {
        activeDaySlots((0..6).toList(), todaysStartTimes, emptyList(), today, now)
    } else {
        activeDaySlots(raceDays, timesUtc, emptyList(), today, now)
    }
    val track = tracks.lookup(trackSlug, trackFriendly)
    return Race(
        id = seriesId,
        type = RaceType.from(type),
        series = seriesName,
        circuit = displayCircuit,
        difficulty = tierName.replaceFirstChar { it.uppercase() },
        carClasses = carClasses,
        classInfos = carClasses.map { classes.lookup(it) },
        times = times,
        raceLength = raceDuration,
        settings = RaceSettings(
            setup = setup,
            assists = null,
            damage = null,
            tireWear = null,
            fuelUsage = fuelMultiplier?.toInt(),
            safetyRank = srRequirement,
            driverRank = null,
            splitSize = splitSize,
            qualifyingLength = qualifyingDuration,
            practiceLength = practiceDuration,
            driverSwaps = driverSwap,
            trackLimits = null,
            tireWarmers = tyreWarmers?.let { if (it) "On" else "Off" },
            limitedTires = tyreAllowance?.toString(),
        ),
        track = track,
        imageUrl = images.match(track?.shortName ?: trackFriendly, carClasses, seriesName),
        weather = buildWeather(raceWeather, qualifyingWeather, practiceWeather),
        leaderboardId = leaderboardId,
    )
}

private fun WeatherDto?.toSession(): SessionWeather? {
    if (this == null || weather.isEmpty()) return null
    return SessionWeather(
        timeOfDay = timeOfDay,
        segments = weather.map { s ->
            WeatherSegment(
                sky = s.sky?.toIntOrNull() ?: 0,
                tempC = s.temperature?.toIntOrNull(),
                humidity = s.humidity?.toIntOrNull(),
                windKmh = s.windSpeed?.toIntOrNull(),
                rainChance = s.rainChange?.toIntOrNull(),
                durationMin = s.durationMinutes,
            )
        },
    )
}

private fun buildWeather(race: WeatherDto?, quali: WeatherDto?, practice: WeatherDto?): RaceWeather? {
    val w = RaceWeather(practice.toSession(), quali.toSession(), race.toSession())
    return if (w.isEmpty) null else w
}

private fun ChampionshipSeriesDto.toRace(
    events: List<ChampionshipEventDto>,
    tracks: TrackIndex,
    classes: ClassIndex,
    images: ImageIndex,
    today: LocalDate,
    now: Instant,
): Race {
    // current round track comes from the next (or most recent) scheduled event
    val mine = events.filter { it.seriesId == id }
        .mapNotNull { e -> runCatching { Instant.parse(e.raceStartsAt) }.getOrNull()?.let { it to e } }
    val round = (mine.filter { it.first >= now }.minByOrNull { it.first }
        ?: mine.maxByOrNull { it.first })?.second?.roundName
    val circuit = round?.substringAfter(':')?.trim()?.takeIf { it.isNotBlank() } ?: "Multi-round"
    val track = tracks.lookup(null, circuit)

    return Race(
        id = id,
        type = RaceType.CHAMPIONSHIP,
        series = seriesName,
        circuit = circuit,
        difficulty = "Championship",
        carClasses = carClasses,
        classInfos = carClasses.map { classes.lookup(it) },
        times = activeDaySlots(raceDays, raceTimesUtc, raceTimeNextDay, today, now),
        raceLength = raceDuration,
        settings = RaceSettings(
            setup = setup,
            assists = null,
            damage = null,
            tireWear = null,
            fuelUsage = null,
            safetyRank = srRequirement,
            driverRank = null,
            splitSize = splitSize,
            qualifyingLength = qualifyingDuration,
            practiceLength = practiceDuration,
            driverSwaps = driverSwap,
            trackLimits = null,
            tireWarmers = null,
            limitedTires = null,
        ),
        track = track,
        imageUrl = images.match(track?.shortName ?: circuit, carClasses, seriesName),
    )
}

// ── indexes ─────────────────────────────────────────────────────────────────

private class TrackIndex(dtos: List<PortalTrackDto>) {
    private val bySlug = HashMap<String, TrackInfo>()
    private val byToken = HashMap<String, TrackInfo>()

    init {
        dtos.forEach { dto ->
            val info = TrackInfo(
                name = dto.trackName,
                shortName = dto.shortName,
                town = dto.town,
                country = dto.country,
                lengthKm = dto.lengthKm,
                numTurns = dto.numTurns,
                // force https + encode spaces — Android blocks cleartext and the path has "Track Maps"
                mapUrl = dto.trackMapUrl?.replace("http://", "https://")?.replace(" ", "%20"),
            )
            if (dto.slug.isNotBlank()) bySlug[dto.slug] = info
            (listOf(dto.shortName, dto.trackName) + dto.aliases)
                .map { it.token() }.filter { it.isNotEmpty() }
                .forEach { byToken.putIfAbsent(it, info) }
        }
    }

    fun lookup(slug: String?, name: String): TrackInfo? {
        slug?.let { bySlug[it] }?.let { return it }
        val key = name.baseToken()
        if (key.isEmpty()) return null
        byToken[key]?.let { return it }
        return byToken.entries.firstOrNull { (t, _) -> t.contains(key) || key.contains(t) }?.value
    }
}

private class ClassIndex(dtos: List<PortalCarClassDto>) {
    private val byToken: Map<String, ClassInfo> = buildMap {
        dtos.forEach { dto ->
            val info = ClassInfo(dto.classId, dto.displayName.ifBlank { dto.portalName }, dto.badgeColour)
            listOf(dto.classId, dto.displayName, dto.portalName)
                .map { it.token() }.filter { it.isNotEmpty() }
                .forEach { putIfAbsent(it, info) }
        }
    }

    fun lookup(raw: String): ClassInfo {
        val key = raw.baseToken()
        byToken[key]?.let { return it }
        byToken.entries.firstOrNull { (t, _) -> t.contains(key) || key.contains(t) }?.let { return it.value }
        return ClassInfo(id = key, name = raw.substringBefore('(').trim(), colorHex = null)
    }
}

private class ImageIndex(urls: List<String>, rcCovers: Map<String, String>) {
    private data class Card(
        val url: String,
        val date: String,
        val classTok: String,
        val trackTok: String,
        val fullTok: String,
    )

    // racecontrol.gg editorial cover keyed by series-name token (takes priority)
    private val rcByName: Map<String, String> =
        rcCovers.entries.associate { (title, url) -> title.token() to url }

    private val cards: List<Card> = urls.map { url ->
        val name = url.substringAfterLast('/').substringBeforeLast('.')
        val parts = name.split('_')
        val lmuIdx = parts.indexOfFirst { it.equals("LMU", ignoreCase = true) }
        val hasMeta = lmuIdx >= 0 && parts.size > lmuIdx + 2
        Card(
            url = url,
            date = parts.getOrElse(0) { "" },
            classTok = if (hasMeta) parts[lmuIdx + 1].token() else "",
            trackTok = if (hasMeta) parts.drop(lmuIdx + 2).joinToString("").token() else "",
            fullTok = name.token(),
        )
    }

    fun match(trackName: String, carClasses: List<String>, series: String): String? =
        rcByName[series.token()] // exact racecontrol cover for this series
            ?: byTrack(trackName, carClasses)
            ?: bySeries(series)

    private fun byTrack(trackName: String, carClasses: List<String>): String? {
        val trackKey = trackName.baseToken()
        if (trackKey.isEmpty()) return null
        val classKeys = carClasses.map { it.baseToken() }.filter { it.isNotEmpty() }
        val sameTrack = cards.filter {
            it.trackTok.isNotEmpty() && (it.trackTok.contains(trackKey) || trackKey.contains(it.trackTok))
        }.ifEmpty { return null }
        return sameTrack
            .sortedWith(
                compareByDescending<Card> { c -> classKeys.any { c.classTok.contains(it) || it.contains(c.classTok) } }
                    .thenByDescending { it.date },
            )
            .first().url
    }

    /** Fallback for promo events without a track-named card (e.g. "LMGC_Q3" for the G Challenge). */
    private fun bySeries(series: String): String? {
        val words = series.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        if (words.size < 2) return null
        val acronym = words.joinToString("") { it.take(1) }            // "lmgcq"
        val last = words.last()                                        // "q3"
        val byAcr = cards.filter { it.fullTok.contains(acronym) }.ifEmpty { return null }
        val refined = byAcr.filter { it.fullTok.contains(last) }.ifEmpty { byAcr }
        return refined.maxByOrNull { it.fullTok }?.url
    }
}
