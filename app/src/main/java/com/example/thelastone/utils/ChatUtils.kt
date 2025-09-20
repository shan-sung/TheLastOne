package com.example.thelastone.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime

/** 是否在列表底部（允許誤差 threshold） */
fun LazyListState.isAtBottom(threshold: Int = 1): Boolean {
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return true
    val total = layoutInfo.totalItemsCount
    return total == 0 || lastVisible >= total - 1 - threshold
}

/** 以 IME inset > 0 判斷鍵盤開啟 */
@Composable
fun rememberKeyboardOpen(): State<Boolean> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    return remember(density) { derivedStateOf { ime.getBottom(density) > 0 } }
}