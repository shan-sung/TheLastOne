package com.example.thelastone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.thelastone.ui.navigation.AppScaffold
import com.example.thelastone.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Edge-to-edge
        setContent {
            AppTheme {
                AppScaffold()
            }
        }
    }
}