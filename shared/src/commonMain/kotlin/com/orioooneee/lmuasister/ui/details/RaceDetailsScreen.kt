package com.orioooneee.lmuasister.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.CarGroup
import com.orioooneee.lmuasister.data.model.AvailableCar
import com.orioooneee.lmuasister.data.model.ClassLeaderboard
import com.orioooneee.lmuasister.data.model.Hotlap
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.data.model.RaceLeaderboards
import com.orioooneee.lmuasister.data.model.RaceSettings
import com.orioooneee.lmuasister.data.model.RaceWeather
import com.orioooneee.lmuasister.data.model.SessionWeather
import com.orioooneee.lmuasister.data.model.TrackInfo
import com.orioooneee.lmuasister.data.model.TopCar
import com.orioooneee.lmuasister.data.model.TopCarsResult
import com.orioooneee.lmuasister.ui.IconBolt
import com.orioooneee.lmuasister.ui.profile.stripCarClass
import com.orioooneee.lmuasister.ui.tracks.TrackPreview
import com.orioooneee.lmuasister.ui.components.ClassChip
import com.orioooneee.lmuasister.ui.components.carClassColor
import com.orioooneee.lmuasister.ui.components.CoverImage
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.SrBadge
import com.orioooneee.lmuasister.ui.components.TimesGrid
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.components.accentColor
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassGte
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassLmp3
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.util.formatLap
import com.orioooneee.lmuasister.ui.util.formatSector
import com.orioooneee.lmuasister.ui.util.rememberNow
import com.orioooneee.lmuasister.ui.util.skyColor
import com.orioooneee.lmuasister.ui.util.skyEmoji
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.cars_section
import lmuassister.shared.generated.resources.duration_race
import lmuassister.shared.generated.resources.fastest_laps
import lmuassister.shared.generated.resources.format
import lmuassister.shared.generated.resources.hotlaps
import lmuassister.shared.generated.resources.length_km
import lmuassister.shared.generated.resources.next_start_times
import lmuassister.shared.generated.resources.no
import lmuassister.shared.generated.resources.no_lap_times
import lmuassister.shared.generated.resources.session_practice
import lmuassister.shared.generated.resources.session_qualifying
import lmuassister.shared.generated.resources.session_race
import lmuassister.shared.generated.resources.set_assists
import lmuassister.shared.generated.resources.set_damage
import lmuassister.shared.generated.resources.set_driver_rank
import lmuassister.shared.generated.resources.set_driver_swaps
import lmuassister.shared.generated.resources.set_fuel_usage
import lmuassister.shared.generated.resources.set_limited_tires
import lmuassister.shared.generated.resources.set_practice
import lmuassister.shared.generated.resources.set_qualifying
import lmuassister.shared.generated.resources.set_safety_rank
import lmuassister.shared.generated.resources.set_setup
import lmuassister.shared.generated.resources.set_split_size
import lmuassister.shared.generated.resources.set_tire_warmers
import lmuassister.shared.generated.resources.set_tire_wear
import lmuassister.shared.generated.resources.set_track_limits
import lmuassister.shared.generated.resources.full_leaderboard
import lmuassister.shared.generated.resources.set_start_interval
import lmuassister.shared.generated.resources.track_city
import lmuassister.shared.generated.resources.track_name
import lmuassister.shared.generated.resources.track_official_name
import lmuassister.shared.generated.resources.track_country
import lmuassister.shared.generated.resources.track_length
import lmuassister.shared.generated.resources.track_turns
import lmuassister.shared.generated.resources.weather
import lmuassister.shared.generated.resources.weather_rain
import lmuassister.shared.generated.resources.yes
import lmuassister.shared.generated.resources.your_position
import lmuassister.shared.generated.resources.your_position_none

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceDetailsScreen(
    race: Race,
    insets: PaddingValues,
    onBack: () -> Unit,
    onOpenLeaderboard: (leaderboardId: String, title: String) -> Unit = { _, _ -> },
) {
    val now = rememberNow()
    val upcoming = remember(race, now) { race.times.filter { it >= now } }

    val repo = koinInject<RaceRepository>()
    // Leaderboard (fast) and hot-laps (async build) load in parallel — each resolves
    // its own skeleton independently. Offline-first: paint cached (memory/disk) instantly,
    // then refresh from the network. null = nothing cached yet → skeleton.
    val leaderboards by produceState<RaceLeaderboards?>(
        if (race.leaderboardId != null) repo.peekLeaderboards(race.id) else RaceLeaderboards.EMPTY,
        race.id,
    ) {
        if (race.leaderboardId == null) {
            value = RaceLeaderboards.EMPTY
            return@produceState
        }
        if (value == null) repo.cachedLeaderboards(race.id)?.let { value = it }
        val fresh = withTimeoutOrNull(LEADERBOARDS_TIMEOUT_MS) { repo.leaderboards(race.id).getOrNull() }
        if (fresh != null) value = fresh else if (value == null) value = RaceLeaderboards.EMPTY
    }
    val hotlaps by produceState<List<Hotlap>?>(repo.peekHotlaps(race.id), race.id) {
        if (value == null) repo.cachedHotlaps(race.id)?.let { value = it }
        val fresh = repo.hotlaps(race.id).getOrNull()
        if (fresh != null) value = fresh else if (value == null) value = emptyList()
    }
    // Exact livery name → physical car model. The schedule still keeps every allowed
    // livery, but the UI collapses identical models and uses the first livery's artwork.
    val liveryToModel by produceState(emptyMap<String, String>(), race.id) {
        value = runCatching { repo.liveryToModel() }.getOrDefault(emptyMap())
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize().background(Carbon),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp + insets.calculateTopPadding(),
            end = 16.dp,
            bottom = 16.dp + insets.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)) {
                CoverImage(
                    url = race.imageUrl,
                    contentDescription = race.title,
                    modifier = Modifier.fillMaxSize().background(Surface2),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Carbon.copy(alpha = 0.35f),
                            0.5f to Color.Transparent,
                            1f to Carbon.copy(alpha = 0.95f),
                        ),
                    ),
                )
                CircleButton(Modifier.align(Alignment.TopStart).padding(12.dp), onBack)
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(race.type.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = race.accentColor())
                    Spacer(Modifier.height(6.dp))
                    Text(race.title, style = MaterialTheme.typography.headlineMedium, color = TextHigh, fontWeight = FontWeight.Black)
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                race.classInfos.take(6).forEach { ClassChip(it) }
                if (race.raceLength > 0) MetaChip(stringResource(Res.string.duration_race, race.raceLength))
                race.settings.safetyRank?.let { SrBadge(it) }
            }
        }

        if (race.availableCars.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AvailableCarsSection(race.availableCars, liveryToModel)
            }
        } else if (race.carsByClass.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { CarsTicker(race.carsByClass) }
        }

        if (race.leaderboardId != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val lbs = leaderboards
                if (lbs == null) LeaderboardSkeletonCard()
                else LeaderboardWithTopCars(
                    lbs = lbs,
                    raceId = race.id,
                    raceClasses = race.carClasses,
                    raceTitle = race.title,
                    onOpenFull = onOpenLeaderboard,
                )
            }
        }
        race.track?.let {
            item {
                TrackCard(
                    it,
                    hotlaps.orEmpty(),
                    hotlapsLoading = hotlaps == null,
                    hotlapsSkeletonCount = (race.carClasses.size * 2).coerceAtLeast(2),
                )
            }
        }
        race.weather?.let { item { WeatherCard(it) } }
        item {
            Card(stringResource(Res.string.format)) { DetailRows(settingRows(race.settings)) }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Card(stringResource(Res.string.next_start_times)) {
                    TimesGrid(race.times)
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: TrackInfo,
    hotlaps: List<Hotlap> = emptyList(),
    hotlapsLoading: Boolean = false,
    hotlapsSkeletonCount: Int = 6,
) {
    val flag = track.countryCode?.let { flagUrlFromCode(it) } ?: track.country?.let { flagUrl(it) }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrackPreview(
                backgroundUrl = track.backgroundUrl, mapUrl = track.mapUrl, logoUrl = track.logoUrl, flagUrl = flag,
                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                height = 170.dp, emblemHeight = 30.dp, flagSize = 24.dp,
            )
            val officialName = track.name.takeIf { it.isNotBlank() }
            val primaryName = track.simpleName?.takeIf { it.isNotBlank() } ?: officialName
            DetailRows(
                listOfNotNull(
                    primaryName?.let { stringResource(Res.string.track_name) to it },
                    officialName?.takeIf { it != primaryName }?.let { stringResource(Res.string.track_official_name) to it },
                    track.country?.let { stringResource(Res.string.track_country) to it },
                    track.town?.let { stringResource(Res.string.track_city) to it },
                    track.lengthKm?.let { stringResource(Res.string.track_length) to stringResource(Res.string.length_km, it.toString()) },
                    track.numTurns?.let { stringResource(Res.string.track_turns) to it.toString() },
                ),
            )
            when {
                hotlapsLoading -> HotlapsSkeleton(hotlapsSkeletonCount)
                hotlaps.isNotEmpty() -> HotlapsFlow(hotlaps)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotlapsFlow(hotlaps: List<Hotlap>) {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(Res.string.hotlaps),
            style = MaterialTheme.typography.labelMedium,
            color = TextMed,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            hotlaps.forEach { h -> HotlapCard(h) { runCatching { uri.openUri(h.url) } } }
        }
    }
}

private val HOTLAP_CARD_W = 152.dp
private val HOTLAP_CARD_H = 198.dp
private val HOTLAP_THUMB_H = 86.dp

@Composable
private fun HotlapCard(h: Hotlap, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .width(HOTLAP_CARD_W)
            .height(HOTLAP_CARD_H)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().height(HOTLAP_THUMB_H).background(Carbon)) {
            CoverImage(url = h.thumbnail, contentDescription = h.title, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Carbon.copy(alpha = 0.55f)),
                ),
            )
            Box(
                Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape).background(Carbon.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(">", style = MaterialTheme.typography.labelMedium, color = TextHigh)
            }
            h.lapTime?.let {
                Box(
                    Modifier.align(Alignment.BottomStart).padding(6.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Carbon.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClassBadgeChip(h.classBadge ?: h.carClass?.uppercase() ?: "-")
                h.gameVersion?.let {
                    Text("v$it", style = MaterialTheme.typography.labelSmall, color = TextMed, maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                h.car ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                h.driver ?: h.author ?: "-",
                style = MaterialTheme.typography.labelSmall,
                color = TextMed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun classBadgeColor(label: String): Color {
    val b = label.lowercase()
    return when {
        "hyper" in b || b == "hy" -> ClassHyper
        "gt3" in b -> ClassGt3
        "gte" in b -> ClassGte
        "lmp2" in b -> ClassLmp2
        "lmp3" in b -> ClassLmp3
        else -> ClassMixed
    }
}

@Composable
private fun ClassBadgeChip(label: String) {
    val c = classBadgeColor(label)
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(c).padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = onBadgeText(c), maxLines = 1)
    }
}

private fun classRank(carClass: String): Int {
    val c = carClass.lowercase()
    return when {
        "hyper" in c -> 0
        "lmp2" in c -> 1
        "lmp3" in c -> 2
        "gt3" in c -> 3
        "gte" in c -> 4
        else -> 5
    }
}

@Composable
private fun CarsTicker(groups: List<CarGroup>) {
    val ordered = remember(groups) { groups.sortedBy { classRank(it.carClass) } }
    if (ordered.all { it.cars.isEmpty() }) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.cars_section).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        // The marquee measures its content unbounded, so the chips must live in a
        // wrap-content Row inside a full-width Box (the Box is the visible viewport).
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ordered.forEach { g ->
                    g.cars.forEach { car -> CarTickerChip(car, g.carClass) }
                }
            }
        }
    }
}

