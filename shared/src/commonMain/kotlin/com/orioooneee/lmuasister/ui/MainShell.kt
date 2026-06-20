package com.orioooneee.lmuasister.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.orioooneee.lmuasister.data.model.Race
import com.orioooneee.lmuasister.ui.components.EmptyState
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.details.FullLeaderboardScreen
import com.orioooneee.lmuasister.ui.details.RaceDetailsScreen
import com.orioooneee.lmuasister.ui.home.HomeScreen
import com.orioooneee.lmuasister.ui.legal.PrivacyPolicyScreen
import com.orioooneee.lmuasister.ui.profile.AllRacesScreen
import com.orioooneee.lmuasister.ui.profile.ProfileScreen
import com.orioooneee.lmuasister.ui.profile.RaceProfileDetailScreen
import com.orioooneee.lmuasister.ui.profile.SteamLoginViewModel
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlinx.serialization.Serializable
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.error_title
import lmuassister.shared.generated.resources.nav_profile
import lmuassister.shared.generated.resources.nav_schedule
import lmuassister.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object HomeRoute

@Serializable
object ProfileRoute

@Serializable
data class DetailsRoute(val raceId: String)

@Serializable
data class LeaderboardRoute(val leaderboardId: String, val title: String)

@Serializable
object AllRacesRoute

@Serializable
data class ProfileRaceDetailRoute(val eventId: String, val split: Int = -1)

@Serializable
object PrivacyRoute

/** Top-level tabs shown in the bottom navigation bar. */
private enum class TopTab(val icon: ImageVector) {
    Schedule(IconCalendarOutline),
    Profile(IconPersonOutline),
}

/**
 * App shell: a bottom-navigation bar switches between the Schedule and Profile
 * top-level tabs, with race details + leaderboard pushed onto the same back stack
 * (the bar hides on those detail screens).
 */
@Composable
fun MainShell(
    viewModel: ScheduleViewModel = koinViewModel(),
    profileViewModel: SteamLoginViewModel = koinViewModel(),
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    // Bottom bar only on the top-level destinations — hidden on details/leaderboard.
    val onTopLevel = currentDest?.hierarchy?.any {
        it.hasRoute(HomeRoute::class) || it.hasRoute(ProfileRoute::class)
    } == true
    val onProfile = currentDest?.hierarchy?.any { it.hasRoute(ProfileRoute::class) } == true

    // Kick a fresh schedule update once on launch (cache is already painted instantly
    // by the VM).
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Pushed (detail) screens slide up from the bottom and slide back down on close; the
    // nav bar slides down with them. Top-level tab switches are instant.
    val slideUp: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { slideInVertically(tween(280)) { it } }
    val slideDown: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { slideOutVertically(tween(280)) { it } }

    Scaffold(
        containerColor = Carbon,
        bottomBar = {
            AnimatedVisibility(
                visible = onTopLevel,
                enter = slideInVertically(tween(280)) { it },
                exit = slideOutVertically(tween(280)) { it },
            ) {
                BottomBar(
                    selected = if (onProfile) TopTab.Profile else TopTab.Schedule,
                    onSelect = { tab ->
                        val route = if (tab == TopTab.Profile) ProfileRoute else HomeRoute
                        nav.navigate(route) {
                            popUpTo(HomeRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { insets ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize().padding(insets),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable<HomeRoute> {
                ScheduleTab(viewModel, onOpenRace = { nav.navigate(DetailsRoute(it.id)) })
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onSeeAllRaces = { nav.navigate(AllRacesRoute) },
                    onOpenRace = { eventId, split ->
                        nav.navigate(ProfileRaceDetailRoute(eventId, split ?: -1))
                    },
                    onOpenPrivacy = { nav.navigate(PrivacyRoute) },
                )
            }
            composable<PrivacyRoute>(enterTransition = slideUp, popExitTransition = slideDown) {
                PrivacyPolicyScreen(onBack = { nav.popBackStack() })
            }
            composable<AllRacesRoute>(enterTransition = slideUp, popExitTransition = slideDown) {
                AllRacesScreen(
                    viewModel = profileViewModel,
                    onBack = { nav.popBackStack() },
                    onOpenRace = { eventId, split ->
                        nav.navigate(ProfileRaceDetailRoute(eventId, split ?: -1))
                    },
                )
            }
            composable<ProfileRaceDetailRoute>(enterTransition = slideUp, popExitTransition = slideDown) { entry ->
                val route = entry.toRoute<ProfileRaceDetailRoute>()
                RaceProfileDetailScreen(
                    viewModel = profileViewModel,
                    eventId = route.eventId,
                    split = route.split.takeIf { it >= 0 },
                    onBack = { nav.popBackStack() },
                )
            }
            composable<DetailsRoute>(enterTransition = slideUp, popExitTransition = slideDown) { entry ->
                val id = entry.toRoute<DetailsRoute>().raceId
                val state by viewModel.state.collectAsStateWithLifecycle()
                val race = (state as? ScheduleUiState.Success)?.data?.schedule?.races?.firstOrNull { it.id == id }
                if (race != null) {
                    RaceDetailsScreen(
                        race,
                        onBack = { nav.popBackStack() },
                        onOpenLeaderboard = { lbId, title ->
                            nav.navigate(LeaderboardRoute(lbId, title))
                        },
                    )
                }
            }
            composable<LeaderboardRoute>(enterTransition = slideUp, popExitTransition = slideDown) { entry ->
                val route = entry.toRoute<LeaderboardRoute>()
                FullLeaderboardScreen(
                    leaderboardId = route.leaderboardId,
                    title = route.title,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

/** Schedule tab: loading / error / the actual home screen, with pull-to-refresh. */
@Composable
private fun ScheduleTab(viewModel: ScheduleViewModel, onOpenRace: (Race) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val cars by viewModel.cars.collectAsStateWithLifecycle()

    when (val s = state) {
        is ScheduleUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

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
            RefreshableContent(refreshing, viewModel::refresh) {
                HomeScreen(
                    schedule = data.schedule,
                    weeks = data.weeks,
                    selectedWeek = data.selected,
                    onSelectWeek = viewModel::selectWeek,
                    onOpenRace = onOpenRace,
                    onRefresh = viewModel::refresh,
                    cars = cars,
                )
            }
        }
    }
}

@Composable
private fun BottomBar(selected: TopTab, onSelect: (TopTab) -> Unit) {
    NavigationBar(containerColor = Surface1) {
        TopTab.entries.forEach { tab ->
            val label = when (tab) {
                TopTab.Schedule -> stringResource(Res.string.nav_schedule)
                TopTab.Profile -> stringResource(Res.string.nav_profile)
            }
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = TextMed,
                    unselectedTextColor = TextMed,
                ),
            )
        }
    }
}
