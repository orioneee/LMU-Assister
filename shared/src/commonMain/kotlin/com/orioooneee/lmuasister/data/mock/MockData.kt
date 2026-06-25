package com.orioooneee.lmuasister.data.mock

import com.orioooneee.lmuasister.data.remote.AppJson
import com.orioooneee.lmuasister.data.remote.CarClassCountDto
import com.orioooneee.lmuasister.data.remote.CarDto
import com.orioooneee.lmuasister.data.remote.CarsByClassDto
import com.orioooneee.lmuasister.data.remote.CarsResponse
import com.orioooneee.lmuasister.data.remote.ClassInfoDto
import com.orioooneee.lmuasister.data.remote.ClassLeaderboardDto
import com.orioooneee.lmuasister.data.remote.ClassificationRowDto
import com.orioooneee.lmuasister.data.remote.FavoriteCarClassInfoDto
import com.orioooneee.lmuasister.data.remote.FavoriteCarDto
import com.orioooneee.lmuasister.data.remote.GameVersionDto
import com.orioooneee.lmuasister.data.remote.HotlapDto
import com.orioooneee.lmuasister.data.remote.HotlapsResponse
import com.orioooneee.lmuasister.data.remote.LapDto
import com.orioooneee.lmuasister.data.remote.ReasonDto
import com.orioooneee.lmuasister.data.remote.LeaderboardEntryDto
import com.orioooneee.lmuasister.data.remote.LeaderboardPageResponse
import com.orioooneee.lmuasister.data.remote.LeaderboardsDto
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.RaceDetailResponse
import com.orioooneee.lmuasister.data.remote.RaceDto
import com.orioooneee.lmuasister.data.remote.RaceSessionDetailDto
import com.orioooneee.lmuasister.data.remote.RacesPageDto
import com.orioooneee.lmuasister.data.remote.RatingDto
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
import com.orioooneee.lmuasister.data.remote.SessionSummaryDto
import com.orioooneee.lmuasister.data.remote.SessionWeatherDto
import com.orioooneee.lmuasister.data.remote.SettingsDto
import com.orioooneee.lmuasister.data.remote.ProfileStatsDto
import com.orioooneee.lmuasister.data.remote.ProfileJson
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.data.remote.StatTotalsDto
import com.orioooneee.lmuasister.data.remote.SuspensionDto
import com.orioooneee.lmuasister.data.remote.TrackAttemptDto
import com.orioooneee.lmuasister.data.remote.TrackAssetsDto
import com.orioooneee.lmuasister.data.remote.TrackDetailResponse
import com.orioooneee.lmuasister.data.remote.TrackDto
import com.orioooneee.lmuasister.data.remote.TrackFullDto
import com.orioooneee.lmuasister.data.remote.TrackPersonalDto
import com.orioooneee.lmuasister.data.remote.TracksResponse
import com.orioooneee.lmuasister.data.remote.WeatherDto
import com.orioooneee.lmuasister.data.remote.WeatherSegmentDto
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.encodeToString

/**
 * Deterministic fake backend payloads for offline / third-party development.
 *
 * Everything is derived from a fixed [SEED] (and per-entity sub-seeds), so a given
 * race/leaderboard/profile always renders the same data across runs. Only the race
 * *times* are anchored to the wall clock, so countdowns stay live.
 *
 * Each public function returns an already-encoded JSON string (snake_case, via
 * [AppJson]) — exactly what the real HTTP body would be — so the API/repository
 * layer decodes it with zero awareness that it came from a mock.
 */
internal object MockData {

    private const val SEED = 13_37L
    private val currentGameVersion = GameVersionDto(
        version = "1.3.3.4",
        patch = "1.3",
        buildId = "23771736",
        publishedAt = "2026-06-17T12:42:11Z",
        title = "V1.3.3.4 - Update 3, Patch 3, Hotfix 4 - 17th June",
        url = "https://steamstore-a.akamaihd.net/news/externalpost/mock",
        source = "steam_news",
    )

    /** Stable RNG keyed by the given parts — same key ⇒ same sequence, every run. */
    private fun rng(vararg keys: Any): Random =
        Random(keys.fold(SEED) { acc, k -> acc * 31 + k.hashCode() })

    // ───────────────────────── reference data ─────────────────────────

    private data class Trk(
        val name: String, val short: String, val simple: String,
        val town: String, val country: String, val cc: String,
        val lengthKm: String, val turns: Int,
    )