@Composable
private fun CarTickerChip(car: String, carClass: String) {
    val accent = carClassColor(carClass)
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(7.dp))
        Text(car, style = MaterialTheme.typography.labelMedium, color = TextHigh, maxLines = 1)
    }
}

@Composable
private fun AvailableCarsSection(
    availableCars: Map<String, List<AvailableCar>>,
    liveryToModel: Map<String, String>,
) {
    val normalizedModels = remember(liveryToModel) {
        liveryToModel.mapKeys { (name, _) -> name.trim().lowercase() }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.cars_section).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.SemiBold,
        )
        availableCars.forEach { (className, cars) ->
            val collapsed = remember(cars, normalizedModels) {
                cars.map { car ->
                    val rawModel = normalizedModels[car.friendly.trim().lowercase()] ?: car.friendly
                    val model = stripCarClass(rawModel).ifBlank { rawModel }
                    model to car
                }.distinctBy { (model, car) ->
                    "${car.manufacturer.trim().lowercase()}|${model.trim().lowercase()}"
                }
            }
            if (collapsed.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(className, style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        collapsed.forEach { (model, car) ->
                            AvailableCarCard(car, model, carCardWidth(car.manufacturer))
                        }
                    }
                }
            }
        }
    }
}

private fun carCardWidth(manufacturer: String): Dp = when {
    manufacturer.length > 10 -> 264.dp
    manufacturer.length > 8 -> 244.dp
    else -> 220.dp
}

