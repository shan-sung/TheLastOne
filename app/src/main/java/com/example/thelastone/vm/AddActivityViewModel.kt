package com.example.thelastone.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.Place
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.toFull
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.utils.decodePlaceArg
import com.example.thelastone.utils.findDayIndexByDate
import com.example.thelastone.utils.millisToDateString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

// ui/add/AddActivityUiState.kt
data class AddActivityUiState(
    val phase: Phase = Phase.Loading,
    val trip: Trip? = null,
    val place: PlaceLite? = null,
    val selectedDateMillis: Long? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val note: String? = null,
    val submitting: Boolean = false,   // 提交中 spinner/disabled 用
) {
    sealed interface Phase {
        data object Loading : Phase
        data class Error(val message: String) : Phase
        data object Ready : Phase
    }
}

@HiltViewModel
class AddActivityViewModel @Inject constructor(
    private val repo: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    // placeJson 在 Add 模式才會有；Edit 模式不一定有
    private val placeLiteFromArg: PlaceLite? =
        savedStateHandle.get<String>("placeJson")?.let { decodePlaceArg(it) }

    // 模式判斷：有 dayIndex + activityIndex 就是 Edit
    sealed class Mode {
        data object Add : Mode()
        data class Edit(val dayIndex: Int, val activityIndex: Int) : Mode()
    }
    private val mode: Mode = run {
        val di = savedStateHandle.get<Int>("dayIndex")
        val ai = savedStateHandle.get<Int>("activityIndex")
        if (di != null && ai != null) Mode.Edit(di, ai) else Mode.Add
    }
    val editing: Boolean get() = mode is Mode.Edit  // 給 UI 換按鈕文字用

    private val _state = MutableStateFlow(AddActivityUiState(place = placeLiteFromArg))
    val state: StateFlow<AddActivityUiState> = _state

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    val effects: SharedFlow<Effect> = _effects

    sealed interface Effect { data class NavigateToDetail(val tripId: String) : Effect }

    init { reload() }

    fun reload() = viewModelScope.launch {
        _state.update { it.copy(phase = AddActivityUiState.Phase.Loading) }
        runCatching { repo.getTripDetail(tripId) }
            .onSuccess { t ->
                when (val m = mode) {
                    Mode.Add -> {
                        val defaultMillis = LocalDate.parse(t.startDate, DATE_FMT)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        _state.update {
                            it.copy(
                                phase = AddActivityUiState.Phase.Ready,
                                trip = t,
                                selectedDateMillis = it.selectedDateMillis ?: defaultMillis,
                                // place 已在建構時帶入 placeLiteFromArg（新增時會顯示）
                            )
                        }
                    }
                    is Mode.Edit -> {
                        val day = t.days[m.dayIndex]
                        val act = day.activities[m.activityIndex]
                        val dayDate = when (val d = day.date) {
                            is String -> LocalDate.parse(d, DATE_FMT)
                            is java.time.LocalDate -> d
                            is java.time.LocalDateTime -> d.toLocalDate()
                            else -> LocalDate.parse(d.toString(), DATE_FMT)
                        }
                        val millis = dayDate.atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()

                        _state.update {
                            it.copy(
                                phase = AddActivityUiState.Phase.Ready,
                                trip = t,
                                selectedDateMillis = millis,
                                place = act.place.toLite(),    // ← 由舊資料預填
                                startTime = act.startTime,
                                endTime   = act.endTime,
                                note      = act.note
                            )
                        }
                    }
                }
            }
            .onFailure { e ->
                _state.update { it.copy(phase = AddActivityUiState.Phase.Error(e.message ?: "載入失敗")) }
            }
    }

    fun updateDate(millis: Long?)  { _state.update { it.copy(selectedDateMillis = millis) } }
    fun updateStartTime(v: String?) { _state.update { it.copy(startTime = v?.ifBlank { null }) } }
    fun updateEndTime(v: String?)   { _state.update { it.copy(endTime = v?.ifBlank { null }) } }
    fun updateNote(v: String?)      { _state.update { it.copy(note = v?.ifBlank { null }) } }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val t = s.trip ?: return@launch
        val millis = s.selectedDateMillis ?: run {
            _state.update { it.copy(phase = AddActivityUiState.Phase.Error("請選擇日期")) }
            return@launch
        }
        val dateStr = millisToDateString(millis)
        val newDayIndex = findDayIndexByDate(t, dateStr)
            ?: return@launch _state.update { it.copy(phase = AddActivityUiState.Phase.Error("日期不在行程範圍內")) }

        _state.update { it.copy(submitting = true) }

        when (val m = mode) {
            Mode.Add -> {
                val act = Activity(
                    id = UUID.randomUUID().toString(),
                    place = (s.place ?: placeLiteFromArg)!!.toFull(),
                    startTime = s.startTime,
                    endTime = s.endTime,
                    note = s.note
                )
                runCatching { repo.addActivity(tripId, newDayIndex, act) }
                    .onSuccess {
                        _state.update { it.copy(submitting = false) }
                        _effects.tryEmit(Effect.NavigateToDetail(tripId))
                    }
                    .onFailure { e ->
                        _state.update { it.copy(submitting = false, phase = AddActivityUiState.Phase.Error(e.message ?: "新增失敗")) }
                    }
            }
            is Mode.Edit -> {
                val oldDay = m.dayIndex
                val act0 = t.days[oldDay].activities[m.activityIndex]
                val updated = act0.copy(
                    // place 保持原地點；若你要讓使用者換地點，這裡可改成 (s.place ?: act0.place.toLite()).toFull()
                    startTime = s.startTime,
                    endTime = s.endTime,
                    note = s.note
                )
                val result = runCatching {
                    if (newDayIndex == oldDay) {
                        repo.updateActivity(tripId, oldDay, m.activityIndex, updated)
                    } else {
                        // 日期變更：先刪舊、再加到新的一天
                        repo.removeActivity(tripId, oldDay, m.activityIndex)
                        repo.addActivity(tripId, newDayIndex, updated.copy(id = UUID.randomUUID().toString()))
                    }
                }
                result
                    .onSuccess {
                        _state.update { it.copy(submitting = false) }
                        _effects.tryEmit(Effect.NavigateToDetail(tripId))
                    }
                    .onFailure { e ->
                        _state.update { it.copy(submitting = false, phase = AddActivityUiState.Phase.Error(e.message ?: "更新失敗")) }
                    }
            }
        }
    }
}

// 依你的 PlaceLite 欄位調整
private fun Place.toLite(): PlaceLite =
    PlaceLite(placeId = placeId, name = name, lat = lat, lng = lng, address = address)
