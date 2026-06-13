package com.orioooneee.lmuasister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.orioooneee.lmuasister.ui.components.EmptyState
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.details.RaceDetailsScreen
import com.orioooneee.lmuasister.ui.home.HomeScreen
import com.orioooneee.lmuasister.ui.theme.Carbon
import kotlinx.serialization.Serializable
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.error_title
import lmuassister.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object HomeRoute

@Serializable
data class DetailsRoute(val raceId: String)

/** Single-screen app with a NavHost (Home ⇄ race details + a real back stack). */
@Composable
fun MainShell(viewModel: ScheduleViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    Scaffold(containerColor = Carbon) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            when (val s = state) {
                is ScheduleUiState.Loading -> Center { CircularProgressIndicator() }

                is ScheduleUiState.Error -> EmptyState(
                    title = stringResource(Res.string.error_title),
                    subtitle = s.message,
                    accent = MaterialTheme.colorScheme.error,
                    actionLabel = stringResource(Res.string.retry),
                    onAction = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                )

                is ScheduleUiState.Success -> {
                    val data = s.data
                    NavHost(navController = nav, startDestination = HomeRoute) {
                        composable<HomeRoute> {
                            RefreshableContent(refreshing, viewModel::refresh) {
                                HomeScreen(
                                    schedule = data.schedule,
                                    weeks = data.weeks,
                                    selectedWeek = data.selected,
                                    onSelectWeek = viewModel::selectWeek,
                                    onOpenRace = { nav.navigate(DetailsRoute(it.id)) },
                                    onRefresh = viewModel::refresh,
                                )
                            }
                        }
                        composable<DetailsRoute> { entry ->
                            val id = entry.toRoute<DetailsRoute>().raceId
                            val race = data.schedule.races.firstOrNull { it.id == id }
                            if (race != null) {
                                RaceDetailsScreen(race, onBack = { nav.popBackStack() })
                            }
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