@Composable
private fun AvailableCarCard(car: AvailableCar, modelName: String, width: Dp) {
    Column(
        modifier = Modifier
            .width(width)
            .height(94.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(57.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            car.manufacturerLogoUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = car.manufacturer,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                car.manufacturer,
                style = MaterialTheme.typography.labelMedium,
                color = TextMed,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f, fill = false),
            )
            car.carImageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = modelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(96.dp).height(57.dp).clip(RoundedCornerShape(5.dp)),
                )
            }
        }
        Text(
            modelName,
            style = MaterialTheme.typography.bodySmall,
            color = TextHigh,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LeaderboardSkeletonCard() {
    val brush = shimmerBrush()
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large).padding(16.dp),
    ) {
        ShimmerBar(Modifier.width(130.dp).height(16.dp), brush)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBar(Modifier.width(14.dp).height(8.dp), brush)
            Spacer(Modifier.width(12.dp))
            ShimmerBar(Modifier.width(46.dp).height(8.dp), brush)
            Spacer(Modifier.weight(1f))
            ShimmerBar(Modifier.width(52.dp).height(8.dp), brush)
            Spacer(Modifier.width(12.dp))
            ShimmerBar(Modifier.width(34.dp).height(8.dp), brush)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBar(Modifier.width(16.dp).height(13.dp), brush)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBar(Modifier.fillMaxWidth(0.45f).height(13.dp), brush)
                Spacer(Modifier.height(6.dp))
                ShimmerBar(Modifier.fillMaxWidth(0.28f).height(9.dp), brush)
            }
            Spacer(Modifier.width(12.dp))
            ShimmerBar(Modifier.width(70.dp).height(13.dp), brush)
            Spacer(Modifier.width(10.dp))
            ShimmerBar(Modifier.width(44.dp).height(13.dp), brush)
        }
        Spacer(Modifier.height(12.dp))
        ShimmerBar(Modifier.fillMaxWidth().height(38.dp), brush, corner = 8.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotlapsSkeleton(count: Int) {
    val brush = shimmerBrush()
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        ShimmerBar(Modifier.width(58.dp).height(11.dp), brush)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(count) {
                Column(
                    Modifier.width(HOTLAP_CARD_W).height(HOTLAP_CARD_H)
                        .clip(RoundedCornerShape(12.dp)).background(Surface2),
                ) {
                    ShimmerBar(Modifier.fillMaxWidth().height(HOTLAP_THUMB_H), brush, corner = 0.dp)
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ShimmerBar(Modifier.width(40.dp).height(16.dp), brush)
                            Spacer(Modifier.weight(1f))
                            ShimmerBar(Modifier.width(24.dp).height(10.dp), brush)
                        }
                        Spacer(Modifier.height(8.dp))
                        ShimmerBar(Modifier.fillMaxWidth(0.9f).height(10.dp), brush)
                        Spacer(Modifier.weight(1f))
                        ShimmerBar(Modifier.fillMaxWidth(0.6f).height(9.dp), brush)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(w: RaceWeather) {
    Card(stringResource(Res.string.weather)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            w.race?.let { WeatherSession(stringResource(Res.string.session_race), it) }
            w.qualifying?.let { WeatherSession(stringResource(Res.string.session_qualifying), it) }
            w.practice?.let { WeatherSession(stringResource(Res.string.session_practice), it) }
        }
    }
}

@Composable
private fun WeatherSession(label: String, sw: SessionWeather) {
    val rainPeak = sw.segments.maxOfOrNull { it.rainChance ?: 0 } ?: 0
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.SemiBold)
            if (rainPeak > 0) {
                Text(stringResource(Res.string.weather_rain, rainPeak), style = MaterialTheme.typography.labelSmall, color = ClassLmp2)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)),
        ) {
            sw.segments.forEach { seg ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight((seg.durationMin ?: 1).toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(skyColor(seg.sky, seg.rainChance ?: 0)),
                ) {
                    Text(skyEmoji(seg.sky, seg.rainChance ?: 0), style = MaterialTheme.typography.labelSmall, color = TextHigh)
                    seg.tempC?.let { Text("${it}C", style = MaterialTheme.typography.labelSmall, color = TextHigh) }
                }
            }
        }
    }
}

