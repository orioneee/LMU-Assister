package com.orioooneee.lmuasister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.details.RaceDetailsScreen
import com.orioooneee.lmuasister.ui.home.HomeScreen
import com.orioooneee.lmuasister.ui.theme.Carbon
import org.koin.compose.viewmodel.koinViewModel

/** Single-screen app: everything lives on Home (week picker + tier pager + all categories). */
@Composable
fun MainShell(viewModel: ScheduleViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Race?>(null) }

    selected?.let { race ->
        RaceDetailsScreen(race, onBack = { selected = null })
        return
    }

    Scaffold(containerColor = Carbon) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScheduleUiState.Loading -> Center { CircularProgressIndicator() }

                is ScheduleUiState.Error -> Center {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌ ${s.message}", color = MaterialTheme.colorScheme.error)
                        Box(Modifier.height(12.dp))
                        Button(onClick = viewModel::refresh) { Text("Retry") }
                    }
                }

                is ScheduleUiState.Success -> RefreshableContent(refreshing, viewModel::refresh) {
                    HomeScreen(
                        schedule = s.data.schedule,
                        weeks = s.data.weeks,
                        selectedWeek = s.data.selected,
                        onSelectWeek = viewModel::selectWeek,
                        onOpenRace = { selected = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
