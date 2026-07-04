package com.orioooneee.lmuasister.ui.cars

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.remote.CarDetailedDto
import com.orioooneee.lmuasister.data.remote.CarLiveryItemDto
import com.orioooneee.lmuasister.data.remote.displayId
import com.orioooneee.lmuasister.data.remote.galleryUrls
import com.orioooneee.lmuasister.data.remote.heroUrl
import com.orioooneee.lmuasister.data.remote.image
import com.orioooneee.lmuasister.ui.components.MetaChip
import com.orioooneee.lmuasister.ui.components.ShimmerBar
import com.orioooneee.lmuasister.ui.components.classColorFor
import com.orioooneee.lmuasister.ui.components.classDisplayLabel
import com.orioooneee.lmuasister.ui.components.onBadgeText
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.profile.stripCarClass
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import org.koin.compose.koinInject

private val MinCarCardHeight = 232.dp

@Composable
fun CarsScreen(
    insets: PaddingValues,
    onOpenCar: (CarDetailedDto) -> Unit,
) {
    val repo = koinInject<RaceRepository>()
    val cars by produceState<List<CarDetailedDto>?>(repo.cachedDetailedCars()) {
        repo.cachedDetailedCars()?.let { value = it }
        repo.detailedCars().getOrNull()?.let { value = it }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp + insets.calculateTopPadding(), end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Cars", style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Bold)
            cars?.let { Text("${it.size} models", style = MaterialTheme.typography.labelMedium, color = TextLow) }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth < 360.dp -> 1
                maxWidth < 920.dp -> 2
                maxWidth < 1240.dp -> 3
                else -> 4
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val list = cars
                if (list == null) {
                    items(3) {
                        CardSkeletonRow(columns)
                    }
                } else {
                    orderedCarSections(list).forEach { (category, group) ->
                        item(key = "section_$category") {
                            CarSectionHeader(category, group.size)
                        }
                        items(
                            items = group.chunked(columns),
                            key = { row -> row.joinToString("|") { it.displayId() } },
                        ) { rowCars ->
                            CarGridRow(rowCars, columns, onOpenCar)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarDetailScreen(
    carId: String,
    initialCar: CarDetailedDto?,
    insets: PaddingValues,
    onBack: () -> Unit,
) {
    val repo = koinInject<RaceRepository>()
    val result by produceState<Result<CarDetailedDto>?>(initialCar?.let { Result.success(it) }, carId, initialCar) {
        initialCar?.let { value = Result.success(it) }
        val fresh = runCatching { repo.detailedCar(carId) }
        when {
            fresh.isSuccess && fresh.getOrNull() != null -> value = Result.success(fresh.getOrNull()!!)
            value == null -> value = Result.failure(fresh.exceptionOrNull() ?: IllegalStateException("Couldn't load this car"))
        }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        val car = result?.getOrNull() ?: initialCar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                car?.let { cleanCarName(it) } ?: "Car",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val res = result) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> res.fold(
                onSuccess = { CarDetailContent(it, insets.calculateBottomPadding()) },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(it.message ?: "Couldn't load this car", style = MaterialTheme.typography.bodyMedium, color = TextMed)
                    }
                },
            )
        }
    }
}

@Composable
private fun CarGridRow(cars: List<CarDetailedDto>, columns: Int, onOpenCar: (CarDetailedDto) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cars.forEach { car ->
            CarGridCard(
                car = car,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenCar(car) },
            )
        }
        repeat(columns - cars.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CarGridCard(car: CarDetailedDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = classColorFor(car.category)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinCarCardHeight)
            .clip(shape)
            .background(Surface1)
            .border(1.dp, accent.copy(alpha = 0.58f), shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(118.dp).background(Surface2)) {
            car.heroUrl()?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = car.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.52f to Color.Transparent, 1f to Carbon.copy(alpha = 0.88f))),
            )
            ClassBadge(
                label = classDisplayLabel(car.category).ifBlank { car.category },
                color = accent,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            )
            car.manufacturerLogoUrl.takeIf { it.isNotBlank() }?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = car.manufacturer,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(width = 64.dp, height = 44.dp),
                )
            }
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ManufacturerLabel(car.manufacturer, accent)
            Text(
                cleanCarName(car),
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            CarCardFacts(car)
        }
    }
}