internal fun gapLabel(deltaMs: Long): String =
    "+${deltaMs / 1000}.${(deltaMs % 1000).toString().padStart(3, '0')}"

/** Wide enough for 4-digit ranks (full leaderboard goes into the thousands). */
private val POS_COL_W = 44.dp

private const val LB_PREVIEW = 5
private const val LEADERBOARDS_TIMEOUT_MS = 20_000L
private const val TOPCARS_CACHE_CHECK_TIMEOUT_MS = 5_000L
private const val TOPCARS_FETCH_TIMEOUT_MS = 30_000L

@Composable
private fun LeaderboardWithTopCars(
    lbs: RaceLeaderboards,
    raceId: String,
    raceClasses: List<String>,
    raceTitle: String,
    onOpenFull: (leaderboardId: String, title: String) -> Unit,
) {
    val classBoards = remember(lbs, raceClasses) {
        leaderboardClassBoards(lbs, raceClasses)
    }
    if (classBoards.isEmpty()) {
        lbs.overall?.let { board ->
            LeaderboardCard(board = board, raceTitle = raceTitle, onOpenFull = onOpenFull)
        } ?: Card(stringResource(Res.string.fastest_laps)) {
            Text(stringResource(Res.string.no_lap_times), style = MaterialTheme.typography.bodyMedium, color = TextLow)
        }
        return
    }

    var selected by remember(classBoards.map { it.carClass }) { mutableStateOf(0) }
    val selectedIndex = selected.coerceIn(0, classBoards.lastIndex)
    val selectedBoard = classBoards[selectedIndex]
    val topCarsClass = selectedBoard.carClass.takeUnless { isMonoClassRace(raceClasses) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ClassScopeSelector(classBoards, selectedIndex) { selected = it }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 760.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    LeaderboardCard(
                        board = selectedBoard,
                        raceTitle = raceTitle,
                        onOpenFull = onOpenFull,
                        modifier = Modifier.weight(1.15f),
                    )
                    TopCarsCard(
                        raceId = raceId,
                        displayClass = selectedBoard.carClass,
                        queryClass = topCarsClass,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LeaderboardCard(
                        board = selectedBoard,
                        raceTitle = raceTitle,
                        onOpenFull = onOpenFull,
                    )
                    TopCarsCard(
                        raceId = raceId,
                        displayClass = selectedBoard.carClass,
                        queryClass = topCarsClass,
                    )
                }
            }
        }
    }
}

private fun leaderboardClassBoards(lbs: RaceLeaderboards, raceClasses: List<String>): List<ClassLeaderboard> {
    val singleClass = raceClasses
        .firstOrNull { isUsableClassLabel(it) }
        ?.takeIf { isMonoClassRace(raceClasses) }
    val overall = lbs.overall
    if (singleClass != null && overall != null) return listOf(overall.copy(carClass = singleClass))

    val fromBackend = lbs.byClass
        .filter { isUsableClassLabel(it.carClass) && (it.entries.isNotEmpty() || it.leaderboardId != null) }
    return fromBackend
}

private fun isMonoClassRace(raceClasses: List<String>): Boolean =
    raceClasses.count(::isUsableClassLabel) == 1

