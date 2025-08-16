package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.Activity
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.TripDetailUiState
import com.example.thelastone.vm.TripDetailViewModel

@Composable
fun TripDetailScreen(
    padding: PaddingValues,
    viewModel: TripDetailViewModel = hiltViewModel(),
    onAddActivity: (tripId: String) -> Unit = {},
    onOpenActivity: (tripId: String, dayIndex: Int, act: Activity) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is TripDetailUiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
        is TripDetailUiState.Error -> ErrorState(
            modifier = Modifier.padding(padding),
            message = s.message,
            onRetry = viewModel::load
        )
        is TripDetailUiState.Data -> {
            val trip = s.trip
            var selected by rememberSaveable { mutableIntStateOf(0) }

            // 用 Box 疊出右下 FAB，但內容仍採用 Column + LazyColumn(weight=1f) 的版型
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { TripInfoCard(trip) }
                        dayTabsAndActivities(
                            trip = trip,
                            selected = selected,
                            onSelect = { selected = it },
                            onActivityClick = { dayIdx, _, act -> onOpenActivity(trip.id, dayIdx, act) }
                        )
                        // 給 FAB 一點墊底空間，避免最後一項被浮動按鈕擋住
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
                // 右下 FAB（Preview 不顯示）
                FloatingActionButton(
                    onClick = { onAddActivity(trip.id) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        }
    }
}
