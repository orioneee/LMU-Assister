package com.orioooneee.lmuasister.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
import com.orioooneee.lmuasister.ui.components.RaceRowSkeleton
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlinx.coroutines.launch

/** Items past the last visible row to keep buffered (≈ 3 pages of 5). */
private const val PREFETCH_AHEAD = 15

/**
 * The player's full race history — pages of 5 pulled from GET /profile/races as the
 * user scrolls (infinite scroll). Tapping a race opens its detail page.
 */
@Composable
fun AllRacesScreen(
    viewModel: SteamLoginViewModel,
    onBack: () -> Unit,
    onOpenRace: (eventId: String, split: Int?) -> Unit,
) {
    val races = remember { mutableStateListOf<RecentRaceDto>() }
    var page by remember { mutableStateOf(0) }        // last page loaded (0 = none yet)
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    suspend fun loadNext() {
        if (loading || !hasMore) return
        loading = true
        error = null
        val next = page + 1
        runCatching { viewModel.racesPage(next) }
            .onSuccess { p ->
                races.addAll(p.races)
                page = next
                hasMore = p.hasMore && p.races.isNotEmpty()
            }
            .onFailure { error = it.message ?: "Couldn't load races" }
        loading = false
    }

    // Aggressive prefetch: keep ~PREFETCH_AHEAD items (several pages) buffered past the
    // last visible row so the loader is rarely seen. The collector re-fires after each
    // page lands (totalItemsCount grows), so it self-chains until the buffer is full.
    LaunchedEffect(Unit) { loadNext() }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - PREFETCH_AHEAD && error == null) loadNext()
        }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton("‹", Modifier, onBack)
            Text(
                "All races",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (races.isNotEmpty()) {
                Text(
                    "${races.size}${if (hasMore) "+" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMed,
                )
            }
        }

        val brush = shimmerBrush()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(races) { race ->
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = race.eventId != null) {
                            race.eventId?.let { onOpenRace(it, race.split) }
                        },
                ) {
                    RaceHistoryRow(race)
                }
            }
            when {
                // Initial load → a screen of skeleton rows.
                races.isEmpty() && loading -> items(7) { RaceRowSkeleton(brush) }
                // Loading the next page → one skeleton row as the footer.
                loading -> item { RaceRowSkeleton(brush) }
                error != null -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { error = null; scope.launch { loadNext() } }
                                .padding(8.dp),
                        )
                    }
                }
                races.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("No races yet", style = MaterialTheme.typography.bodyMedium, color = TextMed)
                    }
                }
            }
        }
    }
}