@Composable
private fun CarSectionHeader(category: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            category.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = TextHigh,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Text(
            "$count cars",
            style = MaterialTheme.typography.labelLarge,
            color = classColorFor(category),
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun CarCardFacts(car: CarDetailedDto) {
    val engine = car.specs.engine.takeIf { it.isNotBlank() }
    val compactFacts = listOfNotNull(
        car.liveryCount.takeIf { it > 0 }?.let { "$it liveries" },
        car.specs.power.takeIf { it.isNotBlank() },
        car.specs.weight.takeIf { it.isNotBlank() },
    )
    if (engine == null && compactFacts.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        engine?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = TextMed,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().basicMarquee(),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            compactFacts.forEach { fact -> FactChip(fact) }
        }
    }
}

@Composable
private fun FactChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Surface2)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = TextMed,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ManufacturerLabel(manufacturer: String, color: Color) {
    if (manufacturer.isBlank()) return
    Text(
        manufacturer.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CardSkeletonRow(columns: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(columns) {
            CarCardSkeleton(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun CarCardSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinCarCardHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp)),
    ) {
        Box(Modifier.fillMaxWidth().height(112.dp).background(brush))
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBar(Modifier.fillMaxWidth(0.55f).height(12.dp), brush)
            ShimmerBar(Modifier.fillMaxWidth(0.9f).height(14.dp), brush)
            ShimmerBar(Modifier.fillMaxWidth(0.65f).height(14.dp), brush)
        }
    }
}

@Composable
private fun CarDetailContent(car: CarDetailedDto, bottomInset: androidx.compose.ui.unit.Dp) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val liveryColumns = if (maxWidth >= 840.dp) 2 else 1
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (car.heroUrl() != null || car.description.isNotBlank()) {
                item { CarHeroCard(car) }
            } else {
                item { CarSummaryCard(car) }
            }

            val gallery = car.galleryUrls()
            if (gallery.isNotEmpty()) {
                item { SectionTitle("Gallery") }
                item { GalleryStrip(gallery, car.name) }
            }

            val specItems = headlineSpecs(car)
            if (specItems.isNotEmpty()) {
                item { SectionTitle("Specs") }
                item { SpecsCard(specItems) }
            }

            val tech = extraTechSpecs(car, specItems)
            if (tech.isNotEmpty()) {
                item { SectionTitle("Tech specs") }
                item { SpecsCard(tech) }
            }

            if (car.liveries.isNotEmpty()) {
                item { SectionTitle("Liveries") }
                items(
                    items = car.liveries.chunked(liveryColumns),
                    key = { row -> row.joinToString("|") { it.id.ifBlank { it.name } } },
                ) { row ->
                    LiveryGridRow(row, liveryColumns)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarHeroCard(car: CarDetailedDto) {
    val accent = classColorFor(car.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp)),
    ) {
        Box(Modifier.fillMaxWidth().height(210.dp).background(Surface2)) {
            car.heroUrl()?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = car.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0f to Color.Transparent, 1f to Carbon.copy(alpha = 0.86f))),
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClassBadge(classDisplayLabel(car.category).ifBlank { car.category }, accent)
                    if (car.liveryCount > 0) MetaChip("${car.liveryCount} liveries")
                    car.specs.engine.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
                }
                Text(cleanCarName(car), style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Black)
                ManufacturerRow(car.manufacturer, car.manufacturerLogoUrl)
            }
        }
        car.description.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMed,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarSummaryCard(car: CarDetailedDto) {
    val accent = classColorFor(car.category)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClassBadge(classDisplayLabel(car.category).ifBlank { car.category }, accent)
        if (car.liveryCount > 0) MetaChip("${car.liveryCount} liveries")
        car.specs.engine.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
        if (car.manufacturer.isNotBlank()) MetaChip(car.manufacturer)
    }
}

