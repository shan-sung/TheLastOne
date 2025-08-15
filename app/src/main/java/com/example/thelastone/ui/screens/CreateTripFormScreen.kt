// CreateTripFormScreen.kt
package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CreateTripFormScreen(
    padding: PaddingValues,
    onPreview: () -> Unit,
    onCancel: () -> Unit
) {
    // 先放兩顆按鈕代表「送出表單」與「取消」
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        Button(onClick = onPreview) { Text("送出表單 → 預覽") }
        Button(onClick = onCancel) { Text("取消") }
    }
}
