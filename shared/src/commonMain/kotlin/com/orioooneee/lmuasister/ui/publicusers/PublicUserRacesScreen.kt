package com.orioooneee.lmuasister.ui.publicusers

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
import com.orioooneee.lmuasister.ui.profile.RaceHistoryRow
import com.orioooneee.lmuasister.ui.theme.Amber
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.TextHigh
import com.orioooneee.lmuasister.ui.theme.TextMed
import org.koin.compose.viewmodel.koinViewModel

private const val PREFETCH_AHEAD = 15

@Composable
fun PublicUserRacesScreen(
    uid: String,
    viewModel: PublicUsersViewModel = koinViewModel(),
    insets: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.races.collectAsStateWithLifecycle()
    val current = state.takeIf { it.uid == uid } ?: PublicUserRacesState(uid = uid, loading = true)
    val listState = rememberLazyListState()

    LaunchedEffect(uid) { viewModel.openRaces(uid) }
    LaunchedEffect(listState, uid, current.races.size, current.hasMore, current.loading, current.error) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - PREFETCH_AHEAD && current.error == null) {
                viewModel.loadMoreRaces(uid)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = 12.dp,
                top = 12.dp + insets.calculateTopPadding(),
                end = 12.dp,
                bottom = 12.dp,
            ),
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
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + insets.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(current.races) { race ->
                Box(Modifier.clip(RoundedCornerShape(12.dp))) {
                    RaceHistoryRow(race)
                }
            }
            when {
                current.races.isEmpty() && current.loading -> items(7) { RaceRowSkeleton(brush) }
                current.loading -> item { RaceRowSkeleton(brush) }
                current.error != null -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            current.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.retryRaces() }
                                .padding(8.dp),
                        )
                    }
                }
                current.races.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("No races yet", style = MaterialTheme.typography.bodyMedium, color = TextMed)
                    }
                }
            }
        }
    }
}
