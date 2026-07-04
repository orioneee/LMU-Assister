package com.orioooneee.lmuasister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.analytics.TelemetryError
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.featureflags.FeatureFlags
import com.orioooneee.lmuasister.featureflags.FeatureFlagsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class WeekTab(val key: String, val label: String)

data class ScheduleData(
    val schedule: Schedule,
    val weeks: List<WeekTab>,
    val selected: String,
    val featureFlags: FeatureFlags,
)

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data class Success(val data: ScheduleData) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

private const val CURRENT = "__current__"

class ScheduleViewModel(
    private val repository: RaceRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _cars = MutableStateFlow<List<CarModel>>(emptyList())
    val cars: StateFlow<List<CarModel>> = _cars.asStateFlow()

    private val cache = mutableMapOf<String, Schedule>()
    private var weeks: List<WeekTab> = emptyList()
    private var selected: String? = null

    init {
        viewModelScope.launch {
            featureFlagsRepository.flags.collectLatest { flags ->
                val current = _state.value as? ScheduleUiState.Success ?: return@collectLatest
                _state.value = current.copy(data = current.data.copy(featureFlags = flags))
            }
        }
        viewModelScope.launch {
            featureFlagsRepository.refresh()
        }
        viewModelScope.launch {
            repository.cachedCars()?.let { _cars.value = it }
            repository.cars().getOrNull()?.let { _cars.value = it }
        }
        viewModelScope.launch {
            repository.cachedWeeks()?.let { cachedWeeks ->
                weeks = cachedWeeks.map { WeekTab(it.key, it.label.ifBlank { it.key }) }
                selected = weeks.firstOrNull()?.key
                fetchWeek(selected)
            }
        }
    }

    fun selectWeek(key: String) {
        if (key == selected) return
        selected = key
        val cached = cache[key]
        Telemetry.log(AnalyticsEvent.WeekSelected(key, isCached = cached != null))
        if (cached != null) {
            emitSuccess(key, cached)
        } else {
            _refreshing.value = true
            viewModelScope.launch {
                fetchWeek(key)
                _refreshing.value = false
            }
        }
    }

    fun refresh() {
        _refreshing.value = true
        viewModelScope.launch {
            if (repository.refreshSchedule().isSuccess) {
                weeks = repository.availableWeeks().map { WeekTab(it.key, it.label.ifBlank { it.key }) }
                val sel = selected
                if (sel == null || weeks.none { it.key == sel }) selected = weeks.firstOrNull()?.key
                cache.clear()
                fetchWeek(selected)
                prefetchOtherWeeks()
            } else if (_state.value !is ScheduleUiState.Success) {
                _state.value = ScheduleUiState.Error("Network error - could not load schedule")
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
                    _state.value = ScheduleUiState.Error(it.message ?: "Network error - could not load schedule")
                    Telemetry.log(AnalyticsEvent.ScheduleError("network"))
                }
            }
    }

    private suspend fun prefetchOtherWeeks() {
        weeks.forEach { week ->
            if (cache[week.key] == null) {
                runCatching { repository.load(week.key) }.getOrNull()?.getOrNull()?.let { cache[week.key] = it }
            }
        }
    }

    private fun emitSuccess(key: String?, schedule: Schedule) {
        TrackLogoIndex.populate(
            schedule.races.flatMap { r ->
                listOf(r.track?.name to r.track?.logoUrl, r.circuit to r.track?.logoUrl)
            },
        )
        val sel = key ?: weeks.firstOrNull()?.key ?: ""
        _state.value = ScheduleUiState.Success(
            ScheduleData(
                schedule = schedule,
                weeks = weeks,
                selected = sel,
                featureFlags = featureFlagsRepository.flags.value,
            ),
        )
    }
}
