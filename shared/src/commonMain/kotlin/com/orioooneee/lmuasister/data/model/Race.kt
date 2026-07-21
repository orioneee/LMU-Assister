package com.orioooneee.lmuasister.data.model

import kotlin.time.Instant

data class ScheduleWeek(
    val key: String,
    val label: String,
)

enum class SchedulePeriod {
    CURRENT,
    NEXT,
}

enum class ScheduleCategory {
    RACES,
    SPECIAL,
    CHAMPIONSHIP,
}

data class ScheduleSlice(
    val week: ScheduleWeek,
    val schedule: Schedule,
)

enum class RaceType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    SPECIAL("Special"),
    CHAMPIONSHIP("Championship");

    companion object {
        fun from(raw: String): RaceType = when {
            raw.startsWith("Weekly", ignoreCase = true) -> WEEKLY
            raw.startsWith("Special", ignoreCase = true) -> SPECIAL
            raw.startsWith("Champ", ignoreCase = true) -> CHAMPIONSHIP
            else -> DAILY
        }
    }
}

data class ClassInfo(
    val id: String,
    val name: String,
    val colorHex: String?,
)

data class CarGroup(
    val carClass: String,
    val cars: List<String>,
)

data class AvailableCar(
    val friendly: String,
    val manufacturer: String,
    val carImageUrl: String?,
    val manufacturerLogoUrl: String?,
)

data class TrackInfo(
    val name: String,
    val shortName: String,
    /** Common/short name ("Le Mans") vs the official [name] ("Circuit de la Sarthe"). */
    val simpleName: String? = null,
    val town: String?,
    val country: String?,
    val lengthKm: Double?,
    val numTurns: Int?,
    /** Track scheme/minimap (svg). Carries the backend `scheme` asset. */
    val mapUrl: String?,
    val logoUrl: String? = null,
    /** Colored event card art (webp) — used as the track emblem banner. Backend `cover`. */
    val cardUrl: String? = null,
    /** Track background art (webp). Backend `background`. */
    val backgroundUrl: String? = null,
    /** ISO-3166 alpha-2 country code (for the flag), when the backend sends it. */
    val countryCode: String? = null,
)

/** One car from the v3 roster (deduped to distinct models for the carousel). */
data class CarModel(
    val id: String,
    val name: String,
    val manufacturer: String?,
    val model: String,
    val carClass: String,
    val series: String?,
    val engine: String?,
)

data class WeatherSegment(
    val sky: Int,
    val tempC: Int?,
    val humidity: Int?,
    val windKmh: Int?,
    val rainChance: Int?,
    val durationMin: Int?,
)

data class SessionWeather(
    val timeOfDay: String?,
    val segments: List<WeatherSegment>,
)

data class RaceWeather(
    val practice: SessionWeather?,
    val qualifying: SessionWeather?,
    val race: SessionWeather?,
) {
    val isEmpty: Boolean get() = practice == null && qualifying == null && race == null
}

data class RaceSettings(
    val setup: String?,
    val assists: String?,
    val damage: String?,
    val tireWear: String?,
    val fuelUsage: String?,
    val safetyRank: String?,
    val driverRank: String?,
    val splitSize: Int?,
    val startIntervalMin: Int?,
    val qualifyingLength: Int?,
    val practiceLength: Int?,
    val driverSwaps: Boolean?,
    val trackLimits: String?,
    val tireWarmers: String?,
    val limitedTires: String?,
    val privateQualifying: Boolean?,
    val multiFormationLap: Int?,
    val mechanicalFailures: Int?,
    val raceTimeScale: Int?,
    val realRoadScale: Int?,
    val trackLimitsPointsAllowed: Int?,
)

/**
 * Unified race, merged from all three sources:
 *  - lmuschedule API  → schedule core (series, circuit, classes, difficulty, times, length, settings)
 *  - lmuportal Supabase → [track] details + [classInfos] colours
 *  - LMU card S3 bucket → [imageUrl] cover
 */
