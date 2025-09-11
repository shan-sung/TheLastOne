package com.example.thelastone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
            AppTheme( // ← 你自己的
                darkTheme = isSystemInDarkTheme(), // 預設跟隨系統
                dynamicColor = true                 // Android 12+ 啟用動態色
            ) {
                // 這裡放整個 App UI
                AppScaffold()
            }
        }
    }
}