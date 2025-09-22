package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.SpotFilters
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.SpotRepository
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Trips
    val popularTrips: List<Trip> = emptyList(),
    val isRefreshing: Boolean = false,

    // Popular Spots（全台，不需定位）
    val spots: List<PlaceLite> = emptyList(),
    val spotsLoading: Boolean = false,
    val spotsError: String? = null,
    val spotsInitialized: Boolean = false,

    // Nearby Spots（需定位）
    val nearby: List<PlaceLite> = emptyList(),
    val nearbyLoading: Boolean = false,
    val nearbyError: String? = null,
    val nearbyInitialized: Boolean = false,

    val filters: SpotFilters = SpotFilters(),
    val filtered: List<PlaceLite> = emptyList(),   // 篩選結果（顯示在 Filter 畫面或返回 Explore）
    val filtering: Boolean = false,
    val filteringError: String? = null
)

// ExploreViewModel.kt
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val tripRepo: TripRepository,
    private val spotRepo: SpotRepository
) : ViewModel() {

    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private fun popularTripsFlow(): Flow<List<Trip>> =
        tripRepo.observePublicTrips().map { list -> list.sortedBy { it.startDate } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val popularResource: Flow<Result<List<Trip>>> =
        refresh.onStart { emit(Unit) }
            .flatMapLatest {
                popularTripsFlow()
                    .map { Result.success(it) }
                    .catch { e -> emit(Result.failure(e)) }
            }

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    init {
        // Trips
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
        // Spots 的刷新交給畫面再決定叫哪一個（附近 or 台灣）
    }
    fun retry() = refresh()


    fun loadSpotsTaiwan(userId: String? = null, limit: Int = 30) {
        viewModelScope.launch {
            _state.update { it.copy(spotsLoading = true, spotsError = null) }
            runCatching { spotRepo.getTaiwanPopularSpots(userId, limit) }
                .onSuccess { list ->
                    _state.update {
                        it.copy(
                            spots = list,
                            spotsLoading = false,
                            spotsInitialized = true
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            spotsError = e.message ?: "熱門景點載入失敗",
                            spotsLoading = false,
                            spotsInitialized = true
                        )
                    }
                }
        }
    }

    /** 供第 3 區塊使用：載入附近熱門（需定位） */
    fun loadNearbyAroundMe(
        userId: String? = null,
        limit: Int = 30,
        lat: Double,
        lng: Double,
        radiusMeters: Int = 2000,
        openNow: Boolean? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(nearbyLoading = true, nearbyError = null) }
            runCatching {
                spotRepo.getRecommendedSpots(userId, limit, lat, lng, radiusMeters, openNow)
            }.onSuccess { list ->
                _state.update {
                    it.copy(
                        nearby = list,
                        nearbyLoading = false,
                        nearbyInitialized = true
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        nearbyError = e.message ?: "附近景點載入失敗",
                        nearbyLoading = false,
                        nearbyInitialized = true
                    )
                }
            }
        }
    }

    fun applyFilters(filters: SpotFilters, limit: Int = 30) {
        viewModelScope.launch {
            _state.update { it.copy(filtering = true, filteringError = null, filters = filters) }
            runCatching { spotRepo.getFilteredSpots(filters, limit) }
                .onSuccess { list ->
                    _state.update { it.copy(filtering = false, filtered = list) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(filtering = false, filteringError = e.message ?: "篩選失敗")
                    }
                }
        }
    }
    /** 沒有定位或拿不到座標時，可以清空 nearby 狀態但標記為初始化 */
    fun markNearbyAsUnavailable() {
        _state.update { it.copy(nearby = emptyList(), nearbyInitialized = true, nearbyLoading = false, nearbyError = null) }
    }
}