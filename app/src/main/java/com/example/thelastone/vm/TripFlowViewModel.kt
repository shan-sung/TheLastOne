package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TripUiState {
    data object Idle : TripUiState
    data object Loading : TripUiState
    data class Preview(val trip: Trip) : TripUiState
    data class Saved(val tripId: String) : TripUiState
    data class Error(val message: String) : TripUiState
}

@HiltViewModel
class TripFlowViewModel @Inject constructor(
    private val repo: TripRepository
) : ViewModel() {

    private val _state = MutableStateFlow<TripUiState>(TripUiState.Idle)
    val state: StateFlow<TripUiState> = _state

    // 表單送出 → 要求 AI 產生預覽 Trip
    fun submitForm(form: TripForm) {
        _state.value = TripUiState.Loading
        viewModelScope.launch {
            try {
                val preview = repo.createTrip(form)
                _state.value = TripUiState.Preview(preview)
            } catch (e: Exception) {
                _state.value = TripUiState.Error(e.message ?: "Create failed")
            }
        }
    }

    // 預覽頁按「確認」→ 儲存 Trip（入庫）→ 回傳正式 tripId
    fun confirmSave(previewTrip: Trip) {
        _state.value = TripUiState.Loading
        viewModelScope.launch {
            try {
                val saved = repo.saveTrip(previewTrip)
                _state.value = TripUiState.Saved(saved.id)
            } catch (e: Exception) {
                _state.value = TripUiState.Error(e.message ?: "Save failed")
            }
        }
    }

    fun reset() {
        _state.value = TripUiState.Idle
    }
}