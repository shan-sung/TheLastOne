package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun EditProfileScreen(padding: androidx.compose.foundation.layout.PaddingValues) {
    Text("Edit Profile", modifier = Modifier
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(padding))
}