@Composable
private fun GalleryStrip(urls: List<String>, contentDescription: String) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(urls) { url ->
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(238.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .border(1.dp, Outline, RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun SpecsCard(items: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        specRows(items).forEach { rowItems ->
            val wide = rowItems.size == 1 && isWideSpec(rowItems.first())
            if (wide) {
                val (label, value) = rowItems.first()
                SpecCell(label, value, Modifier.fillMaxWidth(), expanded = true, matchRowHeight = false)
            } else {
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { (label, value) ->
                        SpecCell(label, value, Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpecCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    matchRowHeight: Boolean = true,
) {
    Column(
        modifier = modifier
            .then(if (matchRowHeight) Modifier.fillMaxHeight() else Modifier)
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLow, maxLines = 1)
        if (expanded) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LiveryGridRow(liveries: List<CarLiveryItemDto>, columns: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        liveries.forEach { livery ->
            LiveryRow(livery, Modifier.weight(1f).fillMaxHeight())
        }
        repeat(columns - liveries.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LiveryRow(livery: CarLiveryItemDto, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = livery.image(),
            contentDescription = livery.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(104.dp).height(64.dp).clip(RoundedCornerShape(8.dp)).background(Surface2),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                livery.name.ifBlank { livery.mappedAssetName.ifBlank { "Livery" } },
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                livery.series.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
                livery.vehicleVersion.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
            }
        }
    }
}

@Composable
private fun ManufacturerRow(manufacturer: String, logoUrl: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = manufacturer,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 32.dp, height = 18.dp),
            )
        }
        Text(
            manufacturer.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClassBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onBadgeText(color), fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = TextHigh, fontWeight = FontWeight.Bold)
}

private fun cleanCarName(car: CarDetailedDto): String =
    stripCarClass(car.name.ifBlank { car.slug.ifBlank { car.id } }).ifBlank { car.name.ifBlank { car.id } }

private fun orderedCarSections(cars: List<CarDetailedDto>): List<Pair<String, List<CarDetailedDto>>> =
    cars.sortedWith(
        compareBy<CarDetailedDto>(
            { categoryRank(it.category) },
            { sectionTitle(it.category) },
            { it.manufacturer },
            { cleanCarName(it) },
        ),
    )
        .groupBy { sectionTitle(it.category) }
        .toList()

private fun sectionTitle(category: String): String =
    category.trim().takeIf { it.isNotBlank() } ?: "Other"

private fun categoryRank(category: String): Int {
    val c = category.lowercase()
    return when {
        "gte" in c -> 0
        "gt3" in c -> 1
        "lmp3" in c -> 2
        "lmp2" in c -> 3
        "hyper" in c || "lmh" in c || "lmdh" in c || "gtp" in c -> 4
        else -> 9
    }
}

private fun headlineSpecs(car: CarDetailedDto): List<Pair<String, String>> = buildList {
    car.specs.engine.takeIf { it.isNotBlank() }?.let { add("Engine" to it) }
    car.specs.power.takeIf { it.isNotBlank() }?.let { add("Power" to it) }
    car.specs.weight.takeIf { it.isNotBlank() }?.let { add("Weight" to it) }
    car.specs.transmission.takeIf { it.isNotBlank() }?.let { add("Transmission" to it) }
    car.specs.length.takeIf { it.isNotBlank() }?.let { add("Length" to it) }
    car.specs.width.takeIf { it.isNotBlank() }?.let { add("Width" to it) }
    car.specs.height.takeIf { it.isNotBlank() }?.let { add("Height" to it) }
    car.specs.bestResult.takeIf { it.isNotBlank() }?.let { add("Best result" to it) }
}

private fun specRows(items: List<Pair<String, String>>): List<List<Pair<String, String>>> {
    val rows = mutableListOf<List<Pair<String, String>>>()
    var pending: Pair<String, String>? = null
    items.forEach { item ->
        if (isWideSpec(item)) {
            pending?.let { rows += listOf(it) }
            pending = null
            rows += listOf(item)
        } else {
            val left = pending
            if (left == null) {
                pending = item
            } else {
                rows += listOf(left, item)
                pending = null
            }
        }
    }
    pending?.let { rows += listOf(it) }
    return rows
}

private fun isWideSpec(item: Pair<String, String>): Boolean {
    val (label, value) = item
    val key = label.specKey()
    return value.length > 28 ||
        (key in setOf("engine", "transmission", "chassis", "bestresult") && value.length > 20)
}

private fun extraTechSpecs(car: CarDetailedDto, headline: List<Pair<String, String>>): List<Pair<String, String>> {
    val headlineLabels = headline.mapTo(mutableSetOf()) { it.first.specKey() }
    val headlineValues = headline.mapTo(mutableSetOf()) { it.second.specValue() }
    val hiddenLabels = setOf("category", "class", "carclass", "vehicleclass")
    return car.techSpecs.entries
        .asSequence()
        .mapNotNull { (key, value) ->
            val cleanValue = value.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val cleanKey = key.replace('_', ' ').trim().replaceFirstChar(Char::uppercaseChar)
            cleanKey to cleanValue
        }
        .filterNot { (key, value) ->
            val normalizedKey = key.specKey()
            normalizedKey in hiddenLabels || normalizedKey in headlineLabels || value.specValue() in headlineValues
        }
        .toList()
}

private fun String.specKey(): String =
    lowercase().replace(Regex("[^a-z0-9]"), "")

private fun String.specValue(): String =
    lowercase().replace(Regex("[^a-z0-9]"), "")
