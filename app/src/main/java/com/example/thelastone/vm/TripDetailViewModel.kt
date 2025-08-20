package com.example.thelastone.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.di.SessionManager
import com.example.thelastone.utils.TripPerms
import com.example.thelastone.utils.computePerms
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val retry = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // 暴露權限給 UI
    private val _perms = MutableStateFlow<TripPerms?>(null)
    val perms: StateFlow<TripPerms?> = _perms


    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TripDetailUiState> =
        retry.onStart { emit(Unit) }
            .flatMapLatest {
                repo.observeTripDetail(tripId)
                    .onEach { trip ->
                        // 每次資料更新時重算權限
                        val uid = session.currentUserId
                        _perms.value = trip.computePerms(uid)
                    }
                    .map<Trip, TripDetailUiState> { TripDetailUiState.Data(it) }
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
                .onFailure { /* TODO: snackbar */ }
        }
    }

    fun reload() { retry.tryEmit(Unit) }
}