package com.orioooneee.lmuasister.ui.publicusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.data.cache.LocalCache
import com.orioooneee.lmuasister.data.remote.BackendApi
import com.orioooneee.lmuasister.data.remote.ProfileJson
import com.orioooneee.lmuasister.data.remote.PublicUserDto
import com.orioooneee.lmuasister.data.remote.RaceDetailDto
import com.orioooneee.lmuasister.data.remote.RacesPageDto
import com.orioooneee.lmuasister.data.remote.RecentRaceDto
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

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

data class PublicUserRacesState(
    val uid: String? = null,
    val category: String? = null,
    val races: List<RecentRaceDto> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

@Serializable
private data class CachedPublicUsersSummary(val summary: UsersSummaryResponse)

@Serializable
private data class CachedPublicUserDetail(val profile: SteamProfile)

@Serializable
private data class CachedPublicUserSearch(
    val query: String = "",
    val users: List<PublicUserDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
private data class CachedPublicUserRaces(
    val uid: String,
    val category: String? = null,
    val races: List<RecentRaceDto> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true,
)

private const val USERS_SUMMARY_CACHE_KEY = "public_users_summary_v1"

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

    private val _races = MutableStateFlow(PublicUserRacesState())
    val races: StateFlow<PublicUserRacesState> = _races.asStateFlow()

    private val _categoryRaces = MutableStateFlow(PublicUserRacesState())
    val categoryRaces: StateFlow<PublicUserRacesState> = _categoryRaces.asStateFlow()

    private val query = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        Telemetry.log(AnalyticsEvent.DriversViewed)
        refresh()
        viewModelScope.launch {
            query
                .debounce(320)
                .distinctUntilChanged()
                .collect { runSearch(it, reset = true) }
        }
    }

    fun refresh() {
        val cached = loadCachedSummary()
        _state.value = cached?.let { PublicUsersUiState.Success(it) } ?: PublicUsersUiState.Loading
        cached?.let { Telemetry.log(AnalyticsEvent.DriversLoaded(total = it.count, fromCache = true)) }
        viewModelScope.launch {
            apiCall { api.usersSummary() }
                .onSuccess {
                    saveCachedSummary(it)
                    _state.value = PublicUsersUiState.Success(it)
                    Telemetry.log(AnalyticsEvent.DriversLoaded(total = it.count, fromCache = false))
                }
                .onFailure {
                    if (cached == null) {
                        _state.value = PublicUsersUiState.Error(it.readableMessage("Couldn’t load drivers"))
                    }
                }
        }
    }

    fun loadDetail(uid: String) {
        val cached = loadCachedDetail(uid)
        _detail.value = cached?.let { PublicUserDetailUiState.Success(it) } ?: PublicUserDetailUiState.Loading
        cached?.let {
            Telemetry.log(AnalyticsEvent.PublicProfileLoaded(fromCache = true, externalData = it.externalData))
        }
        viewModelScope.launch {
            apiCall { api.publicUser(uid) }
                .onSuccess {
                    saveCachedDetail(uid, it)
                    _detail.value = PublicUserDetailUiState.Success(it)
                    Telemetry.log(AnalyticsEvent.PublicProfileLoaded(fromCache = false, externalData = it.externalData))
                }
                .onFailure {
                    if (cached == null) {
                        _detail.value = PublicUserDetailUiState.Error(it.readableMessage("User not found"))
                        Telemetry.log(AnalyticsEvent.PublicProfileFailed(it.analyticsReason()))
                    }
                }
        }
    }

    fun setSearchQuery(value: String) {
        val clean = value.trim()
        val cached = if (clean.isBlank()) null else loadCachedSearch(clean)
        _search.value = if (value.isBlank()) {
            PublicUserSearchState(query = value)
        } else if (cached != null) {
            cached.copy(query = value, loading = true, error = null)
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

    fun openRaces(uid: String) {
        val cur = _races.value
        if (cur.uid == uid && cur.races.isNotEmpty()) return
        val cached = loadCachedRaces(uid, category = null)
        _races.value = cached ?: PublicUserRacesState(uid = uid)
        if (cached == null) {
            loadMoreRaces(uid)
        } else {
            refreshFirstRacesPage(uid, category = null, cached = cached, target = _races)
        }
    }

    fun loadMoreRaces(uid: String? = null) {
        val cur = _races.value
        val targetUid = uid ?: cur.uid ?: return
        if (cur.uid != targetUid) {
            _races.value = loadCachedRaces(targetUid, category = null) ?: PublicUserRacesState(uid = targetUid)
            loadMoreRaces(targetUid)
            return
        }
        if (cur.loading || !cur.hasMore) return
        viewModelScope.launch {
            _races.value = cur.copy(loading = true, error = null)
            apiCall { api.publicUserRaces(targetUid, cur.page + 1) }
                .onSuccess { page ->
                    val next = _races.value.copy(
                        races = mergeAppend(_races.value.races, page.races),
                        page = page.page,
                        hasMore = page.hasMore && page.races.isNotEmpty(),
                        loading = false,
                        error = null,
                    )
                    _races.value = next
                    saveCachedRaces(next)
                }
                .onFailure {
                    _races.value = _races.value.copy(
                        loading = false,
                        error = it.readableMessage("Couldn’t load races"),
                    )
                }
        }
    }

    fun retryRaces() {
        _races.value = _races.value.copy(error = null)
        loadMoreRaces()
    }

    fun openCategoryRaces(uid: String, category: String) {
        val cur = _categoryRaces.value
        if (cur.uid == uid && cur.category == category && cur.races.isNotEmpty()) return
        val cached = loadCachedRaces(uid, category)
        _categoryRaces.value = cached ?: PublicUserRacesState(uid = uid, category = category)
        if (cached == null) {
            loadMoreCategoryRaces(uid, category)
        } else {
            refreshFirstRacesPage(uid, category, cached, _categoryRaces)
        }
    }

    fun loadMoreCategoryRaces(uid: String? = null, category: String? = null) {
        val cur = _categoryRaces.value
        val targetUid = uid ?: cur.uid ?: return
        val targetCategory = category ?: cur.category ?: return
        if (cur.uid != targetUid || cur.category != targetCategory) {
            _categoryRaces.value = loadCachedRaces(targetUid, targetCategory)
                ?: PublicUserRacesState(uid = targetUid, category = targetCategory)
            loadMoreCategoryRaces(targetUid, targetCategory)
            return
        }
        if (cur.loading || !cur.hasMore) return
        viewModelScope.launch {
            _categoryRaces.value = cur.copy(loading = true, error = null)
            apiCall { api.publicUserCategoryRaces(targetUid, targetCategory, cur.page + 1) }
                .onSuccess { page ->
                    val next = _categoryRaces.value.copy(
                        races = mergeAppend(_categoryRaces.value.races, page.races),
                        page = page.page,
                        total = page.total ?: _categoryRaces.value.total,
                        hasMore = page.hasMore && page.races.isNotEmpty(),
                        loading = false,
                        error = null,
                    )
                    _categoryRaces.value = next
                    saveCachedRaces(next)
                }
                .onFailure {
                    _categoryRaces.value = _categoryRaces.value.copy(
                        loading = false,
                        error = it.readableMessage("Couldn’t load races"),
                    )
                }
        }
    }

    fun retryCategoryRaces() {
        _categoryRaces.value = _categoryRaces.value.copy(error = null)
        loadMoreCategoryRaces()
    }

    suspend fun raceDetail(uid: String, eventId: String, split: Int?): RaceDetailDto =
        api.publicUserRaceDetail(uid, eventId, split)
            .also { runCatching { LocalCache.write(raceDetailKey(uid, eventId, split), ProfileJson.encodeToString(it)) } }

    fun cachedRaceDetail(uid: String, eventId: String, split: Int?): RaceDetailDto? =
        LocalCache.read(raceDetailKey(uid, eventId, split))?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ProfileJson.decodeFromString<RaceDetailDto>(it) }.getOrNull() }

    private fun runSearch(rawQuery: String, reset: Boolean) {
        searchJob?.cancel()
        val clean = rawQuery.trim()
        if (clean.isBlank()) {
            _search.value = PublicUserSearchState(query = rawQuery)
            return
        }
        val current = _search.value
        val page = if (reset) 1 else current.page + 1
        if (reset) {
            Telemetry.log(AnalyticsEvent.DriversSearchSubmitted(queryLength = clean.length))
        }
        val cached = if (reset) loadCachedSearch(clean) else null
        _search.value = current.copy(
            query = rawQuery,
            loading = reset,
            loadingMore = !reset,
            error = null,
            users = if (reset) cached?.users ?: emptyList() else current.users,
            page = if (reset) cached?.page ?: current.page else current.page,
            total = if (reset) cached?.total ?: 0 else current.total,
            hasMore = if (reset) cached?.hasMore ?: false else current.hasMore,
        )
        searchJob = viewModelScope.launch {
            apiCall { api.usersSearch(clean, page) }
                .onSuccess { resp ->
                    val next = _search.value.copy(
                        loading = false,
                        loadingMore = false,
                        users = if (reset) resp.users else mergeAppendUsers(_search.value.users, resp.users),
                        page = resp.page,
                        total = resp.total,
                        hasMore = resp.hasMore,
                        error = null,
                    )
                    _search.value = next
                    saveCachedSearch(clean, next)
                    Telemetry.log(
                        AnalyticsEvent.DriversSearchResults(
                            queryLength = clean.length,
                            page = resp.page,
                            results = resp.users.size,
                            total = resp.total,
                            hasMore = resp.hasMore,
                        ),
                    )
                }
                .onFailure {
                    _search.value = _search.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = it.readableMessage("Search failed"),
                    )
                    Telemetry.log(
                        AnalyticsEvent.DriversSearchFailed(
                            queryLength = clean.length,
                            page = page,
                            reason = it.analyticsReason(),
                        ),
                    )
                }
        }
    }

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> = runCatching { block() }

    private fun Throwable.readableMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() }?.take(160) ?: fallback

    private fun Throwable.analyticsReason(): String {
        val raw = message.orEmpty().lowercase()
        return when {
            "timeout" in raw -> "timeout"
            "network" in raw || "connect" in raw || "unreachable" in raw -> "network"
            "404" in raw || "not_found" in raw || "not found" in raw -> "not_found"
            "401" in raw || "403" in raw || "auth" in raw -> "auth"
            "500" in raw || "502" in raw || "503" in raw -> "server"
            else -> "other"
        }
    }

    private fun refreshFirstRacesPage(
        uid: String,
        category: String?,
        cached: PublicUserRacesState,
        target: MutableStateFlow<PublicUserRacesState>,
    ) {
        viewModelScope.launch {
            val firstPage = apiCall {
                if (category == null) api.publicUserRaces(uid, 1) else api.publicUserCategoryRaces(uid, category, 1)
            }.getOrNull() ?: return@launch
            val next = cached.copy(
                races = mergeFirstPage(firstPage, cached.races),
                page = maxOf(cached.page, firstPage.page),
                total = firstPage.total ?: cached.total,
                hasMore = firstPage.hasMore || cached.hasMore,
                loading = false,
                error = null,
            )
            if (target.value.uid == uid && target.value.category == category) {
                target.value = next
            }
            saveCachedRaces(next)
        }
    }

    private fun loadCachedSummary(): UsersSummaryResponse? =
        readCache<CachedPublicUsersSummary>(USERS_SUMMARY_CACHE_KEY)?.summary

    private fun saveCachedSummary(summary: UsersSummaryResponse) {
        writeCache(USERS_SUMMARY_CACHE_KEY, CachedPublicUsersSummary(summary))
    }

    private fun loadCachedDetail(uid: String): SteamProfile? =
        readCache<CachedPublicUserDetail>("public_user_${cachePart(uid)}")?.profile

    private fun saveCachedDetail(uid: String, profile: SteamProfile) {
        writeCache("public_user_${cachePart(uid)}", CachedPublicUserDetail(profile))
    }

    private fun loadCachedSearch(query: String): PublicUserSearchState? =
        readCache<CachedPublicUserSearch>("public_users_search_${cachePart(query.lowercase())}")?.let {
            PublicUserSearchState(
                query = it.query,
                users = it.users,
                page = it.page,
                total = it.total,
                hasMore = it.hasMore,
            )
        }

    private fun saveCachedSearch(query: String, state: PublicUserSearchState) {
        writeCache(
            "public_users_search_${cachePart(query.lowercase())}",
            CachedPublicUserSearch(
                query = query,
                users = state.users,
                page = state.page,
                total = state.total,
                hasMore = state.hasMore,
            ),
        )
    }

    private fun loadCachedRaces(uid: String, category: String?): PublicUserRacesState? =
        readCache<CachedPublicUserRaces>(raceCacheKey(uid, category))?.let {
            PublicUserRacesState(
                uid = it.uid,
                category = it.category,
                races = it.races,
                page = it.page,
                total = it.total,
                hasMore = it.hasMore,
            )
        }

    private fun saveCachedRaces(state: PublicUserRacesState) {
        val uid = state.uid ?: return
        writeCache(
            raceCacheKey(uid, state.category),
            CachedPublicUserRaces(
                uid = uid,
                category = state.category,
                races = state.races,
                page = state.page,
                total = state.total,
                hasMore = state.hasMore,
            ),
        )
    }

    private fun raceCacheKey(uid: String, category: String?): String =
        if (category == null) {
            "public_user_races_${cachePart(uid)}"
        } else {
            "public_user_races_${cachePart(uid)}_${cachePart(category)}"
        }

    private fun raceDetailKey(uid: String, eventId: String, split: Int?): String =
        "public_user_race_detail_${cachePart(uid)}_${cachePart(eventId)}_${split ?: "auto"}"

    private inline fun <reified T> readCache(key: String): T? =
        runCatching {
            LocalCache.read(key)?.takeIf { it.isNotBlank() }?.let { ProfileJson.decodeFromString<T>(it) }
        }.getOrNull()

    private inline fun <reified T> writeCache(key: String, value: T) {
        runCatching { LocalCache.write(key, ProfileJson.encodeToString(value)) }
    }

    private fun cachePart(value: String): String =
        value.take(96).map { ch ->
            if (ch.isLetterOrDigit() || ch == '_' || ch == '-') ch else '_'
        }.joinToString("")

    private fun mergeFirstPage(firstPage: RacesPageDto, cached: List<RecentRaceDto>): List<RecentRaceDto> {
        val freshKeys = firstPage.races.map(::raceKey).toSet()
        return firstPage.races + cached.filter { raceKey(it) !in freshKeys }
    }

    private fun mergeAppend(current: List<RecentRaceDto>, next: List<RecentRaceDto>): List<RecentRaceDto> {
        val seen = current.map(::raceKey).toMutableSet()
        return current + next.filter { seen.add(raceKey(it)) }
    }

    private fun mergeAppendUsers(current: List<PublicUserDto>, next: List<PublicUserDto>): List<PublicUserDto> {
        val seen = current.map { it.uid }.toMutableSet()
        return current + next.filter { seen.add(it.uid) }
    }

    private fun raceKey(race: RecentRaceDto): String =
        listOfNotNull(race.eventId, race.seriesId, race.date, race.title, (race.split ?: race.splitNo)?.toString()).joinToString("|")
}
