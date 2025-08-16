package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.di.DEMO_USER
import com.example.thelastone.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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
    private val session: SessionManager,
) : ViewModel() {

    private val retry = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // 1) 觀察使用者變化 → 轉成 uid 流
    private val uidFlow: Flow<String> =
        session.auth
            .map { it?.user?.id ?: DEMO_USER.id }
            .distinctUntilChanged()

    // 2) 把 retry 接進來；3) 依 uid + retry 重新訂閱資料；4) 用 stateIn 輸出 StateFlow
    val state: StateFlow<MyTripsUiState> =
        uidFlow
            .flatMapLatest { uid ->
                // 任何時刻觸發 retry()，都會重新展開 observeMyTrips(uid)
                retry
                    .onStart { emit(Unit) } // 初次也跑一次
                    .flatMapLatest {
                        repo.observeMyTrips(uid)
                            // （可選）避免重複相同清單觸發重組
                            .distinctUntilChanged()
                            .map { list ->
                                if (list.isEmpty()) MyTripsUiState.Empty
                                else MyTripsUiState.Data(trips = list, userId = uid)
                            }
                            .onStart { emit(MyTripsUiState.Loading) }
                    }
            }
            .catch { emit(MyTripsUiState.Error(it.message ?: "Load failed")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MyTripsUiState.Loading
            )

    fun retry() { retry.tryEmit(Unit) }

    // 下拉刷新建議實作在 repository（例如 refreshMyTrips），這裡只觸發它：
    // fun refresh() = viewModelScope.launch {
    //     val uid = session.auth.first()?.user?.id ?: DEMO_USER.id
    //     repo.refreshMyTrips(uid)
    // }
}
