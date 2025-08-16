package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TripFormViewModel @Inject constructor(
    private val repo: TripRepository
) : ViewModel() {

    // ===== 表單狀態（簡化：給預設值就能直接按預覽）=====
    data class Form(
        val name: String = "我的小旅行",
        val totalBudget: Int? = 10000,
        val startDate: String = LocalDate.now().plusDays(3).toString(), // yyyy-MM-dd
        val endDate: String = LocalDate.now().plusDays(4).toString(),
        val transportPreferences: List<String> = listOf("Walk"),
        val useGmapsRating: Boolean = true,
        val styles: List<String> = listOf("Relax")
    )
    private val _form = MutableStateFlow(Form())
    val form: StateFlow<Form> = _form

    fun updateName(v: String) = _form.update { it.copy(name = v) }
    fun updateBudget(v: String) = _form.update { it.copy(totalBudget = v.toIntOrNull()) }
    fun updateStart(v: String) = _form.update { it.copy(startDate = v) }
    fun updateEnd(v: String) = _form.update { it.copy(endDate = v) }

    // ===== 預覽狀態 =====
    sealed interface PreviewUiState {
        data object Idle : PreviewUiState
        data object Loading : PreviewUiState
        data class Data(val trip: Trip) : PreviewUiState
        data class Error(val message: String) : PreviewUiState
    }
    private val _preview = MutableStateFlow<PreviewUiState>(PreviewUiState.Idle)
    val preview: StateFlow<PreviewUiState> = _preview

    // ===== 儲存狀態 =====
    sealed interface SaveUiState {
        data object Idle : SaveUiState
        data object Loading : SaveUiState
        data class Success(val tripId: String) : SaveUiState
        data class Error(val message: String) : SaveUiState
    }
    private val _save = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val save: StateFlow<SaveUiState> = _save

    fun generatePreview() {
        val f = _form.value
        val tf = TripForm(
            name = f.name,
            totalBudget = f.totalBudget,
            startDate = f.startDate,
            endDate = f.endDate,
            transportPreferences = f.transportPreferences,
            useGmapsRating = f.useGmapsRating,
            styles = f.styles
        )
        viewModelScope.launch {
            _preview.value = PreviewUiState.Loading
            runCatching { repo.createTrip(tf) }
                .onSuccess { _preview.value = PreviewUiState.Data(it) }
                .onFailure { _preview.value = PreviewUiState.Error(it.message ?: "Preview failed") }
        }
    }

    fun confirmSave() {
        val p = _preview.value
        if (p !is PreviewUiState.Data) return
        viewModelScope.launch {
            _save.value = SaveUiState.Loading
            runCatching { repo.saveTrip(p.trip) }
                .onSuccess { _save.value = SaveUiState.Success(it.id) }
                .onFailure { _save.value = SaveUiState.Error(it.message ?: "Save failed") }
        }
    }

    fun resetSaveState() { _save.value = SaveUiState.Idle }
}
