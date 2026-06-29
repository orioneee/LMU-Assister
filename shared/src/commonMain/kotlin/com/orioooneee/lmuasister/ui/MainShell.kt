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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.orioooneee.lmuasister.data.remote.CarDetailedDto
import com.orioooneee.lmuasister.data.remote.displayId
import com.orioooneee.lmuasister.ui.cars.CarDetailScreen
import com.orioooneee.lmuasister.ui.cars.CarsScreen
import com.orioooneee.lmuasister.ui.components.EmptyState
import com.orioooneee.lmuasister.ui.components.RefreshableContent
import com.orioooneee.lmuasister.ui.details.FullLeaderboardScreen
import com.orioooneee.lmuasister.ui.details.RaceDetailsScreen
import com.orioooneee.lmuasister.ui.home.HomeScreen
import com.orioooneee.lmuasister.ui.legal.PrivacyPolicyScreen
import com.orioooneee.lmuasister.ui.profile.AllRacesScreen
import com.orioooneee.lmuasister.ui.profile.CategoryRacesScreen
import com.orioooneee.lmuasister.ui.profile.ProfileScreen
import com.orioooneee.lmuasister.ui.profile.PublicRaceProfileDetailScreen
import com.orioooneee.lmuasister.ui.profile.RaceProfileDetailScreen
import com.orioooneee.lmuasister.ui.profile.SteamLoginViewModel
import com.orioooneee.lmuasister.ui.profile.SuspensionsScreen
import com.orioooneee.lmuasister.ui.profile.TrackBreakdownScreen
import com.orioooneee.lmuasister.ui.publicusers.PublicUserCategoryRacesScreen
import com.orioooneee.lmuasister.ui.publicusers.PublicUserDetailScreen
import com.orioooneee.lmuasister.ui.publicusers.PublicUserRacesScreen
import com.orioooneee.lmuasister.ui.publicusers.PublicUserTrackBreakdownScreen
import com.orioooneee.lmuasister.ui.publicusers.PublicUsersScreen
import com.orioooneee.lmuasister.ui.tracks.PublicTrackDetailScreen
import com.orioooneee.lmuasister.ui.tracks.TrackDetailScreen
import com.orioooneee.lmuasister.ui.tracks.TracksScreen
import com.orioooneee.lmuasister.ui.theme.Carbon
import com.orioooneee.lmuasister.ui.theme.Surface1
import com.orioooneee.lmuasister.ui.theme.TextMed
import kotlinx.serialization.Serializable
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.error_title
import lmuassister.shared.generated.resources.nav_cars
import lmuassister.shared.generated.resources.nav_profile
import lmuassister.shared.generated.resources.nav_drivers
import lmuassister.shared.generated.resources.nav_schedule
import lmuassister.shared.generated.resources.nav_tracks
import lmuassister.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object HomeRoute

@Serializable
object ProfileRoute

@Serializable
object PublicUsersRoute

@Serializable
object TracksRoute

@Serializable
object CarsRoute

@Serializable
data class TrackDetailRoute(val trackId: String)

@Serializable
data class CarDetailRoute(val carId: String)

@Serializable
data class DetailsRoute(val raceId: String)
@Serializable
data class LeaderboardRoute(val leaderboardId: String, val title: String)

@Serializable
object AllRacesRoute

@Serializable
data class CategoryRacesRoute(val category: String, val title: String)

@Serializable
data class SuspensionsRoute(val active: Boolean)

@Serializable
object TrackBreakdownRoute

@Serializable
data class ProfileRaceDetailRoute(val eventId: String, val split: Int = -1)

@Serializable
data class PublicUserDetailRoute(val uid: String)

@Serializable
data class PublicUserRacesRoute(val uid: String)

@Serializable
data class PublicUserCategoryRacesRoute(val uid: String, val category: String, val title: String)

@Serializable
data class PublicUserTrackBreakdownRoute(val uid: String)

@Serializable
data class PublicUserTrackDetailRoute(val uid: String, val trackId: String)

@Serializable
data class PublicUserRaceDetailRoute(val uid: String, val eventId: String, val split: Int = -1)

@Serializable
object PrivacyRoute

private const val NAV_ANIM = 300

private enum class TopTab(val icon: ImageVector) {
    Schedule(IconCalendarOutline),
    Tracks(IconFlag),
    Cars(IconCar),
    Drivers(IconGroupOutline),
    Profile(IconPerson),
}

