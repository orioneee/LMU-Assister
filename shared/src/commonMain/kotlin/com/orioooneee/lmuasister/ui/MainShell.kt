package com.orioooneee.lmuasister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.details.RaceDetailsScreen
import com.orioooneee.lmuasister.ui.home.HomeScreen
import com.orioooneee.lmuasister.ui.schedule.ScheduleScreen
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Surface1
import org.koin.compose.viewmodel.koinViewModel

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", IconHome),
    SCHEDULE("Schedule", IconSchedule),
}

@Composable
fun MainShell(viewModel: ScheduleViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var selected by remember { mutableStateOf<Race?>(null) }

    // Details takes over the whole screen (with its own back).
    selected?.let { race ->
        RaceDetailsScreen(race, onBack = { selected = null })
        return
    }

    Scaffold(
        containerColor = Carbon,
        bottomBar = {
            NavigationBar(containerColor = Surface1) {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
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

                is ScheduleUiState.Success -> {
                    val data = s.data
                    when (tab) {
                        MainTab.HOME -> RefreshableContent(refreshing, viewModel::refresh) {
                            HomeScreen(
                                schedule = data.schedule,
                                weeks = data.weeks,
                                selectedWeek = data.selected,
                                onSelectWeek = viewModel::selectWeek,
                                onOpenRace = { selected = it },
                            )
                        }
                        MainTab.SCHEDULE -> RefreshableContent(refreshing, viewModel::refresh) {
                            ScheduleScreen(
                                schedule = data.schedule,
                                weeks = data.weeks,
                                selectedWeek = data.selected,
                                onSelectWeek = viewModel::selectWeek,
                                onOpenRace = { selected = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
