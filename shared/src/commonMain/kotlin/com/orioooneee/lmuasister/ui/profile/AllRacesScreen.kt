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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.ui.components.RaceRowSkeleton
import com.orioooneee.lmuasister.ui.components.shimmerBrush
import com.orioooneee.lmuasister.ui.details.CircleButton
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextMed

/** Items past the last visible row to keep buffered (≈ 3 pages of 5). */
private const val PREFETCH_AHEAD = 15

/**
 * The player's full race history — pages of 5 pulled from GET /profile/races as the user
 * scrolls. Pagination state lives in [SteamLoginViewModel] so it survives opening a race
 * detail and coming back (the screen's own state would be dropped from the back stack).
 */
@Composable
fun AllRacesScreen(
    viewModel: SteamLoginViewModel,
    insets: PaddingValues,
    onBack: () -> Unit,
    onOpenRace: (eventId: String, split: Int?) -> Unit,
) {
    val state by viewModel.allRacesState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // First load (only if we don't already have pages cached in the VM) + infinite scroll.
    LaunchedEffect(Unit) { if (state.races.isEmpty()) viewModel.loadMoreAllRaces() }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - PREFETCH_AHEAD && state.error == null) viewModel.loadMoreAllRaces()
        }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp + insets.calculateTopPadding(), end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleButton(Modifier, onBack)
            Text(
                "All races",
                style = MaterialTheme.typography.titleLarge,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }

        val brush = shimmerBrush()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + insets.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.races) { race ->
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
                state.races.isEmpty() && state.loading -> items(7) { RaceRowSkeleton(brush) }
                state.loading -> item { RaceRowSkeleton(brush) }
                state.error != null -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            state.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.retryAllRaces() }
                                .padding(8.dp),
                        )
                    }
                }
                state.races.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("No races yet", style = MaterialTheme.typography.bodyMedium, color = TextMed)
                    }
                }
            }
        }
    }
}
