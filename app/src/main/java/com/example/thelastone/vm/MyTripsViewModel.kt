package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyTripsUiState {
    data object Loading : MyTripsUiState
    data class Data(val trips: List<Trip>) : MyTripsUiState
    data object Empty : MyTripsUiState
    data class Error(val message: String) : MyTripsUiState
}

@HiltViewModel
class MyTripsViewModel @Inject constructor(
    private val repo: TripRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MyTripsUiState>(MyTripsUiState.Loading)
    val state: StateFlow<MyTripsUiState> = _state

    fun load(userId: String) {
        viewModelScope.launch {
            _state.value = MyTripsUiState.Loading
            runCatching { repo.getMyTrips(userId) }
                .onSuccess { list ->
                    _state.value = if (list.isEmpty()) MyTripsUiState.Empty
                    else MyTripsUiState.Data(list)
                }
                .onFailure { e ->
                    _state.value = MyTripsUiState.Error(e.message ?: "Load failed")
                }
        }
    }
}
