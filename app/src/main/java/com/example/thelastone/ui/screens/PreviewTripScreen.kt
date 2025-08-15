// PreviewTripScreen.kt
package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PreviewTripScreen(
    padding: PaddingValues,
    onConfirmSaved: (String) -> Unit,
    onBack: () -> Unit
) {
    // 先用假 id 模擬儲存成功
    val fakeTripId = "trip-123"

    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        Text("這裡是預覽頁（之後顯示 AI 生成的行程）")
        Button(onClick = { onConfirmSaved(fakeTripId) }) { Text("確認儲存 → 進 Detail") }
        Button(onClick = onBack) { Text("返回") }
    }
}