    private val tracks = listOf(
        Trk("Circuit de Spa-Francorchamps", "Spa", "Spa", "Stavelot", "Belgium", "be", "7.004", 20),
        Trk("Circuit de la Sarthe", "Le Mans", "Le Mans", "Le Mans", "France", "fr", "13.626", 38),
        Trk("Bahrain International Circuit", "Bahrain", "Bahrain", "Sakhir", "Bahrain", "bh", "5.412", 15),
        Trk("Sebring International Raceway", "Sebring", "Sebring", "Sebring", "United States", "us", "6.019", 17),
        Trk("Autodromo Nazionale Monza", "Monza", "Monza", "Monza", "Italy", "it", "5.793", 11),
        Trk("Autodromo Enzo e Dino Ferrari", "Imola", "Imola", "Imola", "Italy", "it", "4.909", 19),
    )

    private val carsByClass = linkedMapOf(
        "Hypercar" to listOf(
            "Ferrari 499P", "Toyota GR010 Hybrid", "Porsche 963",
            "Cadillac V-Series.R", "BMW M Hybrid V8", "Peugeot 9X8",
        ),
        "LMP2" to listOf("Oreca 07 Gibson"),
        "LMP3" to listOf("Ligier JS P325", "Duqueine D09"),
        "LMGT3" to listOf(
            "Ferrari 296 LMGT3", "McLaren 720S LMGT3 Evo", "Porsche 911 GT3 R",
            "BMW M4 LMGT3", "Aston Martin Vantage AMR LMGT3", "Corvette Z06 GT3.R",
            "Ford Mustang LMGT3", "Lamborghini Huracán LMGT3 Evo2",
        ),
    )

    /** Per-class reference lap (ms) — overall pace ordering falls out of this. */
    private val classPaceMs = mapOf(
        "Hypercar" to 122_000L, "LMP2" to 128_000L, "LMP3" to 134_000L, "LMGT3" to 137_000L,
    )

    private data class Drv(val name: String, val cc: String, val initials: String)

    private val drivers = listOf(
        Drv("Max Verstappen", "nl", "VER"), Drv("Lewis Hamilton", "gb", "HAM"),
        Drv("Charles Leclerc", "mc", "LEC"), Drv("Lando Norris", "gb", "NOR"),
        Drv("Sergio Perez", "mx", "PER"), Drv("Carlos Sainz", "es", "SAI"),
        Drv("George Russell", "gb", "RUS"), Drv("Fernando Alonso", "es", "ALO"),
        Drv("Maciej Malinowski", "pl", "MAL"), Drv("Dominik Blajer", "pl", "BLA"),
        Drv("Marin Bessiere", "fr", "BES"), Drv("Vincent Mt07", "fr", "VMT"),
        Drv("Smajl Hasandic", "ba", "HAS"), Drv("Anthony Cecilio", "fr", "CEC"),
        Drv("Maksim Yosha", "by", "YOS"), Drv("Chance Asher", "us", "ASH"),
        Drv("Morgan 23", "it", "MOR"), Drv("Luca Engstler", "de", "ENG"),
        Drv("Niklas Krutten", "de", "KRU"), Drv("Daniel Juncadella", "es", "JUN"),
        Drv("Augusto Farfus", "br", "FAR"), Drv("Kelvin van der Linde", "za", "VDL"),
        Drv("Mirko Bortolotti", "it", "BOR"), Drv("Alessio Rovera", "it", "ROV"),
        Drv("Nicki Thiim", "dk", "THI"), Drv("Ben Keating", "us", "KEA"),
        Drv("Sarah Bovy", "be", "BOV"), Drv("Rahel Frey", "ch", "FRE"),
        Drv("Matt Campbell", "au", "CAM"), Drv("Earl Bamber", "nz", "BAM"),
    )

    private const val PLAYER_NAME = "Danylo Perepeliuk"
    private const val PLAYER_CC = "ua"
    private const val PLAYER_UID = "mock-uid-0397"

    // ───────────────────────── schedule ─────────────────────────

    private data class Spec(
        val id: String, val type: String, val series: String, val title: String,
        val track: Int, val classes: List<String>, val difficulty: String, val lenMin: Int,
    )

    private fun weekSpecs(prefix: String) = listOf(
        Spec("$prefix-lmp3-bahrain", "Daily", "LMP3 Fixed", "LMP3 Fixed", 2, listOf("LMP3"), "Bronze", 32),
        Spec("$prefix-mclaren-q4", "Daily", "Logitech McLaren G Challenge", "Logitech McLaren G Challenge Q4", 1, listOf("LMGT3"), "Bronze", 30),
        Spec("$prefix-lmgt3-spa", "Daily", "LMGT3 Fixed", "LMGT3 Fixed", 0, listOf("LMGT3"), "Bronze", 32),
        Spec("$prefix-elms-sebring", "Daily", "ELMS Sprint Trophy", "ELMS Sprint Trophy", 3, listOf("LMP2", "LMP3", "LMGT3"), "Silver", 42),
        Spec("$prefix-wec-lemans", "Weekly", "WEC Endurance", "WEC Endurance — 4 Hours", 1, listOf("Hypercar", "LMP2", "LMGT3"), "Gold", 60),
        Spec("$prefix-hyper-monza", "Weekly", "Hypercar Challenge", "Hypercar Challenge", 4, listOf("Hypercar"), "Silver", 45),
        Spec("$prefix-special-lemans", "Special", "Le Mans 24h Qualifier", "Le Mans 24h Qualifier", 1, listOf("Hypercar", "LMGT3"), "Gold", 50),
        Spec("$prefix-champ-imola", "Championship", "Spring Championship", "Spring Championship — Round 3", 5, listOf("LMGT3"), "Silver", 40),
    )

