package com.orioooneee.lmuasister.data.model

import kotlin.time.Instant

/** The four event categories shown in the app. */
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

/** Car class enriched from lmuportal `car_class` (real display name + badge colour). */
data class ClassInfo(
    val id: String,
    val name: String,
    val colorHex: String?,
)

/** Track details enriched from lmuportal `tracks`. */
data class TrackInfo(
    val name: String,
    val shortName: String,
    val town: String?,
    val country: String?,
    val lengthKm: Double?,
    val numTurns: Int?,
    val mapUrl: String?,
)

/** Everything else from the lmuschedule race object — shown on the details screen. */
data class RaceSettings(
    val setup: String?,
    val assists: String?,
    val damage: String?,
    val tireWear: Int?,
    val fuelUsage: Int?,
    val safetyRank: String?,
    val driverRank: String?,
    val splitSize: Int?,
    val qualifyingLength: Int?,
    val practiceLength: Int?,
    val driverSwaps: Boolean?,
    val trackLimits: String?,
    val tireWarmers: String?,
    val limitedTires: String?,
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
    val times: List<Instant>,
    val raceLength: Int,
    val settings: RaceSettings,
    val track: TrackInfo?,
    val imageUrl: String?,
) {
    val title: String get() = series.ifBlank { circuit }

    /** Class accent colour for chips/borders (first class with a known colour). */
    val accentColorHex: String? get() = classInfos.firstOrNull { it.colorHex != null }?.colorHex

    fun nextStart(now: Instant): Instant? = times.firstOrNull { it >= now }
}

data class Schedule(val races: List<Race>) {
    val daily: List<Race> get() = races.filter { it.type == RaceType.DAILY }
    val weekly: List<Race> get() = races.filter { it.type == RaceType.WEEKLY }
    val special: List<Race> get() = races.filter { it.type == RaceType.SPECIAL }
    val championship: List<Race> get() = races.filter { it.type == RaceType.CHAMPIONSHIP }
    val isEmpty: Boolean get() = races.isEmpty()
}
