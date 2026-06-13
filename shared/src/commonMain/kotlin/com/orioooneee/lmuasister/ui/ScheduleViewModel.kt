package com.orioooneee.lmuasister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.data.RaceRepository
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

    /** Loaded schedules per week key — makes switching weeks instant. */
    private val cache = mutableMapOf<String, Schedule>()
    private var weeks: List<String> = emptyList()
    private var selected: String? = null

    init {
        viewModelScope.launch {
            // 1) Offline-first: paint instantly from the cached schedule (memory/disk), no network.
            repository.cachedWeeks()?.let { cachedKeys ->
                weeks = cachedKeys
                selected = cachedKeys.firstOrNull()
                fetchWeek(selected) // load() reads the cached response — instant
            }
            // 2) Refresh from the network, then update the cache + UI in place.
            if (repository.refreshSchedule().isSuccess) {
                weeks = repository.availableWeeks() // from the just-refreshed cache
                val sel = selected
                if (sel == null || sel !in weeks) selected = weeks.firstOrNull()
                cache.clear() // drop the stale snapshot we may have shown in step 1
                fetchWeek(selected)
                prefetchOtherWeeks()
            } else if (_state.value !is ScheduleUiState.Success) {
                // nothing cached and the network failed → surface an error
                _state.value = ScheduleUiState.Error("Network error — could not load schedule")
            }
        }
    }

    fun selectWeek(key: String) {
        if (key == selected) return
        selected = key
        val cached = cache[key]
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

    /** Pull-to-refresh / manual — force-refetch from the backend. */
    fun refresh() {
        val key = selected
        cache.clear() // backend refetch updates every week — drop all stale copies
        _refreshing.value = true
        viewModelScope.launch {
            fetchWeek(key, refresh = true)
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
                if (_state.value !is ScheduleUiState.Success) {
                    _state.value = ScheduleUiState.Error(it.message ?: "Network error — could not load schedule")
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
