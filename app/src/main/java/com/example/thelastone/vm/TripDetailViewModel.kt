package com.example.thelastone.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _state = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val state: StateFlow<TripDetailUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = TripDetailUiState.Loading
            runCatching { repo.getTripDetail(tripId) }
                .onSuccess { _state.value = TripDetailUiState.Data(it) }
                .onFailure { _state.value = TripDetailUiState.Error(it.message ?: "Load failed") }
        }
    }
}