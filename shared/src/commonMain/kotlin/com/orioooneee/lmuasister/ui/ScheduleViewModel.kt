package com.orioooneee.lmuasister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.analytics.TelemetryError
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.ui.util.weekKeyShort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeekTab(val key: String, val label: String)

data class ScheduleData(
    val schedule: Schedule,
    val weeks: List<WeekTab>,
    val selected: String,
)

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data class Success(val data: ScheduleData) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

private const val CURRENT = "__current__"

class ScheduleViewModel(
    private val repository: RaceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Car roster for the Home carousel — loaded independently of the schedule. */
    private val _cars = MutableStateFlow<List<CarModel>>(emptyList())
    val cars: StateFlow<List<CarModel>> = _cars.asStateFlow()

    /** Loaded schedules per week key — makes switching weeks instant. */
    private val cache = mutableMapOf<String, Schedule>()
    private var weeks: List<String> = emptyList()
    private var selected: String? = null

    init {
        // Cars: offline-first, independent of the schedule (static reference data).
        viewModelScope.launch {
            repository.cachedCars()?.let { _cars.value = it }
            repository.cars().getOrNull()?.let { _cars.value = it }
        }
        // Offline-first: paint instantly from the cached schedule (memory/disk), no network.
        // The network update is kicked from the Home screen's LaunchedEffect → refresh().
        viewModelScope.launch {
            repository.cachedWeeks()?.let { cachedKeys ->
                weeks = cachedKeys
                selected = cachedKeys.firstOrNull()
                fetchWeek(selected) // load() reads the cached response — instant
            }
        }
    }

    fun selectWeek(key: String) {
        if (key == selected) return
        selected = key
        val cached = cache[key]
        Telemetry.log(AnalyticsEvent.WeekSelected(key, isCached = cached != null))
        if (cached != null) {
            emitSuccess(key, cached) // instant — already prefetched
        } else {
            _refreshing.value = true
            viewModelScope.launch {
                fetchWeek(key)
                _refreshing.value = false
            }
        }
    }

    /** Launch update / pull-to-refresh — force-refetch the whole schedule from the backend. */
    fun refresh() {
        _refreshing.value = true
        viewModelScope.launch {
            if (repository.refreshSchedule().isSuccess) {
                weeks = repository.availableWeeks() // from the just-refreshed cache
                val sel = selected
                if (sel == null || sel !in weeks) selected = weeks.firstOrNull()
                cache.clear() // backend refetch updates every week — drop all stale copies
                fetchWeek(selected)
                prefetchOtherWeeks()
            } else if (_state.value !is ScheduleUiState.Success) {
                // nothing cached and the network failed → surface an error
                _state.value = ScheduleUiState.Error("Network error — could not load schedule")
                Telemetry.log(AnalyticsEvent.ScheduleError("network"))
                Telemetry.recordError(TelemetryError("schedule_refresh_failed"), "stage" to "refresh")
            }
            _refreshing.value = false
        }
    }

    private suspend fun fetchWeek(key: String?, refresh: Boolean = false) {
        repository.load(key, refresh)
            .onSuccess { schedule ->
                cache[key ?: CURRENT] = schedule
                emitSuccess(key, schedule)
            }
            .onFailure {
                Telemetry.recordError(it, "stage" to "fetch_week", "week" to (key ?: "current"))
                if (_state.value !is ScheduleUiState.Success) {
                    _state.value = ScheduleUiState.Error(it.message ?: "Network error — could not load schedule")
                    Telemetry.log(AnalyticsEvent.ScheduleError("network"))
                }
            }
    }

    /** Warm the cache for every other week in the background so switching is instant. */
    private suspend fun prefetchOtherWeeks() {
        weeks.forEach { week ->
            if (cache[week] == null) {
                runCatching { repository.load(week) }.getOrNull()?.getOrNull()?.let { cache[week] = it }
            }
        }
    }

    private fun emitSuccess(key: String?, schedule: Schedule) {
        // Cache the (working) schedule track logos so the profile can fall back to them.
        TrackLogoIndex.populate(
            schedule.races.flatMap { r ->
                listOf(r.track?.name to r.track?.logoUrl, r.circuit to r.track?.logoUrl)
            },
        )
        val sel = key ?: weeks.firstOrNull() ?: ""
        _state.value = ScheduleUiState.Success(
            ScheduleData(
                schedule = schedule,
                weeks = weeks.mapIndexed { i, k -> WeekTab(k, weekLabel(i, k)) },
                selected = sel,
            ),
        )
    }

    private fun weekLabel(index: Int, key: String): String = when (index) {
        0 -> "This week"
        1 -> "Next week"
        else -> weekKeyShort(key)
    }
}