data class Race(
    val id: String,
    val type: RaceType,
    val series: String,
    val circuit: String,
    val difficulty: String,
    val carClasses: List<String>,
    val classInfos: List<ClassInfo>,
    val carsByClass: List<CarGroup> = emptyList(),
    val times: List<Instant>,
    val raceLength: Int,
    val settings: RaceSettings,
    val track: TrackInfo?,
    val imageUrl: String?,
    val weather: RaceWeather? = null,
    val leaderboardId: String? = null,
    /** This period's race is over (week complete / season complete) — not active right now. */
    val completed: Boolean = false,
    /** Exact liveries allowed for this race, grouped by class, with CDN artwork. */
    val availableCars: Map<String, List<AvailableCar>> = emptyMap(),
) {
    val title: String get() = series.ifBlank { circuit }

    val accentColorHex: String? get() = classInfos.firstOrNull { it.colorHex != null }?.colorHex

    fun nextStart(now: Instant): Instant? = times.firstOrNull { it >= now }
}

data class ClassLeaderboard(
    val carClass: String,
    val leaderboardId: String?,
    val entries: List<LapEntry>,
)

data class RaceLeaderboards(
    val overall: ClassLeaderboard? = null,
    val byClass: List<ClassLeaderboard> = emptyList(),
) {
    val isEmpty: Boolean get() = overall == null && byClass.isEmpty()

    companion object {
        val EMPTY = RaceLeaderboards()
    }
}

data class TopCarsResult(
    val status: String,
    val eventId: String,
    val reason: String?,
    val message: String?,
    val leaderboardLimit: Int,
    val leaderboardRecords: Int,
    val cachedAt: String?,
    val expiresAt: String?,
    val scope: TopCarsScope?,
    val classes: List<TopCarsAvailableClass>,
    val cache: TopCarsCache,
    val topCars: List<TopCar>,
) {
    val isReady: Boolean get() = status.equals("ready", ignoreCase = true) && topCars.isNotEmpty()
    val isNoData: Boolean get() = status.equals("no_data", ignoreCase = true)
}

data class TopCarsScope(
    val type: String,
    val carClass: String?,
    val classKey: String?,
)

data class TopCarsAvailableClass(
    val carClass: String,
    val classKey: String,
    val leaderboardId: String?,
)

data class TopCarsCache(
    val hit: Boolean,
    val ttlSeconds: Long?,
    val cachedAt: String?,
    val expiresAt: String?,
)

data class TopCar(
    val rank: Int,
    val car: String,
    val model: String,
    val manufacturer: String?,
    val manufacturerLogoUrl: String?,
    val carClass: String?,
    val count: Int,
    val bestRank: Int,
    val topLapMs: Long,
    val bestLapMs: Long,
    val firstLiveryName: String?,
    val firstLivery: TopCarLivery?,
)

data class TopCarLivery(
    val id: String,
    val name: String,
    val series: String,
    val imageUrl: String?,
    val model: String,
    val manufacturer: String,
    val manufacturerLogoUrl: String,
    val carClass: String,
)

data class LapEntry(
    val rank: Int,
    val initials: String,
    val bestLapMs: Long,
    val sectors: List<Double>,
    val car: String?,
    val carClass: String?,
    val drRank: String?,
    val srRank: String?,
    // Only set on the signed-in player's own "Your position" row.
    val fasterThanPct: Double? = null,
    val rankUnstable: Boolean = false,
    val carImageUrl: String? = null,
    val manufacturer: String? = null,
    val manufacturerLogoUrl: String? = null,
)

/** A YouTube hot-lap video for a track (parsed server-side, no API key). */
data class Hotlap(
    val title: String,
    val videoId: String,
    val url: String,
    val thumbnail: String,
    val author: String?,
    val driver: String?,
    val lapTime: String?,
    val car: String?,
    val carClass: String?,
    val classBadge: String?,
    val gameVersion: String?,
    val views: Long,
)

data class Schedule(val races: List<Race>) {
    val daily: List<Race> get() = races.filter { it.type == RaceType.DAILY }
    val weekly: List<Race> get() = races.filter { it.type == RaceType.WEEKLY }
    val special: List<Race> get() = races.filter { it.type == RaceType.SPECIAL }
    val championship: List<Race> get() = races.filter { it.type == RaceType.CHAMPIONSHIP }
    val isEmpty: Boolean get() = races.isEmpty()
}
