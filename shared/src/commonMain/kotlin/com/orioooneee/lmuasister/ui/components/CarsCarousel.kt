package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.ui.theme.ClassGt3
import com.orioooneee.lmuasister.ui.theme.ClassGte
import com.orioooneee.lmuasister.ui.theme.ClassHyper
import com.orioooneee.lmuasister.ui.theme.ClassLmp2
import com.orioooneee.lmuasister.ui.theme.ClassLmp3
import com.orioooneee.lmuasister.ui.theme.ClassMixed
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.cars_count
import lmuassister.shared.generated.resources.cars_section
import org.jetbrains.compose.resources.stringResource

fun carClassColor(carClass: String): Color {
    val c = carClass.lowercase()
    return when {
        "hyper" in c -> ClassHyper
        "gt3" in c -> ClassGt3
        "gte" in c -> ClassGte
        "lmp2" in c -> ClassLmp2
        "lmp3" in c -> ClassLmp3
        else -> ClassMixed
    }
}

@Composable
fun CarsCarousel(cars: List<CarModel>, modifier: Modifier = Modifier) {
    if (cars.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.cars_section),
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.cars_count, cars.size),
                style = MaterialTheme.typography.labelMedium,
                color = TextLow,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cars, key = { it.id }) { car -> CarCard(car) }
        }
    }
}

private val CAR_CARD_W = 168.dp
private val CAR_CARD_H = 96.dp

@Composable
private fun CarCard(car: CarModel) {
    val accent = carClassColor(car.carClass)
    Row(
        Modifier
            .width(CAR_CARD_W)
            .height(CAR_CARD_H)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(12.dp)),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
        Column(Modifier.fillMaxHeight().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                car.manufacturer?.let {
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    car.model,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ClassPill(classDisplayLabel(car.carClass), accent)
        }
    }
}

@Composable
private fun ClassPill(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(5.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = onBadgeText(color), maxLines = 1)
    }
}
