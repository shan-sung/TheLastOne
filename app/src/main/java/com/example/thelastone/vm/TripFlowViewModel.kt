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

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}

@HiltViewModel
class TripFlowViewModel @Inject constructor(
    private val repo: TripRepository,
    private val session: UserSession // 只需提供 currentUserId
) : ViewModel() {

    // --- Create 表單狀態 ---
    var formState by mutableStateOf(
        TripForm(
            name = "",
            totalBudget = null,
            startDate = "", endDate = "",
            transportPreferences = emptyList(),
            useGmapsRating = true,
            styles = emptyList()
        )
    )
        private set

    fun updateForm(reducer: (TripForm) -> TripForm) {
        formState = reducer(formState)
    }

    // --- Preview 結果 ---
    var previewTrip by mutableStateOf<Trip?>(null)
        private set

    fun createPreview(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val preview = repo.createTrip(formState)
                previewTrip = preview
                onSuccess()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun confirmSave(onSuccess: (Trip) -> Unit, onError: (Throwable) -> Unit) {
        val toSave = previewTrip ?: return
        viewModelScope.launch {
            try {
                // 保險：在存檔時把 createdBy 設為當前使用者（也可由 repo 做）
                val normalized = toSave.copy(createdBy = session.currentUserId)
                val final = repo.saveTrip(normalized)
                onSuccess(final)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }
}
