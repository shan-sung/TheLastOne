package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TripDetailScreen(padding: PaddingValues) {
    Text("Trip Detail", modifier = Modifier
        .fillMaxSize()
        .padding(padding))
}