    private val weekOrder = listOf("week-current" to "This week", "week-next" to "Next week")

    /** All races, indexed by id — single source so /schedule and /race/<id> agree. */
    private val raceIndex: Map<String, RaceDto> by lazy {
        buildMap {
            weekOrder.forEachIndexed { wi, (key, _) ->
                weekSpecs(key).forEachIndexed { ri, spec -> put(spec.id, buildRace(spec, wi, ri)) }
            }
        }
    }

    private val scheduleJson: String by lazy {
        val schedules = weekOrder.associate { (key, _) ->
            key to weekSpecs(key).map { raceIndex.getValue(it.id) }
        }
        val weeks = weekOrder.map { (k, l) -> com.orioooneee.lmuasister.data.remote.WeekDto(k, l) }
        AppJson.encodeToString(
            com.orioooneee.lmuasister.data.remote.ScheduleResponse(weeks = weeks, schedules = schedules),
        )
    }

    fun schedule(): String = scheduleJson

    private fun buildRace(spec: Spec, weekIdx: Int, raceIdx: Int): RaceDto {
        val t = tracks[spec.track]
        // First few races of the current week start soon (live countdowns); later ones space out.
        val startOffsetMin = weekIdx * 7 * 24 * 60 + raceIdx * 7 + 4
        return RaceDto(
            id = spec.id,
            type = spec.type,
            series = spec.series,
            circuit = t.simple,
            difficulty = spec.difficulty,
            carClasses = spec.classes,
            classInfos = spec.classes.map { ClassInfoDto(id = it, name = it) },
            carsByClass = spec.classes.map { cls ->
                CarsByClassDto(carClass = cls, cars = carsByClass.getValue(cls))
            },
            times = slotTimes(startOffsetMin, count = 14, stepMin = 45),
            raceLength = spec.lenMin,
            settings = sampleSettings(spec),
            track = trackDto(t),
            cover = null, // no cover server in mock — UI falls back to its placeholder
            weather = sampleWeather(spec.id),
            leaderboardId = "lb-${spec.id}-overall",
            completed = false,
        )
    }

    private fun trackDto(t: Trk) = TrackDto(
        name = t.name, shortName = t.short, simpleName = t.simple, town = t.town,
        country = t.country, lengthKm = t.lengthKm, numTurns = t.turns, countryCode = t.cc,
    )

    private fun sampleSettings(spec: Spec) = SettingsDto(
        setup = "Fixed", assists = "Limited", damage = "Realistic",
        tireWear = "Realistic", fuelUsage = "Realistic",
        safetyRank = spec.difficulty, driverRank = "Open",
        splitSize = 32, qualifyingLength = 15, practiceLength = 20,
        driverSwaps = false, trackLimits = "Strict", tireWarmers = "On", limitedTires = "No",
    )

    private fun sampleWeather(id: String): WeatherDto {
        val r = rng("weather", id)
        fun segs() = listOf(
            WeatherSegmentDto(sky = r.nextInt(0, 3), tempC = r.nextInt(16, 29), humidity = r.nextInt(35, 75),
                windKmh = r.nextInt(2, 18), rainChance = r.nextInt(0, 30), durationMin = 30),
            WeatherSegmentDto(sky = r.nextInt(1, 5), tempC = r.nextInt(14, 27), humidity = r.nextInt(40, 85),
                windKmh = r.nextInt(3, 22), rainChance = r.nextInt(0, 60), durationMin = 30),
        )
        return WeatherDto(
            qualifying = SessionWeatherDto(timeOfDay = "Afternoon", segments = segs()),
            race = SessionWeatherDto(timeOfDay = "Evening", segments = segs()),
        )
    }

    private fun slotTimes(offsetMin: Int, count: Int, stepMin: Int): List<String> {
        val nowS = Clock.System.now().epochSeconds
        val base = ((nowS / 1800) + 1) * 1800 // next half-hour boundary
        return (0..count).map { i ->
            Instant.fromEpochSeconds(base + offsetMin.toLong() * 60 + i.toLong() * stepMin * 60).toString()
        }
    }

