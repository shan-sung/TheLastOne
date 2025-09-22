package com.example.thelastone.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.Trip
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

data class AddActivityUiState(
    val phase: Phase = Phase.Loading,
    val trip: Trip? = null,
    val place: PlaceLite? = null,
    val selectedDateMillis: Long? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val note: String? = null,
    val submitting: Boolean = false,
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
    private val placeLiteFromArg: PlaceLite? =
        savedStateHandle.get<String>("placeJson")?.let { decodePlaceArg(it) }

    // 以 activityId 判斷模式
    sealed class Mode {
        data object Add : Mode()
        data class Edit(val activityId: String) : Mode()
    }
    private val mode: Mode = run {
        val aid = savedStateHandle.get<String>("activityId")
        if (!aid.isNullOrBlank()) Mode.Edit(aid) else Mode.Add
    }
    val editing: Boolean get() = mode is Mode.Edit

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
                                selectedDateMillis = it.selectedDateMillis ?: defaultMillis
                            )
                        }
                    }
                    is Mode.Edit -> {
                        val (dayIdx, actIdx, act, dayDate) = findActivityPositionAndDate(t, m.activityId)
                            ?: return@onSuccess _state.update {
                                it.copy(phase = AddActivityUiState.Phase.Error("找不到要編輯的活動"))
                            }

                        val millis = dayDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                        _state.update {
                            it.copy(
                                phase = AddActivityUiState.Phase.Ready,
                                trip = t,
                                selectedDateMillis = millis,
                                place = act.place,          // ← 直接用 PlaceLite，移除 toLite()
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

    suspend fun loadForEdit(tripId: String, activityId: String) { reload() }

    fun initForCreate(tripId: String, placeJson: String) {
        // 目前由 placeLiteFromArg 完成預填；需要時可在此解碼並 setState
    }

    fun fail(message: String) {
        _state.update { it.copy(phase = AddActivityUiState.Phase.Error(message)) }
    }

    fun updateDate(millis: Long?)   { _state.update { it.copy(selectedDateMillis = millis) } }
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
                    place = (s.place ?: placeLiteFromArg)!!,   // ← 移除 toFull()
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
                val pos = findActivityPositionAndDate(t, m.activityId)
                if (pos == null) {
                    _state.update { it.copy(submitting = false, phase = AddActivityUiState.Phase.Error("找不到活動")) }
                    return@launch
                }
                val (oldDayIndex, oldActIndex, act0, _) = pos

                val updated = act0.copy(
                    // 若要允許改地點，可用 place = (s.place ?: act0.place)
                    startTime = s.startTime,
                    endTime = s.endTime,
                    note = s.note
                )

                val result = runCatching {
                    if (newDayIndex == oldDayIndex) {
                        repo.updateActivity(tripId, oldDayIndex, oldActIndex, updated)
                    } else {
                        repo.removeActivity(tripId, oldDayIndex, oldActIndex)
                        repo.addActivity(tripId, newDayIndex, updated)
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

    /** 以 activityId 找到 (dayIndex, activityIndex, act, dayDate) */
    private fun findActivityPositionAndDate(trip: Trip, activityId: String)
            : Quadruple<Int, Int, Activity, LocalDate>? {
        trip.days.forEachIndexed { dIdx, day ->
            val aIdx = day.activities.indexOfFirst { it.id == activityId }
            if (aIdx >= 0) {
                val act = day.activities[aIdx]
                val dayDate = LocalDate.parse(day.date, DATE_FMT)
                return Quadruple(dIdx, aIdx, act, dayDate)
            }
        }
        return null
    }
}

/** 小工具：沒有現成 Quadruple 就自建一下（也可用 Pair/Triple 巢狀替代） */
data class Quadruple<A,B,C,D>(val first: A, val second: B, val third: C, val fourth: D)
