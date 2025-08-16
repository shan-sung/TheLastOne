// CreateTripFormScreen.kt
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.thelastone.vm.TripFormViewModel

@Composable
fun CreateTripFormScreen(
    padding: PaddingValues,
    onPreview: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TripFormViewModel
) {
    val form by viewModel.form.collectAsState()
    val preview by viewModel.preview.collectAsState()

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        OutlinedTextField(
            value = form.name,
            onValueChange = viewModel::updateName,
            label = { Text("行程名稱") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.totalBudget?.toString() ?: "",
            onValueChange = viewModel::updateBudget,
            label = { Text("總預算 (元)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.startDate,
            onValueChange = viewModel::updateStart,
            label = { Text("開始日期 yyyy-MM-dd") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.endDate,
            onValueChange = viewModel::updateEnd,
            label = { Text("結束日期 yyyy-MM-dd") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                viewModel.generatePreview()
                onPreview() // 先觸發生成，再導到預覽頁
            }) { Text("送出表單 → 預覽") }

            OutlinedButton(onClick = onCancel) { Text("取消") }
        }

        if (preview is TripFormViewModel.PreviewUiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = (preview as TripFormViewModel.PreviewUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
