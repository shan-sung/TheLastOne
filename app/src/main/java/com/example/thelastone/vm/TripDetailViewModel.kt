package com.example.thelastone.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TripDetailUiState {
    data object Loading : TripDetailUiState
    data class Data(val trip: Trip) : TripDetailUiState
    data class Error(val message: String) : TripDetailUiState
}

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val repo: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val retry = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TripDetailUiState> =
        retry
            .onStart { emit(Unit) } // 首次啟動
            .flatMapLatest {
                repo.observeTripDetail(tripId)
                    .map< Trip, TripDetailUiState> { TripDetailUiState.Data(it) }
                    .catch { emit(TripDetailUiState.Error(it.message ?: "Load failed")) }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TripDetailUiState.Loading
            )

    fun removeActivity(dayIndex: Int, activityIndex: Int) {
        viewModelScope.launch {
            runCatching { repo.removeActivity(tripId, dayIndex, activityIndex) }
                .onFailure { /* TODO: Snackbar 或錯誤狀態 */ }
        }
    }
    fun reload() { retry.tryEmit(Unit) } // 需要時仍可手動觸發
}
