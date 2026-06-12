package com.orioooneee.lmuasister.data.remote

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLQueryComponent
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/** Shared lenient JSON — the APIs carry far more fields than we model. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * lmuportal.com Supabase REST — read with the frontend's public `anon` key.
 * This is the primary source: the full weekly schedule (race_series), the
 * championships (championship_series/_event), plus track + class reference data.
 */
class LmuPortalApi(private val client: HttpClient) {

    suspend fun currentWeekKey(today: String): String? =
        decodeList<RaceSeriesDto>("race_series?select=week_key&week_key=lte.$today&order=week_key.desc&limit=1")
            .firstOrNull()?.weekKey

    /** Distinct week keys from [from] onward (current week + upcoming), sorted ascending. */
    suspend fun weekKeys(from: String): List<String> =
        decodeList<RaceSeriesDto>("race_series?select=week_key&week_key=gte.$from&order=week_key.asc")
            .map { it.weekKey }.distinct()

    suspend fun raceSeries(weekKey: String): List<RaceSeriesDto> =
        decodeList("race_series?select=*&week_key=eq.$weekKey")

    suspend fun activeSeason(): ChampionshipSeasonDto? =
        decodeList<ChampionshipSeasonDto>("championship_season?select=*&status=eq.active&order=current_week.desc&limit=1")
            .firstOrNull()

    suspend fun championshipSeries(seasonId: String): List<ChampionshipSeriesDto> =
        decodeList("championship_series?select=*&season_id=eq.$seasonId")

    suspend fun championshipEvents(): List<ChampionshipEventDto> =
        decodeList("championship_event?select=championship_series_id,round_name,week_number,race_starts_at&order=race_starts_at.asc&limit=1000")

    suspend fun leaderboard(leaderboardId: String, limit: Int = 12): List<LeaderboardEntryDto> =
        decodeList(
            "race_series_leaderboard_entry?leaderboard_id=eq.${leaderboardId.encodeURLQueryComponent()}" +
                "&select=rank,display_name_public,display_initials,score_ms,metadata&order=rank.asc&limit=$limit",
        )

    suspend fun tracks(): List<PortalTrackDto> = decodeList("tracks?select=*")

    suspend fun carClasses(): List<PortalCarClassDto> = decodeList("car_class?select=*")

    private suspend inline fun <reified T> decodeList(path: String): List<T> {
        val text = client.get("$BASE/$path") {
            header("apikey", ANON_KEY)
            header("Authorization", "Bearer $ANON_KEY")
        }.bodyAsText()
        return AppJson.decodeFromString(text)
    }

    companion object {
        private const val BASE = "https://db.lmuportal.space/rest/v1"
        // Public Supabase anon key embedded in the lmuportal web frontend.
        private const val ANON_KEY =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdXBhYmFzZSIsImlhdCI6MTc3Njg3MDc4MCwiZXhwIjo0OTMyNTQ0MzgwLCJyb2xlIjoiYW5vbiJ9.aE59DqdkNMhjsD82MmAc2AYnwp2KENVjzoHTyWmUx3Y"
    }
}

/**
 * Official LMU UI card images on the public `rf2-ui-images-prod` S3 bucket
 * (the same files racecontrol.gg embeds). The bucket is list-enabled, so we pull
 * the current-year `card/` index and match by track/class token in the filename
 * — covers daily, weekly, special and championship rounds.
 *
 * Filenames look like: `20260609_LMU_LMP2LMP3_PaulRicard.webp`.
 */
class LmuCardImageApi(private val client: HttpClient) {
    suspend fun fetchCards(): List<String> {
        val year = Clock.System.now().toLocalDateTime(TimeZone.UTC).year
        // current-year official cards (track-named) + the G Challenge promo cards
        val lmu = list("card/$year").filter { it.contains("_LMU_", ignoreCase = true) }
        val promo = list("card/LMGC")
        return (lmu + promo).distinct().map { "$BUCKET/$it" }
    }

    private suspend fun list(prefix: String): List<String> {
        val xml = client.get("$BUCKET/?list-type=2&prefix=$prefix&max-keys=1000").bodyAsText()
        return KEY_REGEX.findAll(xml).map { it.groupValues[1] }.toList()
    }

    /**
     * racecontrol.gg's editorial cover per event, keyed by the exact race title
     * (e.g. "LMGT3 Fixed" -> a specific Le Mans Night card). These take priority
     * over filename matching so our covers match what racecontrol shows.
     */
    suspend fun fetchRaceControlCovers(): Map<String, String> {
        val html = client.get("https://www.racecontrol.gg/") {
            header("User-Agent", "Mozilla/5.0 (compatible; LmuAssister/1.0)")
        }.bodyAsText()
        val doc = Ksoup.parse(html)
        val out = LinkedHashMap<String, String>()
        doc.select(".glide__slide").forEach { slide ->
            val title = slide.selectFirst(".race-info h4")?.text()?.trim().orEmpty()
            val img = slide.select(".event-image img")
                .firstOrNull { it.hasAttr("src") }?.attr("src").orEmpty()
            if (title.isNotEmpty() && img.startsWith("http")) out.putIfAbsent(title, img)
        }
        return out
    }

    private companion object {
        const val BUCKET = "https://rf2-ui-images-prod.s3.eu-west-1.amazonaws.com"
        val KEY_REGEX = Regex("<Key>(card/[^<]+)</Key>")
    }
}
