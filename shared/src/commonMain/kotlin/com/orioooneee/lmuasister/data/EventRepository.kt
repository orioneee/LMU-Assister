package com.orioooneee.lmuasister.data

/**
 * Source of [RaceEvent]s. Today it returns curated sample data so the UI can be
 * designed and previewed; later, swap [SampleEventRepository] for one backed by a
 * jsoup parser over racecontrol.gg / the LMU forum schedule thread.
 */
interface EventRepository {
    fun weeklyEvents(): List<RaceEvent>
    fun dailyEvents(): List<RaceEvent>
    fun specialEvents(): List<RaceEvent>

    fun allEvents(): List<RaceEvent> = weeklyEvents() + dailyEvents() + specialEvents()

    /** The single most relevant "what's on next" event for the Home hero. */
    fun nextUp(): RaceEvent
}

object SampleEventRepository : EventRepository {

    override fun weeklyEvents(): List<RaceEvent> = listOf(
        RaceEvent(
            id = "w-hyper-spa",
            title = "Hypercar Endurance Series",
            carClass = CarClass.HYPERCAR,
            track = "Spa-Francorchamps",
            countryFlag = "🇧🇪",
            durationMinutes = 60,
            skill = SkillLevel.ADVANCED,
            cadence = EventCadence.WEEKLY,
            format = RaceFormat.ENDURANCE,
            scheduleLabel = "Sundays · 19:00 CET",
            nextStartLabel = "Sun 19:00",
            rounds = 6,
            hasBroadcast = true,
        ),
        RaceEvent(
            id = "w-gt3-sprint",
            title = "LMGT3 Sprint Cup",
            carClass = CarClass.LMGT3,
            track = "Bahrain Int'l Circuit",
            countryFlag = "🇧🇭",
            durationMinutes = 42,
            skill = SkillLevel.INTERMEDIATE,
            cadence = EventCadence.WEEKLY,
            format = RaceFormat.SPRINT,
            scheduleLabel = "Mondays · 18:25 CET",
            nextStartLabel = "Mon 18:25",
            rounds = 4,
            hasBroadcast = true,
        ),
        RaceEvent(
            id = "w-lmp2-feature",
            title = "LMP2 Feature Race",
            carClass = CarClass.LMP2,
            track = "Autódromo José Carlos Pace",
            countryFlag = "🇧🇷",
            durationMinutes = 55,
            skill = SkillLevel.ADVANCED,
            cadence = EventCadence.WEEKLY,
            format = RaceFormat.FEATURE,
            scheduleLabel = "Wednesdays · 20:00 CET",
            nextStartLabel = "Wed 20:00",
            rounds = 5,
        ),
        RaceEvent(
            id = "w-multi-lemans",
            title = "Multi-class Night Race",
            carClass = CarClass.MULTICLASS,
            track = "Circuit de la Sarthe",
            countryFlag = "🇫🇷",
            durationMinutes = 90,
            skill = SkillLevel.PRO,
            cadence = EventCadence.WEEKLY,
            format = RaceFormat.ENDURANCE,
            scheduleLabel = "Fridays · 21:00 CET",
            nextStartLabel = "Fri 21:00",
            rounds = 3,
            hasBroadcast = true,
        ),
    )

    override fun dailyEvents(): List<RaceEvent> = listOf(
        RaceEvent(
            id = "d-gt3-qatar",
            title = "LMGT3 Daily",
            carClass = CarClass.LMGT3,
            track = "Lusail Int'l Circuit",
            countryFlag = "🇶🇦",
            durationMinutes = 25,
            skill = SkillLevel.BEGINNER,
            cadence = EventCadence.DAILY,
            format = RaceFormat.SPRINT,
            scheduleLabel = "Every day · :10 / :40",
            nextStartLabel = "in 12 min",
            startingSoon = true,
        ),
        RaceEvent(
            id = "d-hyper-imola",
            title = "Hypercar Daily",
            carClass = CarClass.HYPERCAR,
            track = "Imola",
            countryFlag = "🇮🇹",
            durationMinutes = 30,
            skill = SkillLevel.INTERMEDIATE,
            cadence = EventCadence.DAILY,
            format = RaceFormat.SPRINT,
            scheduleLabel = "Every day · :00 / :30",
            nextStartLabel = "in 28 min",
        ),
        RaceEvent(
            id = "d-lmp2-fuji",
            title = "LMP2 Daily",
            carClass = CarClass.LMP2,
            track = "Fuji Speedway",
            countryFlag = "🇯🇵",
            durationMinutes = 20,
            skill = SkillLevel.ROOKIE,
            cadence = EventCadence.DAILY,
            format = RaceFormat.SPRINT,
            scheduleLabel = "Every day · :15 / :45",
            nextStartLabel = "in 43 min",
        ),
    )

    override fun specialEvents(): List<RaceEvent> = listOf(
        RaceEvent(
            id = "s-champ-s7",
            title = "LMU Online Championship · S7",
            carClass = CarClass.MULTICLASS,
            track = "Multi-round calendar",
            countryFlag = "🏁",
            durationMinutes = 120,
            skill = SkillLevel.PRO,
            cadence = EventCadence.SPECIAL,
            format = RaceFormat.CHAMPIONSHIP,
            scheduleLabel = "Round 3 · Sat 20:00 CET",
            nextStartLabel = "Sat 20:00",
            rounds = 8,
            hasBroadcast = true,
        ),
    )

    override fun nextUp(): RaceEvent = dailyEvents().first { it.startingSoon }
}