    // ───────────────────────── race detail / leaderboards ─────────────────────────

    fun raceDetail(raceId: String): String {
        val race = raceIndex[raceId]
        val overall = classBoard(raceId, "overall", race?.carClasses ?: listOf("LMGT3"), size = 8)
        val byClass = (race?.carClasses ?: listOf("LMGT3")).map { cls ->
            classBoard(raceId, cls, listOf(cls), size = 6)
        }
        return AppJson.encodeToString(
            RaceDetailResponse(
                race = race,
                leaderboards = LeaderboardsDto(overall = overall, byClass = byClass),
            ),
        )
    }

    /** A board for one class (or "overall" mixing all classes in the race). */
    private fun classBoard(raceId: String, slug: String, classes: List<String>, size: Int): ClassLeaderboardDto {
        val r = rng("board", raceId, slug)
        val entries = (1..size).map { rank ->
            val cls = classes[(rank - 1) % classes.size]
            entry(r, rank, cls)
        }.sortedBy { it.bestLapMs }.mapIndexed { i, e -> e.copy(rank = i + 1) }
        return ClassLeaderboardDto(
            classId = slug.takeIf { it != "overall" },
            carClass = slug.takeIf { it != "overall" },
            leaderboardId = "lb-$raceId-$slug",
            entries = entries,
        )
    }

    private fun entry(r: Random, rank: Int, cls: String): LeaderboardEntryDto {
        val d = drivers[r.nextInt(drivers.size)]
        val base = classPaceMs[cls] ?: 135_000L
        val lap = base + rank * (90L + r.nextInt(80)) + r.nextInt(250)
        val s1 = lap * 0.34
        val s2 = lap * 0.37
        return LeaderboardEntryDto(
            rank = rank,
            initials = d.initials,
            bestLapMs = lap,
            sectors = listOf(s1, s2, lap - s1 - s2),
            car = carsByClass.getValue(cls).let { it[r.nextInt(it.size)] },
            carClass = cls,
            drRank = listOf("B3", "S2", "S1", "G3", "B4").random(r),
            srRank = listOf("B", "S", "G", "P").random(r),
        )
    }

    fun hotlaps(raceId: String): String {
        val race = raceIndex[raceId]
        val cls = race?.carClasses?.lastOrNull() ?: "LMGT3"
        val r = rng("hotlaps", raceId)
        val cars = carsByClass.getValue(cls)
        val laps = (0 until 6).map { i ->
            val car = cars[i % cars.size]
            val lap = (classPaceMs[cls] ?: 135_000L) + i * 600L + r.nextInt(400)
            HotlapDto(
                title = "${race?.circuit ?: "Spa"} — $car",
                videoId = "mock${raceId.hashCode().toUInt().toString(16)}$i",
                url = "",
                thumbnail = "",
                author = listOf("Hymo Hotlaps", "GO Setups", "LMU Daily").random(r),
                driver = drivers[r.nextInt(drivers.size)].name,
                lapTime = formatLap(lap),
                car = car,
                carClass = cls,
                classBadge = cls,
                gameVersion = "1.3.${r.nextInt(1, 4)}",
                views = r.nextLong(500, 40_000),
            )
        }
        return AppJson.encodeToString(HotlapsResponse(status = "ready", hotlaps = laps))
    }

    fun leaderboardPage(leaderboardId: String, cursor: String?, limit: Int, withMe: Boolean): String {
        val total = 40 + (rng("lbtotal", leaderboardId).nextInt(40))
        val cls = leaderboardId.substringAfterLast('-').takeIf { it in classPaceMs } ?: "LMGT3"
        val offset = cursor?.toIntOrNull() ?: 0
        val r = rng("lbpage", leaderboardId, offset)
        val end = minOf(offset + limit, total)
        val entries = (offset until end).map { i -> entry(r, i + 1, cls).copy(rank = i + 1) }
        val next = if (end < total) end.toString() else null
        val me = if (withMe) LeaderboardEntryDto(
            rank = 12, initials = "PER", bestLapMs = (classPaceMs[cls] ?: 137_000L) + 7_227,
            sectors = emptyList(), car = "McLaren 720S LMGT3 Evo", carClass = cls, drRank = "B3", srRank = "S",
            // P12 of `total` → faster than the rest of the field.
            fasterThanPct = ((total - 12).toDouble() / total * 1000).toInt() / 10.0, rankUnstable = false,
        ) else null
        return AppJson.encodeToString(
            LeaderboardPageResponse(
                leaderboardId = leaderboardId, total = total, nextCursor = next, entries = entries, me = me,
            ),
        )
    }

    // ───────────────────────── profile ─────────────────────────

