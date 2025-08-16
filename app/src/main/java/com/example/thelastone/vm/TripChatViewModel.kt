package com.example.thelastone.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Message
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.User
import com.example.thelastone.data.repo.ChatRepository
import com.example.thelastone.data.repo.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Data(
        val trip: Trip?,
        val messages: List<Message>,
        val input: String,
        val analyzing: Boolean,
        val showTripSheet: Boolean
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class TripChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val tripRepo: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = savedStateHandle["tripId"] ?: error("tripId missing")

    private val _input = MutableStateFlow("")
    private val _analyzing = MutableStateFlow(false)
    private val _showTripSheet = MutableStateFlow(false)

    private val tripFlow: Flow<Trip?> =
        tripRepo.observeTripDetail(tripId)
            .map<Trip, Trip?> { it }
            .catch { emit(null) }

    private val messagesFlow: Flow<List<Message>> =
        chatRepo.observeMessages(tripId)

    val state: StateFlow<ChatUiState> =
        combine(tripFlow, messagesFlow, _input, _analyzing, _showTripSheet) { trip, msgs, input, analyzing, sheet ->
            ChatUiState.Data(trip, msgs, input, analyzing, sheet)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUiState.Loading)

    init {
        // 進聊天室就同步一次雲端歷史到本地
        viewModelScope.launch { chatRepo.refresh(tripId) }
    }

    fun updateInput(v: String) { _input.value = v }

    fun send() = viewModelScope.launch {
        val txt = _input.value.trim()
        if (txt.isEmpty()) return@launch
        _input.value = ""
        val me = User(id = "me", name = "You", email = "me@example.com")
        try {
            chatRepo.send(tripId, txt, me) // 會先寫 Room(SENDING) → 網路成功後置 SENT/換 id
        } catch (_: Exception) {
            // 可選：在 UI 顯示「傳送失敗，點擊重送」
        }
    }

    private var analyzeJob: Job? = null
    fun analyze() {
        if (_analyzing.value) return
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _analyzing.value = true
            try { chatRepo.analyze(tripId) } finally { _analyzing.value = false }
        }
    }

    fun toggleTripSheet(show: Boolean) { _showTripSheet.value = show }

    fun onSelectSuggestion(place: PlaceLite) = viewModelScope.launch {
        val me = User(id = "me", name = "You", email = "me@example.com")
        chatRepo.send(tripId, "選擇：${place.name}", me)
        // 也可在這裡彈出 Dialog 並呼叫 tripRepo.addActivity(...)
    }
}
