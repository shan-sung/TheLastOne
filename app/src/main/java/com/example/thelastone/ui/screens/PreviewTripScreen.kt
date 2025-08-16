// PreviewTripScreen.kt
package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.TripFormViewModel

@Composable
fun PreviewTripScreen(
    padding: PaddingValues,
    onConfirmSaved: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TripFormViewModel
) {
    val preview by viewModel.preview.collectAsState()
    val save by viewModel.save.collectAsState()

    // 儲存成功就導去 Detail
    LaunchedEffect(save) {
        if (save is TripFormViewModel.SaveUiState.Success) {
            onConfirmSaved((save as TripFormViewModel.SaveUiState.Success).tripId)
            viewModel.resetSaveState()
        }
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        when (val p = preview) {
            TripFormViewModel.PreviewUiState.Idle -> {
                Text("沒有預覽資料，請回到上一步。")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBack) { Text("返回") }
            }
            TripFormViewModel.PreviewUiState.Loading -> {
                LoadingState(Modifier.fillMaxWidth())
            }
            is TripFormViewModel.PreviewUiState.Error -> {
                ErrorState(
                    modifier = Modifier.fillMaxWidth(),
                    title = "預覽失敗",
                    message = p.message,
                    onRetry = { /* 依需求決定是否提供重試 */ }
                )
            }
            is TripFormViewModel.PreviewUiState.Data -> {
                Text("預覽：${p.trip.name}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("日期：${p.trip.startDate} ～ ${p.trip.endDate}")
                Spacer(Modifier.height(8.dp))
                Text("共 ${p.trip.days.size} 天，示意行程點：${p.trip.days.firstOrNull()?.activities?.size ?: 0} 個")
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) { Text("返回修改") }
                    Button(
                        enabled = save !is TripFormViewModel.SaveUiState.Loading,
                        onClick = { viewModel.confirmSave() }
                    ) {
                        if (save is TripFormViewModel.SaveUiState.Loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("確認儲存")
                    }
                }
                if (save is TripFormViewModel.SaveUiState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (save as TripFormViewModel.SaveUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