    fun profile(): String = ProfileJson.encodeToString(
        SteamProfile(
            uid = PLAYER_UID,
            name = PLAYER_NAME,
            displayName = PLAYER_NAME,
            nationality = PLAYER_CC,
            badge = "Sr Probation",
            badges = listOf("Sr Probation"),
            driverRating = RatingDto(rank = "Bronze", tier = 3, progress = 0.37, rating = 1240.0),
            safetyRating = RatingDto(rank = "Silver", tier = 2, progress = 0.66, rating = 88.0),
            activeSuspensions = 1,
            totalSuspensions = 4,
            suspensions = suspensions(),
            currentGameVersion = currentGameVersion,
            recentRaces = recentRaces().take(3),
            stats = ProfileStatsDto(
                total = StatTotalsDto(
                    races = 128,
                    wins = 14,
                    podiums = 41,
                    top5 = 67,
                    dnfs = 9,
                    polePositions = 9,
                    lapsCompleted = 1_842,
                    lapsLead = 155,
                    fastestLaps = 11,
                    grandSlams = 2,
                    polesConverted = 5,
                    winsNoPole = 9,
                ),
            ),
            favoriteCars = favoriteCars(),
        ),
    )

    fun stats(): String = AppJson.encodeToString(
        mapOf("races" to "128", "wins" to "14", "podiums" to "41", "poles" to "9"),
    )

    private fun suspensions(): List<SuspensionDto> {
        val day = 86_400_000L
        val now = Clock.System.now().toEpochMilliseconds()
        return listOf(
            SuspensionDto(type = 2, reason = "SR ratio below threshold — automatic probation.",
                from = now - 2 * day, to = now + 5 * day, permanent = false, active = true),
            SuspensionDto(type = 1, reason = "Causing a collision (T1, lap 1).",
                from = now - 40 * day, to = now - 38 * day, active = false),
            SuspensionDto(type = 1, reason = null, redacted = true,
                from = now - 90 * day, to = now - 89 * day, active = false),
            SuspensionDto(type = 3, reason = "Repeated track-limit abuse.",
                from = now - 160 * day, to = now - 159 * day, active = false),
        )
    }

    private fun favoriteCars(): List<FavoriteCarDto> = listOf(
        FavoriteCarDto(
            carId = "44_24_PROT92DCFAD4",
            car = "Ford Mustang LMGT3",
            carName = "Ford Mustang LMGT3",
            model = "Mustang LMGT3",
            manufacturer = "Ford",
            manufacturerLogoUrl = null,
            carImageUrl = "https://placehold.co/260x150/1B1E25/F2F4F8.png?text=Ford+Mustang",
            carClass = "GT3",
            classInfo = FavoriteCarClassInfoDto(id = "GT3", name = "GT3", colorHex = "#f59e0b"),
            classColorHex = "#f59e0b",
            races = 18,
            distanceKm = 1_284.6,
            wins = 3,
            poles = 2,
            podiums = 8,
        ),
        FavoriteCarDto(
            carId = "59_24_PROT72SEVO",
            car = "McLaren 720S LMGT3 Evo",
            carName = "McLaren 720S LMGT3 Evo",
            model = "720S LMGT3 Evo",
            manufacturer = "McLaren",
            manufacturerLogoUrl = null,
            carImageUrl = "https://placehold.co/260x150/1B1E25/F2F4F8.png?text=McLaren+720S",
            carClass = "LMGT3",
            classInfo = FavoriteCarClassInfoDto(id = "LMGT3", name = "LMGT3", colorHex = "#16A34A"),
            classColorHex = "#16A34A",
            races = 15,
            distanceKm = 936.2,
            wins = 2,
            poles = 1,
            podiums = 6,
        ),
        FavoriteCarDto(
            carId = "6_23_HYP499P",
            car = "Ferrari 499P",
            carName = "Ferrari 499P",
            model = "499P",
            manufacturer = "Ferrari",
            manufacturerLogoUrl = null,
            carImageUrl = "https://placehold.co/260x150/1B1E25/F2F4F8.png?text=Ferrari+499P",
            carClass = "Hypercar",
            classInfo = FavoriteCarClassInfoDto(id = "Hypercar", name = "Hypercar", colorHex = "#e2231a"),
            classColorHex = "#e2231a",
            races = 11,
            distanceKm = 701.8,
            wins = 1,
            poles = 0,
            podiums = 4,
        ),
    )

    private data class RaceCard(
        val title: String, val track: Int, val cls: String, val car: String,
        val type: String, val tier: String, val field: Int, val grid: Int, val finish: Int,
        val split: Int, val splits: Int, val sr: Double, val dr: Double, val dnf: Boolean,
    )

