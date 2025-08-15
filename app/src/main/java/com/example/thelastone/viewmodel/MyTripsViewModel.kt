package com.example.thelastone.viewmodel

// ui/screens/mytrips/MyTripsViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.UiState
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyTripsViewModel @Inject constructor(
    private val tripRepo: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Trip>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Trip>>> = _uiState

    fun loadTrips() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val trips = tripRepo.getMyTrips()
                _uiState.value = UiState.Success(trips)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
