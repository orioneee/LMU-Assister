package com.orioooneee.lmuasister.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavDestination
import androidx.navigation.toRoute
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
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
import com.orioooneee.lmuasister.ui.profile.SuspensionsScreen
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
data class SuspensionsRoute(val active: Boolean)

@Serializable
data class ProfileRaceDetailRoute(val eventId: String, val split: Int = -1)

@Serializable
object PrivacyRoute

private const val NAV_ANIM = 300

private enum class TopTab(val icon: ImageVector) {
    Schedule(IconCalendarOutline),
    Profile(IconPersonOutline),
}

@Composable
fun MainShell(
    viewModel: ScheduleViewModel = koinViewModel(),
    profileViewModel: SteamLoginViewModel = koinViewModel(),
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    val onTopLevel = currentDest?.hierarchy?.any {
        it.hasRoute(HomeRoute::class) || it.hasRoute(ProfileRoute::class)
    } == true
    val onProfile = currentDest?.hierarchy?.any { it.hasRoute(ProfileRoute::class) } == true

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(currentDest) {
        currentDest?.let { Telemetry.screen(screenNameOf(it)) }
    }

    val enterUp: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { slideInVertically(tween(NAV_ANIM, easing = FastOutSlowInEasing)) { it } }
    val exitFade: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(tween(NAV_ANIM)) }
    val popEnterFade: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(tween(NAV_ANIM)) }
    val popExitDown: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { slideOutVertically(tween(NAV_ANIM, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(NAV_ANIM)) }

    Scaffold(
        containerColor = Carbon,
        bottomBar = {
            if (onTopLevel) {
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
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable<HomeRoute> {
                ScheduleTab(viewModel, insets, onOpenRace = {
                    Telemetry.log(AnalyticsEvent.RaceDetailOpened(it.id, source = "home_grid"))
                    nav.navigate(DetailsRoute(it.id))
                })
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    onSeeAllRaces = {
                        Telemetry.log(AnalyticsEvent.AllRacesOpened)
                        nav.navigate(AllRacesRoute)
                    },
                    onOpenRace = { eventId, split ->
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "profile_recent"))
                        nav.navigate(ProfileRaceDetailRoute(eventId, split ?: -1))
                    },
                    onOpenSuspensions = { active ->
                        Telemetry.log(AnalyticsEvent.SuspensionsOpened(active))
                        nav.navigate(SuspensionsRoute(active))
                    },
                    onOpenPrivacy = {
                        Telemetry.log(AnalyticsEvent.PrivacyOpened)
                        nav.navigate(PrivacyRoute)
                    },
                )
            }
            composable<SuspensionsRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                SuspensionsScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    active = entry.toRoute<SuspensionsRoute>().active,
                    onBack = { nav.popBackStack() },
                )
            }
            composable<PrivacyRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) {
                PrivacyPolicyScreen(insets = insets, onBack = { nav.popBackStack() })
            }
            composable<AllRacesRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) {
                AllRacesScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                    onOpenRace = { eventId, split ->
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "all_races"))
                        nav.navigate(ProfileRaceDetailRoute(eventId, split ?: -1))
                    },
                )
            }
            composable<ProfileRaceDetailRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<ProfileRaceDetailRoute>()
                RaceProfileDetailScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    eventId = route.eventId,
                    split = route.split.takeIf { it >= 0 },
                    onBack = { nav.popBackStack() },
                )
            }
            composable<DetailsRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val id = entry.toRoute<DetailsRoute>().raceId
                val state by viewModel.state.collectAsStateWithLifecycle()
                val race = (state as? ScheduleUiState.Success)?.data?.schedule?.races?.firstOrNull { it.id == id }
                if (race != null) {
                    RaceDetailsScreen(
                        race,
                        insets = insets,
                        onBack = { nav.popBackStack() },
                        onOpenLeaderboard = { lbId, title ->
                            Telemetry.log(AnalyticsEvent.LeaderboardOpened(lbId))
                            nav.navigate(LeaderboardRoute(lbId, title))
                        },
                    )
                }
            }
            composable<LeaderboardRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<LeaderboardRoute>()
                FullLeaderboardScreen(
                    leaderboardId = route.leaderboardId,
                    title = route.title,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun ScheduleTab(viewModel: ScheduleViewModel, insets: PaddingValues, onOpenRace: (Race) -> Unit) {
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
            RefreshableContent(refreshing, viewModel::refresh, topInset = insets.calculateTopPadding()) {
                HomeScreen(
                    schedule = data.schedule,
                    weeks = data.weeks,
                    selectedWeek = data.selected,
                    insets = insets,
                    onSelectWeek = viewModel::selectWeek,
                    onOpenRace = onOpenRace,
                    onRefresh = viewModel::refresh,
                    cars = cars,
                )
            }
        }
    }
}

private fun screenNameOf(dest: NavDestination): String = when {
    dest.hasRoute(HomeRoute::class) -> "schedule"
    dest.hasRoute(ProfileRoute::class) -> "profile"
    dest.hasRoute(DetailsRoute::class) -> "race_details"
    dest.hasRoute(LeaderboardRoute::class) -> "full_leaderboard"
    dest.hasRoute(AllRacesRoute::class) -> "all_races"
    dest.hasRoute(SuspensionsRoute::class) -> "suspensions"
    dest.hasRoute(ProfileRaceDetailRoute::class) -> "race_history_detail"
    dest.hasRoute(PrivacyRoute::class) -> "privacy_policy"
    else -> "unknown"
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
