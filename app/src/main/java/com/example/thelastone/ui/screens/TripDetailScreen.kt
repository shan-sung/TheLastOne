package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.TripDetailUiState
import com.example.thelastone.vm.TripDetailViewModel

@Composable
fun TripDetailScreen(
    padding: PaddingValues,
    vm: TripDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        when (val s = state) {
            TripDetailUiState.Loading -> LoadingState(Modifier.fillMaxWidth())
            is TripDetailUiState.Error -> ErrorState(
                modifier = Modifier.fillMaxWidth(),
                title = "載入失敗",
                message = s.message,
                onRetry = { vm.load() }
            )
            is TripDetailUiState.Data -> {
                Text(s.trip.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("日期：${s.trip.startDate} ～ ${s.trip.endDate}")
                Spacer(Modifier.height(8.dp))
                Text("天數：${s.trip.days.size}")
                Spacer(Modifier.height(16.dp))
                LazyColumn {
                    items(s.trip.days) { d ->
                        Text("• ${d.date}（${d.activities.size} 個活動）")
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
