package com.orioooneee.lmuasister.ui.publicusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.PublicUserDto
import com.orioooneee.lmuasister.data.remote.SteamProfile
import com.orioooneee.lmuasister.data.remote.UsersSummaryResponse
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed interface PublicUsersUiState {
    data object Loading : PublicUsersUiState
    data class Success(val summary: UsersSummaryResponse) : PublicUsersUiState
    data class Error(val message: String) : PublicUsersUiState
}

sealed interface PublicUserDetailUiState {
    data object Loading : PublicUserDetailUiState
    data class Success(val profile: SteamProfile) : PublicUserDetailUiState
    data class Error(val message: String) : PublicUserDetailUiState
}

data class PublicUserSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val users: List<PublicUserDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class PublicUsersViewModel(
    private val api: BackendApi,
) : ViewModel() {

    private val _state = MutableStateFlow<PublicUsersUiState>(PublicUsersUiState.Loading)
    val state: StateFlow<PublicUsersUiState> = _state.asStateFlow()

    private val _detail = MutableStateFlow<PublicUserDetailUiState>(PublicUserDetailUiState.Loading)
    val detail: StateFlow<PublicUserDetailUiState> = _detail.asStateFlow()

    private val _search = MutableStateFlow(PublicUserSearchState())
    val search: StateFlow<PublicUserSearchState> = _search.asStateFlow()

    private val query = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            query
                .debounce(320)
                .distinctUntilChanged()
                .collect { runSearch(it, reset = true) }
        }
    }

    fun refresh() {
        _state.value = PublicUsersUiState.Loading
        viewModelScope.launch {
            apiCall { api.usersSummary() }
                .onSuccess { _state.value = PublicUsersUiState.Success(it) }
                .onFailure { _state.value = PublicUsersUiState.Error(it.readableMessage("Couldn’t load drivers")) }
        }
    }

    fun loadDetail(uid: String) {
        _detail.value = PublicUserDetailUiState.Loading
        viewModelScope.launch {
            apiCall { api.publicUser(uid) }
                .onSuccess { _detail.value = PublicUserDetailUiState.Success(it) }
                .onFailure { _detail.value = PublicUserDetailUiState.Error(it.readableMessage("User not found")) }
        }
    }

    fun setSearchQuery(value: String) {
        _search.value = if (value.isBlank()) {
            PublicUserSearchState(query = value)
        } else {
            _search.value.copy(query = value, loading = true, users = emptyList(), total = 0, hasMore = false, error = null)
        }
        query.value = value
    }

    fun clearSearch() {
        searchJob?.cancel()
        query.value = ""
        _search.value = PublicUserSearchState()
    }

    fun loadMoreSearch() {
        val s = _search.value
        if (s.loading || s.loadingMore || !s.hasMore || s.query.isBlank()) return
        runSearch(s.query, reset = false)
    }

    private fun runSearch(rawQuery: String, reset: Boolean) {
        searchJob?.cancel()
        val clean = rawQuery.trim()
        if (clean.isBlank()) {
            _search.value = PublicUserSearchState(query = rawQuery)
            return
        }
        val current = _search.value
        val page = if (reset) 1 else current.page + 1
        _search.value = current.copy(
            query = rawQuery,
            loading = reset,
            loadingMore = !reset,
            error = null,
            users = if (reset) emptyList() else current.users,
        )
        searchJob = viewModelScope.launch {
            apiCall { api.usersSearch(clean, page) }
                .onSuccess { resp ->
                    _search.value = _search.value.copy(
                        loading = false,
                        loadingMore = false,
                        users = if (reset) resp.users else _search.value.users + resp.users,
                        page = resp.page,
                        total = resp.total,
                        hasMore = resp.hasMore,
                        error = null,
                    )
                }
                .onFailure {
                    _search.value = _search.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = it.readableMessage("Search failed"),
                    )
                }
        }
    }

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> = runCatching { block() }

    private fun Throwable.readableMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() }?.take(160) ?: fallback
}