@Composable
fun MainShell(
    viewModel: ScheduleViewModel = koinViewModel(),
    profileViewModel: SteamLoginViewModel = koinViewModel(),
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    var selectedCar by remember { mutableStateOf<CarDetailedDto?>(null) }

    val onTopLevel = currentDest?.hierarchy?.any {
        it.hasRoute(HomeRoute::class) || it.hasRoute(PublicUsersRoute::class) ||
            it.hasRoute(TracksRoute::class) || it.hasRoute(CarsRoute::class) || it.hasRoute(ProfileRoute::class)
    } == true
    val selectedTab = when {
        currentDest?.hierarchy?.any { it.hasRoute(ProfileRoute::class) } == true -> TopTab.Profile
        currentDest?.hierarchy?.any { it.hasRoute(PublicUsersRoute::class) } == true -> TopTab.Drivers
        currentDest?.hierarchy?.any { it.hasRoute(CarsRoute::class) } == true -> TopTab.Cars
        currentDest?.hierarchy?.any { it.hasRoute(TracksRoute::class) } == true -> TopTab.Tracks
        else -> TopTab.Schedule
    }

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

                    selected = selectedTab,
                    onSelect = { tab ->
                        val route: Any = when (tab) {
                            TopTab.Profile -> ProfileRoute
                            TopTab.Drivers -> PublicUsersRoute
                            TopTab.Cars -> CarsRoute
                            TopTab.Tracks -> TracksRoute
                            TopTab.Schedule -> HomeRoute
                        }
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
                    onOpenCategory = { category ->
                        Telemetry.log(AnalyticsEvent.CategoryRacesOpened(category.key))
                        nav.navigate(CategoryRacesRoute(category.key, category.title))
                    },
                    onOpenPrivacy = {
                        Telemetry.log(AnalyticsEvent.PrivacyOpened)
                        nav.navigate(PrivacyRoute)
                    },
                    onOpenTracks = { nav.navigate(TrackBreakdownRoute) },
                    onOpenCar = { car ->
                        selectedCar = car
                        nav.navigate(CarDetailRoute(car.displayId()))
                    },
                )
            }
            composable<PublicUsersRoute> {
                PublicUsersScreen(
                    insets = insets,
                    onOpenUser = { uid -> nav.navigate(PublicUserDetailRoute(uid)) },
                )
            }
            composable<PublicUserDetailRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                PublicUserDetailScreen(
                    uid = entry.toRoute<PublicUserDetailRoute>().uid,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                    onSeeAllRaces = {
                        val uid = entry.toRoute<PublicUserDetailRoute>().uid
                        Telemetry.log(AnalyticsEvent.PublicAllRacesOpened)
                        nav.navigate(PublicUserRacesRoute(uid))
                    },
                    onOpenCategory = { category ->
                        val uid = entry.toRoute<PublicUserDetailRoute>().uid
                        Telemetry.log(AnalyticsEvent.PublicCategoryRacesOpened(category.key))
                        nav.navigate(PublicUserCategoryRacesRoute(uid, category.key, category.title))
                    },
                    onOpenTracks = {
                        val uid = entry.toRoute<PublicUserDetailRoute>().uid
                        Telemetry.log(AnalyticsEvent.PublicTrackBreakdownOpened)
                        nav.navigate(PublicUserTrackBreakdownRoute(uid))
                    },
                    onOpenRace = { eventId, split ->
                        val uid = entry.toRoute<PublicUserDetailRoute>().uid
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "public_profile_recent"))
                        nav.navigate(PublicUserRaceDetailRoute(uid, eventId, split ?: -1))
                    },
                    onOpenCar = { car ->
                        selectedCar = car
                        nav.navigate(CarDetailRoute(car.displayId()))
                    },
                )
            }
            composable<PublicUserRacesRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<PublicUserRacesRoute>()
                PublicUserRacesScreen(
                    uid = route.uid,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                    onOpenRace = { eventId, split ->
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "public_all_races"))
                        nav.navigate(PublicUserRaceDetailRoute(route.uid, eventId, split ?: -1))
                    },
                )
            }
            composable<PublicUserCategoryRacesRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<PublicUserCategoryRacesRoute>()
                PublicUserCategoryRacesScreen(
                    uid = route.uid,
                    category = route.category,
                    title = route.title,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                    onOpenRace = { eventId, split ->
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "public_category_races"))
                        nav.navigate(PublicUserRaceDetailRoute(route.uid, eventId, split ?: -1))
                    },
                )
            }
            composable<PublicUserTrackBreakdownRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<PublicUserTrackBreakdownRoute>()
                PublicUserTrackBreakdownScreen(
                    uid = route.uid,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                    onOpenTrack = { trackId ->
                        Telemetry.log(AnalyticsEvent.PublicTrackOpened(trackId))
                        nav.navigate(PublicUserTrackDetailRoute(route.uid, trackId))
                    },
                )
            }
            composable<PublicUserTrackDetailRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<PublicUserTrackDetailRoute>()
                PublicTrackDetailScreen(
                    uid = route.uid,
                    trackId = route.trackId,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                )
            }
            composable<PublicUserRaceDetailRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<PublicUserRaceDetailRoute>()
                PublicRaceProfileDetailScreen(
                    uid = route.uid,
                    eventId = route.eventId,
                    split = route.split.takeIf { it >= 0 },
                    insets = insets,
                    onBack = { nav.popBackStack() },
                )
            }
            composable<TracksRoute> {
                TracksScreen(
                    insets = insets,
                    onOpenTrack = { trackId -> nav.navigate(TrackDetailRoute(trackId)) },
                )
            }
            composable<CarsRoute> {
                CarsScreen(
                    insets = insets,
                    onOpenCar = { car ->
                        selectedCar = car
                        nav.navigate(CarDetailRoute(car.displayId()))
                    },
                )
            }
            composable<CarDetailRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<CarDetailRoute>()
                val initial = selectedCar?.takeIf { it.id == route.carId || it.slug == route.carId || it.displayId() == route.carId }
                CarDetailScreen(
                    carId = route.carId,
                    initialCar = initial,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                )
            }
            composable<TrackDetailRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                TrackDetailScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    trackId = entry.toRoute<TrackDetailRoute>().trackId,
                    onBack = { nav.popBackStack() },
                    onOpenRace = { eventId, split ->
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "track_detail"))
                        nav.navigate(ProfileRaceDetailRoute(eventId, split ?: -1))
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
            composable<TrackBreakdownRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) {
                TrackBreakdownScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    onBack = { nav.popBackStack() },
                    onOpenTrack = { trackId -> nav.navigate(TrackDetailRoute(trackId)) },
                )
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
            composable<CategoryRacesRoute>(enterTransition = enterUp, exitTransition = exitFade, popEnterTransition = popEnterFade, popExitTransition = popExitDown) { entry ->
                val route = entry.toRoute<CategoryRacesRoute>()
                CategoryRacesScreen(
                    viewModel = profileViewModel,
                    insets = insets,
                    category = route.category,
                    title = route.title,
                    onBack = { nav.popBackStack() },
                    onOpenRace = { eventId, split ->
                        Telemetry.log(AnalyticsEvent.RaceDetailOpened(eventId, source = "category_races"))
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
    dest.hasRoute(PublicUsersRoute::class) -> "drivers"
    dest.hasRoute(PublicUserDetailRoute::class) -> "public_profile"
    dest.hasRoute(PublicUserRacesRoute::class) -> "public_all_races"
    dest.hasRoute(PublicUserCategoryRacesRoute::class) -> "public_category_races"
    dest.hasRoute(PublicUserTrackBreakdownRoute::class) -> "public_track_breakdown"
    dest.hasRoute(PublicUserTrackDetailRoute::class) -> "public_track_detail"
    dest.hasRoute(PublicUserRaceDetailRoute::class) -> "public_race_detail"
    dest.hasRoute(TracksRoute::class) -> "tracks"
    dest.hasRoute(TrackDetailRoute::class) -> "track_detail"
    dest.hasRoute(CarsRoute::class) -> "cars"
    dest.hasRoute(CarDetailRoute::class) -> "car_detail"
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
    NavigationBar(
        containerColor = Surface1
    ) {
        TopTab.entries.forEach { tab ->
            val label = when (tab) {
                TopTab.Schedule -> stringResource(Res.string.nav_schedule)
                TopTab.Drivers -> stringResource(Res.string.nav_drivers)
                TopTab.Cars -> stringResource(Res.string.nav_cars)
                TopTab.Tracks -> stringResource(Res.string.nav_tracks)
                TopTab.Profile -> stringResource(Res.string.nav_profile)
            }
            NavigationBarItem(
                alwaysShowLabel = false,
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