    private val allCards = listOf(
        RaceCard("Logitech McLaren G Challenge", 1, "LMGT3", "McLaren 720S LMGT3 Evo", "Daily", "Bronze", 19, 4, 12, 29, 30, -14.0, -1.2, false),
        RaceCard("Logitech McLaren G Challenge", 1, "LMGT3", "McLaren 720S LMGT3 Evo", "Daily", "Bronze", 19, 12, 4, 31, 32, 14.1, 4.3, false),
        RaceCard("ELMS Sprint Trophy", 3, "LMGT3", "Ford Mustang LMGT3", "Daily", "Silver", 24, 8, 24, 3, 3, -32.9, -9.9, true),
        RaceCard("LMGT3 Fixed", 0, "LMGT3", "Ferrari 296 LMGT3", "Daily", "Bronze", 22, 6, 3, 12, 14, 9.4, 3.1, false),
        RaceCard("Hypercar Challenge", 4, "Hypercar", "Ferrari 499P", "Weekly", "Silver", 28, 9, 7, 5, 6, 4.2, 1.8, false),
        RaceCard("WEC Endurance — 4 Hours", 1, "Hypercar", "Toyota GR010 Hybrid", "Weekly", "Gold", 30, 14, 6, 1, 1, 6.0, 2.5, false),
        RaceCard("LMP3 Fixed", 2, "LMP3", "Ligier JS P325", "Daily", "Bronze", 20, 3, 2, 8, 9, 7.7, 2.0, false),
        RaceCard("Spring Championship — Round 3", 5, "LMGT3", "BMW M4 LMGT3", "Championship", "Silver", 26, 7, 5, 2, 2, 3.3, 1.1, false),
        RaceCard("Le Mans 24h Qualifier", 1, "LMGT3", "Aston Martin Vantage AMR LMGT3", "Special", "Gold", 32, 18, 11, 4, 4, -2.1, 0.4, false),
        RaceCard("LMGT3 Fixed", 0, "LMGT3", "Porsche 911 GT3 R", "Daily", "Bronze", 21, 5, 9, 7, 8, -3.0, -0.8, false),
        RaceCard("Hypercar Challenge", 4, "Hypercar", "Porsche 963", "Weekly", "Silver", 27, 11, 8, 6, 6, 2.0, 0.9, false),
        RaceCard("ELMS Sprint Trophy", 3, "LMP2", "Oreca 07 Gibson", "Daily", "Silver", 23, 4, 4, 2, 3, 5.5, 1.6, false),
    )

    private fun RaceCard.toRecent(i: Int): RecentRaceDto {
        val t = tracks[track]
        val now = Clock.System.now().toEpochMilliseconds()
        return RecentRaceDto(
            date = Instant.fromEpochMilliseconds(now - (i + 1) * 86_400_000L / 2).toString(),
            title = title,
            eventId = "ev-${title.lowercase().filter { it.isLetterOrDigit() }.take(10)}-$i",
            track = t.simple,
            tier = tier.lowercase(),
            eventType = type.lowercase(),
            official = true,
            split = split,
            totalSplits = splits,
            fieldSize = field,
            classFieldSize = field,   // mock races are single-class, so class == overall
            position = finish,
            classPosition = finish,
            gridPosition = grid,
            carClass = cls,
            car = car,
            carName = car,
            bestLapMs = (classPaceMs[cls] ?: 137_000L) + 4_227 + i * 30,
            finishStatus = if (dnf) "DNF" else "Finished",
            srChange = sr,
            drChange = dr,
            gameVersion = currentGameVersion,
        )
    }

    private fun recentRaces(): List<RecentRaceDto> = allCards.mapIndexed { i, c -> c.toRecent(i) }

    fun racesPage(page: Int): String {
        val pageSize = 5
        val all = recentRaces()
        val from = (page - 1).coerceAtLeast(0) * pageSize
        val slice = all.drop(from).take(pageSize)
        return AppJson.encodeToString(
            RacesPageDto(
                page = page, pageSize = pageSize, count = all.size,
                hasMore = from + pageSize < all.size, races = slice,
            ),
        )
    }

    /** GET /profile/races/<category>?page=N — filter the history by the stat the user tapped. */
    fun categoryRacesPage(category: String, page: Int): String {
        val pageSize = 30
        val all = recentRaces()
        val filtered = when (category) {
            "wins" -> all.filter { it.position == 1 }
            "podiums" -> all.filter { it.position in 1..3 }
            "poles" -> all.filter { it.gridPosition == 1 }
            "top5" -> all.filter { it.position in 1..5 }
            "fastest_laps" -> all.filterIndexed { i, _ -> i % 2 == 0 } // no FL flag in mock
            else -> all
        }
        val from = (page - 1).coerceAtLeast(0) * pageSize
        val slice = filtered.drop(from).take(pageSize)
        return AppJson.encodeToString(
            RacesPageDto(
                page = page, pageSize = pageSize, count = slice.size,
                total = filtered.size, category = category,
                hasMore = from + pageSize < filtered.size, races = slice,
            ),
        )
    }

