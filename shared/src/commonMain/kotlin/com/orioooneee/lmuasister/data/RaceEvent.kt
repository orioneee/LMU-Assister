package com.orioooneee.lmuasister.data

/**
 * Domain model for a Le Mans Ultimate online event.
 *
 * These are the fields the original "LMU Daily" surfaces from racecontrol.gg /
 * the LMU community forum (parsed with jsoup). Keeping them in one model means a
 * real parser can later produce [RaceEvent]s without touching the UI.
 */
data class RaceEvent(
    val id: String,
    val title: String,
    val carClass: CarClass,
    val track: String,
    val countryFlag: String,        // emoji flag, e.g. "🇶🇦"
    val durationMinutes: Int,
    val skill: SkillLevel,
    val cadence: EventCadence,
    val format: RaceFormat,
    val scheduleLabel: String,      // human label, e.g. "Every day · :10 / :40"
    val nextStartLabel: String,     // e.g. "in 12 min" / "Mon 18:25"
    val startingSoon: Boolean = false,
    val rounds: Int = 1,
    val hasBroadcast: Boolean = false,
)

enum class CarClass(val label: String, val short: String) {
    HYPERCAR("Hypercar", "HYP"),
    LMP2("LMP2", "LMP2"),
    LMGT3("LMGT3", "GT3"),
    MULTICLASS("Multi-class", "MIX"),
}

enum class SkillLevel(val label: String) {
    ROOKIE("Rookie"),
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    PRO("Pro"),
}

enum class EventCadence(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    SPECIAL("Special"),
}

enum class RaceFormat(val label: String) {
    SPRINT("Sprint"),
    FEATURE("Feature"),
    ENDURANCE("Endurance"),
    CHAMPIONSHIP("Championship"),
}
