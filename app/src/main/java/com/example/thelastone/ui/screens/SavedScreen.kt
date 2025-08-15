package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SavedScreen(
    padding: PaddingValues,
    openPlace: (String) -> Unit
) {
    Text("Saved", modifier = Modifier
        .fillMaxSize()
        .padding(padding))
}
