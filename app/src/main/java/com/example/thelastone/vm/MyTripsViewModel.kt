package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyTripsUiState {
    data object Loading : MyTripsUiState
    data class Data(val trips: List<Trip>, val userId: String) : MyTripsUiState
    data object Empty : MyTripsUiState
    data class Error(val message: String) : MyTripsUiState
}


@HiltViewModel
class MyTripsViewModel @Inject constructor(
    private val repo: TripRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<MyTripsUiState>(MyTripsUiState.Loading)
    val state: StateFlow<MyTripsUiState> = _state

    init {
        val uid = session.currentUserId
        viewModelScope.launch {
            repo.observeMyTrips(uid)
                .onStart { _state.value = MyTripsUiState.Loading }
                .catch { e -> _state.value = MyTripsUiState.Error(e.message ?: "Load failed") }
                .collect { list ->
                    _state.value = if (list.isEmpty()) MyTripsUiState.Empty
                    else MyTripsUiState.Data(trips = list, userId = uid)
                }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = MyTripsUiState.Loading
            val uid = session.currentUserId
            runCatching { repo.getMyTrips(uid) }
                .onSuccess { list ->
                    _state.value = if (list.isEmpty()) MyTripsUiState.Empty
                    else MyTripsUiState.Data(trips = list, userId = uid)
                }
                .onFailure { e ->
                    _state.value = MyTripsUiState.Error(e.message ?: "Load failed")
                }
        }
    }
}