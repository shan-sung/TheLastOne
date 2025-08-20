package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.data.repo.PlacesRepository
import com.example.thelastone.data.repo.RankPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ExploreMode {
    data object Nearby : ExploreMode
    data object Popular : ExploreMode
}

data class ExploreUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val popularTrips: List<Trip> = emptyList(),
    val isRefreshing: Boolean = false,

    // Nearby
    val nearby: List<PlaceLite> = emptyList(),
    val nearbyError: String? = null,
    val nearbyLoading: Boolean = false,

    val popularSpots: List<PlaceLite> = emptyList(),
    val popularSpotsError: String? = null,
    val popularSpotsLoading: Boolean = false,

    val mode: ExploreMode = ExploreMode.Popular
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repo: TripRepository,
    private val placesRepo: PlacesRepository
) : ViewModel() {

    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private fun popularTripsFlow(): Flow<List<Trip>> =
        repo.observePublicTrips().map { list -> list.sortedBy { it.startDate } }
    @OptIn(ExperimentalCoroutinesApi::class)
    private val popularResource: Flow<Result<List<Trip>>> =
        refresh.onStart { emit(Unit) }
            .flatMapLatest {
                popularTripsFlow()
                    .map { Result.success(it) }
                    .catch { e -> emit(Result.failure(e)) }
            }

    // ---- State ----
    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            popularResource.scan(ExploreUiState()) { prev, result ->
                if (result.isSuccess) {
                    prev.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        popularTrips = result.getOrDefault(emptyList())
                    )
                } else {
                    prev.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.exceptionOrNull()?.message ?: "發生未知錯誤"
                    )
                }
            }.collect { _state.value = it }
        }
    }

    fun refresh() {
        viewModelScope.launch { refresh.emit(Unit) }
    }
    fun retry() = refresh()

    fun loadNearby(
        lat: Double, lng: Double,
        radiusMeters: Int = 6000,
        onlyOpen: Boolean = false
    ) {
        viewModelScope.launch {
            _state.update { it.copy(nearbyLoading = true, nearbyError = null) }
            runCatching {
                placesRepo.searchNearby(
                    lat = lat, lng = lng, radiusMeters = radiusMeters,
                    // ★ 放寬型別，避免抓不到
                    includedTypes = listOf("tourist_attraction", "point_of_interest"),
                    rankPreference = RankPreference.POPULARITY,
                    openNow = if (onlyOpen) true else null,
                    maxResultCount = 30
                )
            }.onSuccess { list ->
                _state.update { it.copy(nearby = list, nearbyLoading = false) }
                if (list.isEmpty()) {
                    _state.update { it.copy(mode = ExploreMode.Popular) }
                    loadPopularSpotsIfNeeded()
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(nearbyError = e.message ?: "附近景點載入失敗", nearbyLoading = false)
                }
                _state.update { it.copy(mode = ExploreMode.Popular) }
                loadPopularSpotsIfNeeded()
            }
        }
    }
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            // 交由 UI 端在拿到座標後呼叫 loadNearby()（你已經這樣做）
            _state.update { it.copy(mode = ExploreMode.Nearby) }
        } else {
            // 立刻載入 Popular Spots 當作 fallback
            loadPopularSpotsIfNeeded()
            _state.update { it.copy(mode = ExploreMode.Popular) }
        }
    }

    fun loadPopularSpotsIfNeeded() {
        val s = _state.value
        if (s.popularSpots.isNotEmpty() || s.popularSpotsLoading) return
        loadPopularSpots()
    }
    fun loadPopularSpots(
        query: String = "top tourist attractions", // 或 "熱門景點"
    ) {
        viewModelScope.launch {
            _state.update { it.copy(popularSpotsLoading = true, popularSpotsError = null) }
            runCatching {
                placesRepo.searchText(
                    query = query,
                    lat = null, lng = null,
                    radiusMeters = null,
                    openNow = null
                )
            }.onSuccess { list ->
                _state.update { it.copy(popularSpots = list, popularSpotsLoading = false) }
            }.onFailure { e ->
                _state.update {
                    it.copy(popularSpotsError = e.message ?: "熱門景點載入失敗", popularSpotsLoading = false)
                }
            }
        }
    }
}