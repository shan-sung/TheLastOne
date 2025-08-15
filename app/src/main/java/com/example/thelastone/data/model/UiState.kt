package com.example.thelastone.data.model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()           // 初始狀態
    object Loading : UiState<Nothing>()        // 載入中
    data class Success<T>(val data: T) : UiState<T>() // 成功
    data class Error(val message: String) : UiState<Nothing>() // 失敗
}


@Composable
fun <T> StateView(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Idle, is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    if (onRetry != null) {
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
        is UiState.Success -> {
            successContent(state.data)
        }
    }
}
