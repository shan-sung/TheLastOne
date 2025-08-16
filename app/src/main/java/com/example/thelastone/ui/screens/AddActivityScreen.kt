package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.thelastone.ui.navigation.TripRoutes
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.utils.findDayIndexByDate
import com.example.thelastone.utils.millisToDateString
import com.example.thelastone.vm.AddActivityUiState
import com.example.thelastone.vm.AddActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    padding: PaddingValues,
    tripId: String,
    placeJson: String?,   // ← 允許為 null（Edit 模式）
    nav: NavHostController,
    vm: AddActivityViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.effects.collect { eff ->
            when (eff) {
                is AddActivityViewModel.Effect.NavigateToDetail -> {
                    nav.navigate(TripRoutes.detail(eff.tripId)) {
                        popUpTo(TripRoutes.detail(eff.tripId)) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val canSubmit = remember(state) { state.trip != null && !state.submitting }

    Scaffold(
        // ✅ 外層 insets 放到這裡一次性處理
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),

        // ✅ 關掉內層 Scaffold 預設的系統 insets，避免和外層重疊
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        bottomBar = {
            Button(
                onClick = vm::submit,
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (vm.editing) "儲存變更" else "提交新增")
            }
        }
    ) { inner ->

        // ✅ 內容只吃 inner + 自己的水平間距
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(inner)
            .padding(horizontal = 16.dp)

        when (val p = state.phase) {
            AddActivityUiState.Phase.Loading -> {
                LoadingState(modifier = contentModifier)
            }
            is AddActivityUiState.Phase.Error -> {
                ErrorState(
                    modifier = contentModifier,
                    title = "載入失敗",
                    message = p.message,
                    retryLabel = "重試",
                    onRetry = vm::reload,
                    secondaryLabel = "返回",
                    onSecondary = { nav.navigateUp() }
                )
            }
            AddActivityUiState.Phase.Ready -> {
                AddActivityForm(
                    modifier = contentModifier,
                    state = state,
                    onDateChange = vm::updateDate,
                    onStartTimeChange = vm::updateStartTime,
                    onEndTimeChange = vm::updateEndTime,
                    onNoteChange = vm::updateNote
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActivityForm(
    modifier: Modifier = Modifier,
    state: AddActivityUiState,
    onDateChange: (Long?) -> Unit,
    onStartTimeChange: (String?) -> Unit,
    onEndTimeChange: (String?) -> Unit,
    onNoteChange: (String?) -> Unit
) {
    val trip = state.trip ?: run {
        EmptyState(
            modifier = modifier,
            title = "尚未載入行程",
            description = "請稍候或重試一次。"
        )
        return
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.selectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = millisToDateString(utcTimeMillis)
                return findDayIndexByDate(trip, d) != null
            }
        }
    )
    LaunchedEffect(datePickerState.selectedDateMillis) {
        onDateChange(datePickerState.selectedDateMillis)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(state.place?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Text("${trip.startDate} ~ ${trip.endDate}")
        }
        item { DatePicker(state = datePickerState) }
        item {
            OutlinedTextField(
                value = state.startTime ?: "",
                onValueChange = { onStartTimeChange(it.ifBlank { null }) },
                label = { Text("開始時間") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = state.endTime ?: "",
                onValueChange = { onEndTimeChange(it.ifBlank { null }) },
                label = { Text("結束時間") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = state.note ?: "",
                onValueChange = { onNoteChange(it.ifBlank { null }) },
                label = { Text("備註") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        // 如果想跟 CreateTrip 一樣在底部留一些視覺空隙，可以加這個：
        item { Spacer(Modifier.height(8.dp)) }
    }
}