@Composable
private fun LeaderboardCard(
    board: ClassLeaderboard,
    raceTitle: String,
    onOpenFull: (leaderboardId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(stringResource(Res.string.fastest_laps), modifier = modifier) {
        Column(Modifier.fillMaxWidth()) {
            val leader = board.entries.firstOrNull()?.bestLapMs ?: 0L
            // Where the signed-in player sits on this class's board (token-gated; waits
            // briefly for the token, null when not signed in / no entry).
            val repo = koinInject<RaceRepository>()
            val hasSession by repo.appToken.collectAsState()
            // Livery/team name → real car model, so rows show "BMW M Hybrid V8" instead
            // of "BMWMH Custom Team #397". Empty until the roster resolves (raw name shown).
            val liveryToModel by produceState(emptyMap<String, String>()) {
                value = runCatching { repo.liveryToModel() }.getOrDefault(emptyMap())
            }
            val me by produceState<MeRow>(MeRow.Loading, board.leaderboardId, board.carClass) {
                val id = board.leaderboardId
                value = MeRow.Loading
                value = if (id == null) {
                    MeRow.None
                } else {
                    runCatching { repo.leaderboardMe(id) }.getOrNull()
                        ?.let { MeRow.Found(it) } ?: MeRow.None
                }
            }
            when (val row = me) {
                is MeRow.Found -> {
                    FasterThanRow(row.entry)
                    YourPositionRow(row.entry, leader, liveryToModel)
                    Spacer(Modifier.height(10.dp))
                }
                // Active-session-only placeholders: a signed-out user has no position,
                // so we show nothing. While the row resolves → skeleton; if there's no
                // lap on this board → an explanatory empty row.
                MeRow.Loading -> if (hasSession != null) {
                    YourPositionSkeleton()
                    Spacer(Modifier.height(10.dp))
                }
                MeRow.None -> if (hasSession != null) {
                    YourPositionEmpty()
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (board.entries.isEmpty()) {
                Text(stringResource(Res.string.no_lap_times), style = MaterialTheme.typography.bodyMedium, color = TextLow)
            } else {
                board.entries.take(LB_PREVIEW).forEach { e -> LeaderboardRow(e, leader, liveryToModel = liveryToModel) }
            }
            board.leaderboardId?.let { id ->
                Spacer(Modifier.height(10.dp))
                FullLeaderboardButton {
                    val suffix = board.carClass.takeIf(::isUsableClassLabel)
                        ?.let { " - ${classLabelFull(it)}" } ?: ""
                    onOpenFull(id, raceTitle + suffix)
                }
            }
        }
    }
}

@Composable
private fun TopCarsCard(
    raceId: String,
    displayClass: String,
    queryClass: String?,
    modifier: Modifier = Modifier,
) {
    val repo = koinInject<RaceRepository>()
    val classQuery = remember(queryClass) { queryClass?.let(::topCarsClassQuery) }
    val scopeKey = classQuery ?: "overall"
    var topCarsLoadKey by remember(raceId, scopeKey) { mutableStateOf(0) }
    var fetchTopCars by remember(raceId, scopeKey) { mutableStateOf(false) }
    val initialTopCars = remember(raceId, scopeKey) {
        repo.peekTopCars(raceId, carClass = classQuery) ?: repo.cachedTopCars(raceId, carClass = classQuery)
    }
    val topCarsState by produceState<TopCarsUiState>(
        initialTopCars?.let { TopCarsUiState.Ready(it) } ?: TopCarsUiState.NoData,
        raceId,
        scopeKey,
        topCarsLoadKey,
    ) {
        val shouldFetch = fetchTopCars
        repo.cachedTopCars(raceId, carClass = classQuery)?.let { value = TopCarsUiState.Ready(it) }
        val hadReadyCache = value is TopCarsUiState.Ready
        if (shouldFetch && !hadReadyCache) value = TopCarsUiState.Fetching
        val timeoutMs = if (shouldFetch) TOPCARS_FETCH_TIMEOUT_MS else TOPCARS_CACHE_CHECK_TIMEOUT_MS
        val response = withTimeoutOrNull(timeoutMs) { repo.topCars(raceId, carClass = classQuery, fetch = shouldFetch) }
        value = response?.fold(
            onSuccess = { result ->
                when {
                    result.isReady -> TopCarsUiState.Ready(result)
                    hadReadyCache -> value
                    else -> TopCarsUiState.NoData
                }
            },
            onFailure = {
                when {
                    hadReadyCache -> value
                    shouldFetch -> TopCarsUiState.Error(it.topCarsErrorMessage())
                    else -> TopCarsUiState.NoData
                }
            },
        ) ?: if (shouldFetch) {
            if (hadReadyCache) value else TopCarsUiState.Error("Top cars are taking too long to load.")
        } else {
            if (hadReadyCache) value else TopCarsUiState.NoData
        }
    }
    Card(modifier = modifier) {
        TopCarsPanel(
            carClass = displayClass,
            state = topCarsState,
            onFetch = {
                fetchTopCars = true
                topCarsLoadKey += 1
            },
        )
    }
}

private sealed interface TopCarsUiState {
    data object Fetching : TopCarsUiState

    data object NoData : TopCarsUiState

    data class Ready(
        val result: TopCarsResult,
    ) : TopCarsUiState

    data class Error(
        val message: String,
    ) : TopCarsUiState
}

@Composable
private fun TopCarsPanel(carClass: String, state: TopCarsUiState, onFetch: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TopCarsHeader(carClass, topCarsLimitLabel(state, carClass))
        when (state) {
            TopCarsUiState.Fetching -> TopCarsSkeleton(building = true)
            TopCarsUiState.NoData -> TopCarsPrompt(carClass, onFetch)
            is TopCarsUiState.Error -> TopCarsError(state.message, onFetch)
            is TopCarsUiState.Ready -> {
                val cars = remember(state.result.topCars) {
                    state.result.topCars.sortedWith(
                        compareBy<TopCar> { it.bestLapMs.takeIf { ms -> ms > 0 } ?: Long.MAX_VALUE }
                            .thenBy { it.rank },
                    )
                }
                val leaderMs = cars.firstOrNull { it.bestLapMs > 0 }?.bestLapMs ?: 0L
                val limit = state.result.leaderboardLimit.takeIf { it > 0 } ?: 100
                cars.forEachIndexed { index, car ->
                    TopCarCard(
                        displayRank = index + 1,
                        car = car,
                        leaderMs = leaderMs,
                        leaderboardLimit = limit,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCarsHeader(carClass: String, limitLabel: String) {
    val accent = carClassColor(carClass)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TOP CARS",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "· ${classLabelFull(carClass)}",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(Outline))
        }
        Text(
            limitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun topCarsLimitLabel(state: TopCarsUiState, carClass: String): String {
    val limit = (state as? TopCarsUiState.Ready)?.result?.leaderboardLimit?.takeIf { it > 0 } ?: 100
    return "based on top $limit ${classLabelFull(carClass)} entries"
}

@Composable
private fun TopCarsPrompt(carClass: String, onFetch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TopCarsEmptyMark()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "No top cars yet",
                style = MaterialTheme.typography.labelLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Build ${classLabelFull(carClass)} from the leaderboard",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TopCarsActionButton("Build", onFetch, compact = true)
    }
}

@Composable
private fun TopCarsError(message: String, onFetch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TopCarsEmptyMark()
        Text(
            message,
            style = MaterialTheme.typography.labelMedium,
            color = TextLow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TopCarsActionButton("Retry", onFetch, compact = true)
    }
}

@Composable
private fun TopCarsEmptyMark() {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "LB",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopCarsActionButton(label: String, onClick: () -> Unit, compact: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val minWidth = if (compact) 72.dp else 156.dp
    val maxWidth = if (compact) 120.dp else 240.dp
    val horizontalPadding = if (compact) 12.dp else 16.dp
    val verticalPadding = if (compact) 7.dp else 11.dp
    val textStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    Box(
        Modifier.widthIn(min = minWidth, max = maxWidth)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = textStyle, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TopCarsSkeleton(building: Boolean) {
    val brush = shimmerBrush()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (building) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2)
                    .border(1.dp, Outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Building top cars from leaderboard",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMed,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        repeat(if (building) 3 else 1) {
            Row(
                Modifier.fillMaxWidth()
                    .height(94.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBar(Modifier.size(42.dp), brush, corner = 10.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBar(Modifier.fillMaxWidth(0.45f).height(12.dp), brush)
                    ShimmerBar(Modifier.fillMaxWidth(0.75f).height(16.dp), brush)
                    ShimmerBar(Modifier.fillMaxWidth(0.55f).height(10.dp), brush)
                }
                Spacer(Modifier.width(10.dp))
                ShimmerBar(Modifier.width(96.dp).height(58.dp), brush, corner = 6.dp)
            }
        }
    }
}

@Composable
private fun TopCarCard(
    displayRank: Int,
    car: TopCar,
    leaderMs: Long,
    leaderboardLimit: Int,
) {
    val carClass = car.carClass?.takeIf { it.isNotBlank() } ?: car.firstLivery?.carClass?.takeIf { it.isNotBlank() }
    val manufacturer = car.manufacturer?.takeIf { it.isNotBlank() }
        ?: car.firstLivery?.manufacturer?.takeIf { it.isNotBlank() }
    val logo = car.manufacturerLogoUrl?.takeIf { it.isNotBlank() }
        ?: car.firstLivery?.manufacturerLogoUrl?.takeIf { it.isNotBlank() }
    val image = car.firstLivery?.imageUrl?.takeIf { it.isNotBlank() }
    val model = car.model.ifBlank { car.firstLivery?.model.orEmpty() }.ifBlank { car.car }
    val accent = carClass?.let { carClassColor(it) } ?: Outline
    val deltaMs = (car.bestLapMs - leaderMs).takeIf { car.bestLapMs > 0 && leaderMs > 0 } ?: 0L

    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopCarManufacturerMark(manufacturer, logo)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        topCarLapLabel(car.bestLapMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextHigh,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    TopCarDeltaBadge(deltaMs, accent)
                }
                Text(
                    stripCarClass(model),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().basicMarquee(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    TopCarRankBadge(displayRank, accent)
                    carClass?.let { TopCarClassPill(it) }
                    TopCarLbRankBadge(car.bestRank, accent)
                    TopCarEntriesShare(car.count, leaderboardLimit, TextMed)
                }
            }
            TopCarImageBox(image, model)
        }
    }
}

@Composable
private fun TopCarManufacturerMark(manufacturer: String?, logoUrl: String?) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier.size(34.dp)
            .clip(shape)
            .background(Surface1)
            .border(1.dp, Outline, shape)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = manufacturer,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(22.dp),
            )
        } else {
            Text(
                manufacturer?.firstOrNull()?.uppercaseChar()?.toString() ?: "-",
                style = MaterialTheme.typography.titleSmall,
                color = TextMed,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun TopCarRankBadge(rank: Int, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            topCarRankLabel(rank),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopCarClassPill(carClass: String) {
    val c = carClassColor(carClass)
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(c.copy(alpha = 0.14f))
            .border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            classLabelFull(carClass),
            style = MaterialTheme.typography.labelSmall,
            color = c,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopCarDeltaBadge(deltaMs: Long, color: Color) {
    val isLeader = deltaMs <= 0
    val label = if (isLeader) "Leader" else gapLabel(deltaMs)
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (isLeader) color.copy(alpha = 0.14f) else Surface1)
            .border(1.dp, if (isLeader) color.copy(alpha = 0.45f) else Outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isLeader) color else TextMed,
            fontFamily = if (isLeader) FontFamily.Default else FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopCarLbRankBadge(rank: Int, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            "LB ${topCarRankLabel(rank)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopCarEntriesShare(count: Int, limit: Int, color: Color) {
    val safeLimit = limit.coerceAtLeast(1)
    Row(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("$count/$safeLimit", style = MaterialTheme.typography.labelSmall, color = TextHigh, fontWeight = FontWeight.Black, maxLines = 1)
        Text("ENTRIES", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TopCarImageBox(imageUrl: String?, model: String) {
    Box(
        modifier = Modifier.width(96.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Surface1),
        contentAlignment = Alignment.Center,
    ) {
        imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = model,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        }
    }
}

private fun topCarRankLabel(rank: Int): String =
    rank.takeIf { it > 0 }?.let { "#$it" } ?: "-"

private fun topCarLapLabel(ms: Long): String =
    ms.takeIf { it > 0 }?.let { formatLap(it) } ?: "-"

private fun Throwable.topCarsErrorMessage(): String = when (message) {
    "app_check_required" -> "Could not verify the app token."
    "event_not_found" -> "This event is no longer available."
    "class_leaderboard_not_found" -> "Top cars are not available for this class yet."
    "leaderboard_unavailable" -> "Could not build top cars right now."
    "topcars_build_error" -> "Could not build top cars right now."
    else -> "Could not load top cars."
}

private sealed interface MeRow {
    data object Loading : MeRow
    data object None : MeRow
    data class Found(val entry: LapEntry) : MeRow
}

@Composable
private fun YourPositionLabel() {
    Text(
        stringResource(Res.string.your_position),
        style = MaterialTheme.typography.labelSmall,
        color = TextMed,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun YourPositionPlaceholderBox(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) { content() }
}

@Composable
private fun YourPositionSkeleton() {
    val brush = shimmerBrush()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        YourPositionLabel()
        YourPositionPlaceholderBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBar(Modifier.width(20.dp).height(15.dp), brush)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShimmerBar(Modifier.fillMaxWidth(0.4f).height(13.dp), brush)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBar(Modifier.fillMaxWidth(0.55f).height(9.dp), brush)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    ShimmerBar(Modifier.width(70.dp).height(13.dp), brush)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBar(Modifier.width(44.dp).height(9.dp), brush)
                }
            }
        }
    }
}

@Composable
private fun YourPositionEmpty() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        YourPositionLabel()
        YourPositionPlaceholderBox {
            Text(
                stringResource(Res.string.your_position_none),
                style = MaterialTheme.typography.bodySmall,
                color = TextLow,
            )
        }
    }
}

/** "⚡ You're faster than 97.7% of drivers" — shown above the board for the signed-in player.
 *  Hidden when the percentile is absent or the board is unstable (numbers can't be trusted). */
@Composable
private fun FasterThanRow(entry: LapEntry) {
    val pct = entry.fasterThanPct
    if (pct == null || entry.rankUnstable) return
    val accent = MaterialTheme.colorScheme.primary
    Column {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(IconBolt, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                "You're faster than ${formatPct(pct)}% of drivers",
                style = MaterialTheme.typography.labelMedium,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** Coarse one-decimal percent, trailing ".0" dropped: 97.69 → "97.7", 98.0 → "98". */
private fun formatPct(p: Double): String {
    val r = (p * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

@Composable
private fun YourPositionRow(entry: LapEntry, leader: Long, liveryToModel: Map<String, String> = emptyMap()) {
    val accent = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        YourPositionLabel()
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        ) {
            LeaderboardRow(entry, leader, liveryToModel = liveryToModel)
        }
    }
}

@Composable
private fun ClassScopeSelector(tabs: List<ClassLeaderboard>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "CLASS",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { i, b ->
                val active = i == selected
                val c = carClassColor(b.carClass)
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (active) c else Surface2)
                        .border(1.dp, if (active) c else Outline, RoundedCornerShape(8.dp))
                        .clickable { onSelect(i) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        classLabelFull(b.carClass),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) onBadgeText(c) else TextMed,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullLeaderboardButton(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.full_leaderboard),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun ClassSectionHeader(carClass: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(carClassColor(carClass)))
        Spacer(Modifier.width(8.dp))
        Text(classLabelFull(carClass), style = MaterialTheme.typography.labelMedium, color = TextMed, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Outline))
    }
}

private fun classLabelFull(carClass: String): String {
    val c = carClass.lowercase()
    return when {
        "hyper" in c -> "HYPERCAR"
        "lmgt3" in c -> "LMGT3"
        "gt3" in c -> "GT3"
        "gte" in c -> "GTE"
        "lmp2" in c -> "LMP2"
        "lmp3" in c -> "LMP3"
        else -> carClass.uppercase()
    }
}

private fun topCarsClassQuery(carClass: String): String {
    val c = carClass.lowercase()
    return when {
        "hyper" in c -> "Hypercar"
        "lmgt3" in c -> "LMGT3"
        "gt3" in c -> "GT3"
        "gte" in c -> "GTE"
        "lmp2" in c -> "LMP2"
        "lmp3" in c -> "LMP3"
        else -> carClass
    }
}

private fun isUsableClassLabel(label: String): Boolean =
    label.isNotBlank() && label != "-" && label != "\u2014"

@Composable
internal fun LeaderboardRow(
    e: LapEntry,
    leaderMs: Long,
    alt: Boolean = false,
    liveryToModel: Map<String, String> = emptyMap(),
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (alt) Surface2 else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${e.rank}",
                Modifier.width(POS_COL_W),
                style = MaterialTheme.typography.titleSmall,
                color = e.carClass?.let { carClassColor(it) } ?: TextMed,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    e.initials,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                e.car?.let { car ->
                    Text(
                        liveryToModel[car] ?: car,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLow,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatLap(e.bestLapMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHigh,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    if (e.bestLapMs <= leaderMs) "-" else gapLabel(e.bestLapMs - leaderMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMed,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
        }
        if (e.sectors.size == 3) {
            Row(
                Modifier.fillMaxWidth().padding(start = POS_COL_W, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                e.sectors.forEachIndexed { idx, s -> SectorSplit(idx + 1, s) }
            }
        }
    }
}

@Composable
private fun SectorSplit(index: Int, seconds: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("S$index", style = MaterialTheme.typography.labelSmall, color = TextLow)
        Spacer(Modifier.width(4.dp))
        Text(
            formatSector(seconds),
            style = MaterialTheme.typography.labelSmall,
            color = TextMed,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

@Composable
private fun settingRows(s: RaceSettings): List<Pair<String, String>> = listOfNotNull(
    s.qualifyingLength?.let { stringResource(Res.string.set_qualifying) to "${it}m" },
    s.practiceLength?.let { stringResource(Res.string.set_practice) to "${it}m" },
    s.setup?.let { stringResource(Res.string.set_setup) to it },
    s.assists?.let { stringResource(Res.string.set_assists) to it },
    s.damage?.let { stringResource(Res.string.set_damage) to it },
    s.tireWear?.let { stringResource(Res.string.set_tire_wear) to it },
    s.fuelUsage?.let { stringResource(Res.string.set_fuel_usage) to it },
    s.safetyRank?.let { stringResource(Res.string.set_safety_rank) to it },
    s.driverRank?.let { stringResource(Res.string.set_driver_rank) to it },
    s.splitSize?.let { stringResource(Res.string.set_split_size) to it.toString() },
    s.startIntervalMin?.let { stringResource(Res.string.set_start_interval) to "every ${it}m" },
    s.driverSwaps?.let { stringResource(Res.string.set_driver_swaps) to if (it) stringResource(Res.string.yes) else stringResource(Res.string.no) },
    s.trackLimits?.let { stringResource(Res.string.set_track_limits) to it },
    s.tireWarmers?.let { stringResource(Res.string.set_tire_warmers) to it },
    s.limitedTires?.let { stringResource(Res.string.set_limited_tires) to it },
)

@Composable
private fun Card(
    title: String? = null,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Surface1)
            .border(1.dp, Outline, MaterialTheme.shapes.large).padding(16.dp),
    ) {
        if (title != null || leading != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(12.dp))
                }
                title?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

private fun flagUrlFromCode(code: String): String? {
    val cc = code.trim().lowercase()
    if (cc.length != 2 || cc.any { it !in 'a'..'z' }) return null
    return "https://cdn.jsdelivr.net/gh/lipis/flag-icons/flags/4x3/$cc.svg"
}

private fun flagUrl(country: String): String? {
    val code = when (country.trim().lowercase()) {
        "france" -> "fr"
        "united states", "usa", "united states of america" -> "us"
        "united kingdom", "uk", "great britain", "england" -> "gb"
        "portugal" -> "pt"
        "brazil" -> "br"
        "italy" -> "it"
        "belgium" -> "be"
        "germany" -> "de"
        "spain" -> "es"
        "austria" -> "at"
        "netherlands" -> "nl"
        "bahrain" -> "bh"
        "qatar" -> "qa"
        "japan" -> "jp"
        "saudi arabia" -> "sa"
        "united arab emirates", "uae" -> "ae"
        "australia" -> "au"
        "canada" -> "ca"
        "mexico" -> "mx"
        "china" -> "cn"
        else -> return null
    }
    return "https://cdn.jsdelivr.net/gh/lipis/flag-icons/flags/4x3/$code.svg"
}

@Composable
private fun DetailRows(rows: List<Pair<String, String>>) {
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { i, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (i % 2 == 1) Surface2 else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextLow)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = TextHigh, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun CircleButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(38.dp).clip(CircleShape).background(Carbon.copy(alpha = 0.55f))
            .border(1.dp, Outline, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHigh, modifier = Modifier.size(22.dp))
    }
}
