// vm/ExploreViewModel.kt
package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.data.repo.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExploreUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val popularTrips: List<Trip> = emptyList(),
    val isRefreshing: Boolean = false,
    val nearby: List<PlaceLite> = emptyList(),      // 👈 新增
    val nearbyError: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repo: TripRepository,
    private val placesRepo: PlacesRepository
) : ViewModel() {

    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private fun popularTripsFlow(): Flow<List<Trip>> =
        repo.observeMyTrips().map { list -> list.sortedBy { it.startDate } }

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

    // ---- Nearby API ----
    fun loadNearby(lat: Double, lng: Double, radiusMeters: Double = 3000.0, onlyOpen: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(nearbyError = null) }
            runCatching {
                placesRepo.searchNearby(
                    lat = lat, lng = lng, radiusMeters = radiusMeters,
                    includedTypes = listOf("tourist_attraction"),
                    rankPreference = "POPULARITY",  // 或 "DISTANCE"
                    openNow = if (onlyOpen) true else null,
                    maxResultCount = 20
                )
            }.onSuccess { list ->
                _state.update { it.copy(nearby = list) }
            }.onFailure { e ->
                _state.update { it.copy(nearbyError = e.message ?: "附近景點載入失敗") }
            }
        }
    }
}
