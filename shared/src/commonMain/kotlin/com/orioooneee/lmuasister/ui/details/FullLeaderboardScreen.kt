package com.orioooneee.lmuasister.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.LapEntry
import com.orioooneee.lmuasister.ui.components.LeaderboardRowSkeleton
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextLow
import com.orioooneee.lmuasister.ui.theme.TextMed
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.full_leaderboard_title
import lmuassister.shared.generated.resources.lb_end
import lmuassister.shared.generated.resources.lb_load_error
import lmuassister.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Full, cursor-paginated leaderboard for one race (Paging 3). */
@Composable
fun FullLeaderboardScreen(leaderboardId: String, title: String, onBack: () -> Unit) {
    val repo = koinInject<RaceRepository>()
    val entries = remember(leaderboardId) { repo.leaderboardPager(leaderboardId).flow }
        .collectAsLazyPagingItems()

    Column(Modifier.fillMaxSize().background(Carbon)) {
        // top bar
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton("‹", Modifier, onBack)
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.full_leaderboard_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMed,
                )
            }
        }
        // One leaderboard id is a single class, so a header derived from the first row
        // labels the whole board; gaps are measured against that overall fastest lap.
        val first = if (entries.itemCount > 0) entries.peek(0) else null
        val best = first?.bestLapMs ?: 0L
        first?.carClass?.let { cls ->
            Column(Modifier.padding(horizontal = 12.dp)) { ClassSectionHeader(cls) }
        }
        // The signed-in player's own row, pinned above the board (token-gated; null if
        // not signed in or they have no entry). Waits briefly for the token internally.
        val me by produceState<LapEntry?>(null, leaderboardId) {
            value = runCatching { repo.leaderboardMe(leaderboardId) }.getOrNull()
        }
        me?.let { m ->
            val accent = MaterialTheme.colorScheme.primary
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("YOUR POSITION", style = MaterialTheme.typography.labelSmall, color = TextMed, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                ) {
                    LeaderboardRow(m, best, alt = false)
                }
            }
        }
        val skeletonBrush = shimmerBrush()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            items(entries.itemCount) { index ->
                entries[index]?.let { e -> LeaderboardRow(e, best, alt = index % 2 == 1) }
            }

            when (val append = entries.loadState.append) {
                is LoadState.Loading -> items(2) { LeaderboardRowSkeleton(skeletonBrush) }
                is LoadState.Error -> item { FooterRetry { entries.retry() } }
                is LoadState.NotLoading ->
                    if (append.endOfPaginationReached && entries.itemCount > 0) item { FooterEnd() }
            }

            when (val refresh = entries.loadState.refresh) {
                is LoadState.Loading -> if (entries.itemCount == 0) items(12) { LeaderboardRowSkeleton(skeletonBrush) }
                is LoadState.Error -> if (entries.itemCount == 0) item { FooterRetry { entries.retry() } }
                else -> {}
            }
        }
    }
}

@Composable
private fun FooterRetry(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.lb_load_error), style = MaterialTheme.typography.bodyMedium, color = TextLow)
        Text(
            stringResource(Res.string.retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry).padding(8.dp),
        )
    }
}

@Composable
private fun FooterEnd() {
    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.lb_end), style = MaterialTheme.typography.labelSmall, color = TextLow)
    }
}