    fun profileRaceDetail(eventId: String): String {
        // Reuse a card so the detail agrees with the list; default to the McLaren P12→P4 race.
        val card = recentRaces().firstOrNull { it.eventId == eventId } ?: recentRaces()[1]
        val t = tracks.firstOrNull { it.simple == card.track } ?: tracks[1]
        fun summary(pos: Int, grid: Int, lapMs: Long) =
            SessionSummaryDto(position = pos, classPosition = pos, gridPosition = grid, bestLapMs = lapMs)
        val detail = RaceDetailDto(
            date = card.date,
            title = card.title,
            eventId = eventId,
            car = card.car,
            carName = card.carName,
            carClass = card.carClass,
            track = t.simple,
            trackInfo = trackDto(t),
            tier = card.tier,
            eventType = card.eventType,
            official = true,
            split = card.split,
            totalSplits = card.totalSplits,
            fieldSize = card.fieldSize,
            position = card.position,
            gridPosition = card.gridPosition,
            bestLapMs = card.bestLapMs,
            finishStatus = card.finishStatus,
            srChange = card.srChange,
            drChange = card.drChange,
            gameVersion = card.gameVersion,
            // impact is a 1–4 weight (rendered as arrows), not a real point value.
            srReasons = listOf(
                ReasonDto(impact = 2.0, positive = true, reason = "Clean racing — no incidents"),
                ReasonDto(impact = 3.0, positive = false, reason = "Contact with another car (T6)"),
                ReasonDto(impact = 1.0, positive = false, reason = "Off track (T10)"),
            ),
            drReasons = listOf(
                ReasonDto(impact = 4.0, positive = (card.drChange ?: 0.0) >= 0.0,
                    reason = "Finished P${card.position} from P${card.gridPosition ?: card.position}"),
                ReasonDto(impact = 1.0, positive = true, reason = "Beat higher-rated drivers"),
            ),
            lapProgress = run {
                val base = card.bestLapMs ?: 137_000L
                (1..12).map { n ->
                    val pit = n == 6
                    val ms = when {
                        n == 1 -> base + 3_500L     // opening lap is slower
                        pit -> base + 21_000L       // in/out lap around the stop
                        n == 3 -> base              // the best lap of the run
                        else -> base + 400L + n * 35L
                    }
                    LapDto(lap = n, position = card.position, classPosition = card.classPosition, lapTimeMs = ms, sectorsMs = sectors(ms), pit = pit)
                }
            },
            sessions = mapOf(
                "qualifying" to RaceSessionDetailDto(
                    me = summary(card.gridPosition ?: 12, card.gridPosition ?: 12, (card.bestLapMs ?: 0)),
                    classification = classification(eventId, "q", card),
                ),
                "race" to RaceSessionDetailDto(
                    me = summary(card.position, card.gridPosition ?: 12, (card.bestLapMs ?: 0) + 1_900),
                    classification = classification(eventId, "r", card),
                ),
            ),
        )
        return AppJson.encodeToString(detail)
    }

    private fun classification(eventId: String, session: String, card: RecentRaceDto): List<ClassificationRowDto> {
        val r = rng("classif", eventId, session)
        val mePos = if (session == "q") (card.gridPosition ?: 12) else card.position
        val field = card.fieldSize.coerceAtLeast(mePos + 1)
        val cls = card.carClass ?: "LMGT3"
        // A window of rows around the player, like the real detail screen.
        val lo = (mePos - 3).coerceAtLeast(1)
        val hi = (lo + 6).coerceAtMost(field)
        val base = classPaceMs[cls] ?: 137_000L
        return (lo..hi).map { pos ->
            val isMe = pos == mePos
            val d = if (isMe) Drv(PLAYER_NAME, PLAYER_CC, "PER") else drivers[r.nextInt(drivers.size)]
            val lap = base + pos * 110L + r.nextInt(200)
            ClassificationRowDto(
                position = pos,
                classPosition = pos,
                gridPosition = (pos + r.nextInt(-2, 3)).coerceIn(1, field),
                name = d.name,
                nationality = d.cc,
                isMe = isMe,
                car = if (isMe) (card.car ?: carsByClass.getValue(cls).first()) else carsByClass.getValue(cls).random(r),
                carClass = cls,
                bestLapMs = lap,
                bestLapSectorsMs = sectors(lap),
                finishStatus = "Finished",
            )
        }
    }

    /** Split a lap time into three plausible sector times (34% / 33% / remainder). */
    private fun sectors(lapMs: Long): List<Long> {
        val s1 = lapMs * 34 / 100
        val s2 = lapMs * 33 / 100
        return listOf(s1, s2, lapMs - s1 - s2)
    }

