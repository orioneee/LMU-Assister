package com.orioooneee.lmuasister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.analytics.TelemetryError
import com.orioooneee.lmuasister.data.RaceRepository
import com.orioooneee.lmuasister.data.model.CarModel
import com.orioooneee.lmuasister.data.model.Schedule
import com.orioooneee.lmuasister.data.model.ScheduleCategory
import com.orioooneee.lmuasister.data.model.SchedulePeriod
import com.orioooneee.lmuasister.data.model.ScheduleSlice
import com.orioooneee.lmuasister.featureflags.FeatureFlags
import com.orioooneee.lmuasister.featureflags.FeatureFlagsRepository
import kotlinx.coroutines.Job
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
    val selectedCategory: ScheduleCategory,
    val loading: Boolean,
    val errorMessage: String? = null,
    val featureFlags: FeatureFlags,
)

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data class Success(val data: ScheduleData) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

private const val CURRENT_WEEK = "current"
private const val NEXT_WEEK = "next"

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

    private var weeks: List<WeekTab> = defaultWeeks()
    private var selectedPeriod = SchedulePeriod.CURRENT
    private var selectedCategory = ScheduleCategory.RACES
    private var loadJob: Job? = null

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
        repository.cachedSchedule(selectedPeriod, selectedCategory)
            ?.let { emitSuccess(selectedPeriod, selectedCategory, it) }
    }

    fun selectWeek(key: String) {
        val period = key.toSchedulePeriod() ?: return
        if (period == selectedPeriod) return
        val cached = repository.cachedSchedule(period, selectedCategory)
        selectedPeriod = period
        Telemetry.log(AnalyticsEvent.WeekSelected(key, isCached = cached != null))
        loadSelected(refresh = false, cached = cached)
    }

    fun selectCategory(category: ScheduleCategory) {
        if (category == selectedCategory) return
        selectedCategory = category
        loadSelected(refresh = false, cached = repository.cachedSchedule(selectedPeriod, category))
    }

    fun refresh() {
        loadSelected(refresh = true)
    }

    private fun loadSelected(refresh: Boolean, cached: ScheduleSlice? = null) {
        loadJob?.cancel()
        val period = selectedPeriod
        val category = selectedCategory

        if (!refresh && cached != null) {
            _refreshing.value = false
            emitSuccess(period, category, cached)
            return
        }

        if (!refresh) {
            emitLoading(period, category)
        }
        _refreshing.value = refresh

        loadJob = viewModelScope.launch {
            repository.loadSchedule(period, category, refresh = refresh)
                .onSuccess { slice ->
                    if (period == selectedPeriod && category == selectedCategory) {
                        emitSuccess(period, category, slice)
                    }
                }
                .onFailure {
                    val hasUsableData = (_state.value as? ScheduleUiState.Success)
                        ?.data
                        ?.takeIf { data ->
                            data.selected == period.weekKey &&
                                data.selectedCategory == category &&
                                !data.loading &&
                                data.errorMessage == null
                        } != null
                    Telemetry.recordError(
                        it,
                        "stage" to "fetch_schedule_slice",
                        "week" to period.analyticsKey,
                        "category" to category.name.lowercase(),
                    )
                    if (period == selectedPeriod && category == selectedCategory && !hasUsableData) {
                        emitError(period, category, it.message ?: "Network error - could not load schedule")
                    }
                    Telemetry.log(AnalyticsEvent.ScheduleError("network"))
                    if (refresh) {
                        Telemetry.recordError(TelemetryError("schedule_refresh_failed"), "stage" to "refresh")
                    }
                }
            if (period == selectedPeriod && category == selectedCategory) {
                _refreshing.value = false
            }
        }
    }

    private fun emitLoading(period: SchedulePeriod, category: ScheduleCategory) {
        _state.value = ScheduleUiState.Success(
            ScheduleData(
                schedule = Schedule(emptyList()),
                weeks = weeks,
                selected = period.weekKey,
                selectedCategory = category,
                loading = true,
                errorMessage = null,
                featureFlags = featureFlagsRepository.flags.value,
            ),
        )
    }

    private fun emitError(period: SchedulePeriod, category: ScheduleCategory, message: String) {
        _state.value = ScheduleUiState.Success(
            ScheduleData(
                schedule = Schedule(emptyList()),
                weeks = weeks,
                selected = period.weekKey,
                selectedCategory = category,
                loading = false,
                errorMessage = message,
                featureFlags = featureFlagsRepository.flags.value,
            ),
        )
    }

    private fun emitSuccess(period: SchedulePeriod, category: ScheduleCategory, slice: ScheduleSlice) {
        weeks = weeks.map { week ->
            if (week.key == period.weekKey) week.copy(label = slice.week.label.ifBlank { week.label }) else week
        }
        val schedule = slice.schedule
        TrackLogoIndex.populate(
            schedule.races.flatMap { r ->
                listOf(r.track?.name to r.track?.logoUrl, r.circuit to r.track?.logoUrl)
            },
        )
        _state.value = ScheduleUiState.Success(
            ScheduleData(
                schedule = schedule,
                weeks = weeks,
                selected = period.weekKey,
                selectedCategory = category,
                loading = false,
                errorMessage = null,
                featureFlags = featureFlagsRepository.flags.value,
            ),
        )
    }
}

private fun defaultWeeks(): List<WeekTab> = listOf(
    WeekTab(SchedulePeriod.CURRENT.weekKey, "This week"),
    WeekTab(SchedulePeriod.NEXT.weekKey, "Next week"),
)

private val SchedulePeriod.weekKey: String
    get() = when (this) {
        SchedulePeriod.CURRENT -> CURRENT_WEEK
        SchedulePeriod.NEXT -> NEXT_WEEK
    }

private val SchedulePeriod.analyticsKey: String
    get() = weekKey

private fun String.toSchedulePeriod(): SchedulePeriod? = when (this) {
    CURRENT_WEEK -> SchedulePeriod.CURRENT
    NEXT_WEEK -> SchedulePeriod.NEXT
    else -> null
}
