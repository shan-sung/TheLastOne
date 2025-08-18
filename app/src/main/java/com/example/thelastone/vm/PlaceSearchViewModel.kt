package com.example.thelastone.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.repo.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceSearchViewModel @Inject constructor(
    private val repo: PlacesRepository
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val loading: Boolean = false,
        val results: List<PlaceLite> = emptyList(),
        val error: String? = null
    )

    var state by mutableStateOf(UiState())
        private set
    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        state = state.copy(query = q)
        debounceSearch()
    }

    fun searchNow(lat: Double? = null, lng: Double? = null, radiusM: Double? = null) {
        val q = state.query.trim()
        if (q.isEmpty()) {
            state = state.copy(results = emptyList(), error = null, loading = false)
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val res = repo.searchText(q, lat, lng, radiusM)
                state = state.copy(results = res, loading = false)
            } catch (e: Exception) {
                state = state.copy(error = e.localizedMessage ?: "Uncaught error", loading = false)
            }
        }
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350) // 去抖
            searchNow()
        }
    }
}