    // ───────────────────────── tracks ─────────────────────────

    private fun Trk.idKey() = simple.lowercase().filter { it.isLetterOrDigit() }

    private fun trackFull(t: Trk): TrackFullDto {
        val id = t.idKey()
        return TrackFullDto(
            id = id,
            code = "${t.short}_2023",
            base = t.simple.lowercase(),
            name = t.name,
            eventName = t.simple,
            fullName = t.name,
            countryCode = t.cc.uppercase(),
            lengthKm = t.lengthKm,
            corners = t.turns,
            type = "Permanent",
            official = true,
            assets = TrackAssetsDto(
                scheme = "/api/v2/track/$id/scheme.svg",
                logo = "/api/v2/track/$id/logo.svg",
                cover = "/api/v2/track/$id/cover.webp",
                background = "/api/v2/track/$id/background.webp",
            ),
        )
    }

    fun tracks(): String = AppJson.encodeToString(
        TracksResponse(count = tracks.size, tracks = tracks.map { trackFull(it) }),
    )

    fun trackDetail(rawId: String): String {
        val key = rawId.lowercase().filter { it.isLetterOrDigit() }
        val t = tracks.firstOrNull { it.idKey() == key } ?: tracks[0]
        val now = Clock.System.now().toEpochMilliseconds()
        fun attempt(cls: String, lapMs: Long, daysAgo: Int, session: String, pos: Int, status: String) =
            TrackAttemptDto(
                bestLapMs = lapMs,
                session = session,
                car = carsByClass[cls]?.firstOrNull() ?: "GT Car",
                carClass = cls,
                date = Instant.fromEpochMilliseconds(now - daysAgo * 86_400_000L).toString(),
                eventId = "ev-$key-$daysAgo",
                eventTitle = "$cls Fixed",
                tier = "intermediate",
                official = true,
                split = 4,
                position = pos,
                finishStatus = status,
                gameVersion = currentGameVersion,
            )
        val base = classPaceMs["LMGT3"] ?: 143_000L
        val gt3 = attempt("LMGT3", base + 845, 6, "qualifying", 6, "Finished")
        val lmp2 = attempt("LMP2", base - 14_000, 12, "race", 3, "Finished")
        val personal = TrackPersonalDto(
            races = 29,
            currentPatch = currentGameVersion,
            bestLapEver = lmp2,
            bestLapCurrentPatch = gt3,
            bestLap = lmp2,                                   // absolute best (faster prototype)
            bestByClass = mapOf("LMGT3" to gt3, "LMP2" to lmp2),
            recent = listOf(
                gt3,
                attempt("LMGT3", base + 1_320, 9, "race", 11, "Finished"),
                lmp2,
                attempt("LMGT3", base + 2_010, 20, "race", 18, "DNF"),
            ),
        )
        return AppJson.encodeToString(TrackDetailResponse(track = trackFull(t), personal = personal))
    }

    // ───────────────────────── cars ─────────────────────────

    fun cars(): String {
        val list = carsByClass.entries.flatMap { (cls, models) ->
            models.map { m ->
                val manufacturer = m.substringBefore(' ')
                CarDto(
                    id = "car-${cls.lowercase()}-${m.lowercase().filter { it.isLetterOrDigit() }}",
                    name = m, manufacturer = manufacturer, model = m, carClass = cls,
                    series = if (cls == "Hypercar" || cls == "LMP2") "WEC" else "LMU",
                )
            }
        }
        val counts = carsByClass.map { (cls, models) -> CarClassCountDto(cls, models.size) }
        return AppJson.encodeToString(CarsResponse(count = list.size, classes = counts, cars = list))
    }

    // ───────────────────────── misc ─────────────────────────

    fun privacy(): String = """
        # Privacy Policy (mock)

        This is a development build running on bundled mock data. No backend is
        contacted, no Steam credentials are used, and nothing is sent anywhere.

        All drivers, lap times, ratings and races shown in the app are randomly
        generated from a fixed seed and are entirely fictitious.
    """.trimIndent()

    /** Simulated network latency per endpoint family — keeps the loaders visible. */
    fun latencyFor(path: String): Long = when {
        path == "/schedule" -> 700
        path.endsWith("/hotlaps") -> 1_200
        path.startsWith("/leaderboard/") -> 450
        path.startsWith("/race/") -> 550
        path == "/profile" -> 650
        path.startsWith("/profile/") -> 500
        path == "/cars" -> 400
        else -> 300
    }

    private fun formatLap(ms: Long): String {
        val totalMs = ms % 60_000
        val minutes = ms / 60_000
        val seconds = totalMs / 1000
        val millis = totalMs % 1000
        return "$minutes:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}"
    }
}
