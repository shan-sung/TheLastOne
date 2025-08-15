package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ExploreScreen(
    padding: PaddingValues,
    openPlace: (String) -> Unit
) {
    // 之後放地圖、搜尋入口、熱門清單等
    Text("Explore", modifier = Modifier
        .fillMaxSize()
        .padding(padding))
}