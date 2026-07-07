package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.data.steam.SteamAchievement
import com.orioooneee.lmuasister.data.steam.SteamAchievements
import com.orioooneee.lmuasister.ui.IconChampionship
import com.orioooneee.lmuasister.ui.components.EmptyState
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Outline
import com.orioooneee.lmuasister.ui.theme.OutlineSoft
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.Surface3
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import com.orioooneee.lmuasister.ui.util.formatIsoDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

@Composable
fun SteamAchievementsScreen(
    viewModel: SteamLoginViewModel,
    insets: PaddingValues = PaddingValues(),
    onBack: () -> Unit,
) {
    val state by viewModel.achievementsState.collectAsStateWithLifecycle()
    val topInset = insets.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()
    val isRefreshing = state.refreshing || (state.loading && state.data != null)

    LaunchedEffect(Unit) {
        viewModel.loadAchievements()
    }

    RefreshableContent(isRefreshing, viewModel::refreshAchievements, topInset = topInset) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 286.dp),
            modifier = Modifier.fillMaxSize().background(Carbon),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = topInset + 10.dp,
                end = 16.dp,
                bottom = bottomInset + 28.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                AchievementsTopBar(
                    loading = state.loading && state.data == null,
                    refreshing = isRefreshing,
                    onBack = onBack,
                    onRefresh = viewModel::refreshAchievements,
                )
            }

            state.data?.let { data ->
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    AchievementsHero(data, fromCache = state.fromCache, error = state.error)
                }
                items(
                    items = data.achievements,
                    key = { "${it.name}_${it.unlockTime}" },
                ) { achievement ->
                    AchievementCard(achievement)
                }
            }

            if (state.loading && state.data == null) {
                items(8) {
                    AchievementSkeletonCard()
                }
            } else if (state.data == null && state.error != null) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Couldn't load achievements",
                        subtitle = state.error,
                        icon = IconChampionship,
                        accent = Amber,
                        actionLabel = "Retry",
                        onAction = viewModel::refreshAchievements,
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsTopBar(
    loading: Boolean,
    refreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHigh)
        }
        Column(Modifier.weight(1f)) {
            Text("Steam achievements", style = MaterialTheme.typography.titleLarge, color = TextHigh, fontWeight = FontWeight.Black)
            Text("Le Mans Ultimate", style = MaterialTheme.typography.bodySmall, color = TextLow)
        }
        IconButton(onClick = onRefresh, enabled = !loading && !refreshing) {
            if (refreshing) {
                CircularProgressIndicator(color = Amber, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Amber)
            }
        }
    }
}

@Composable
private fun AchievementsHero(data: SteamAchievements, fromCache: Boolean, error: String?) {
    val pct = (data.progress * 100f).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Amber.copy(alpha = 0.34f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Amber.copy(alpha = 0.13f))
                    .border(1.dp, Amber.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconChampionship, contentDescription = null, tint = Amber, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${data.unlocked}/${data.total}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextHigh,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    "$pct% complete",
                    style = MaterialTheme.typography.labelLarge,
                    color = Amber,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        ProgressRail(data.progress)
        when {
            error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = Amber)
            fromCache -> Text("Showing saved achievements", style = MaterialTheme.typography.bodySmall, color = TextLow)
        }
    }
}

@Composable
private fun AchievementCard(achievement: SteamAchievement) {
    val accent = if (achievement.achieved) Amber else TextLow
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (achievement.achieved) Surface1 else Surface2)
            .border(1.dp, if (achievement.achieved) Amber.copy(alpha = 0.26f) else OutlineSoft, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AchievementImage(achievement)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    achievement.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (achievement.achieved) TextHigh else TextMed,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                achievement.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMed,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AchievementStatusPill(if (achievement.achieved) "Unlocked" else "Locked", accent)
            achievement.unlockLabel()?.let {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AchievementImage(achievement: SteamAchievement) {
    val alpha = if (achievement.achieved) 1f else 0.52f
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface3)
            .border(1.dp, Outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (!achievement.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = achievement.imageUrl,
                contentDescription = achievement.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(alpha),
            )
        } else {
            Icon(IconChampionship, contentDescription = null, tint = TextLow, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun AchievementStatusPill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun AchievementSkeletonCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, OutlineSoft, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Surface3))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Surface3))
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(Surface3))
                Box(Modifier.fillMaxWidth(0.66f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Surface3))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.widthIn(min = 82.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).background(Surface3))
    }
}

@Composable
private fun ProgressRail(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Surface3)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Amber),
        )
    }
}

private fun SteamAchievement.unlockLabel(): String? =
    unlockTime.takeIf { achieved && it > 0L }
        ?.let { Instant.fromEpochSeconds(it).toString() }
        ?.let { formatIsoDateTime(it) }
