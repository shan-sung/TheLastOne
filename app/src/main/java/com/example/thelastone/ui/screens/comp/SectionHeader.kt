package com.example.thelastone.ui.screens.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,          // true -> headlineSmall；false -> titleMedium
    secondaryTone: Boolean = false,  // true -> onSurfaceVariant
    bold: Boolean = false,
    bottomSpace: Dp = 12.dp,         // ← 新增：底部間距（預設 12dp）
    sticky: Boolean = false          // ← 新增：sticky header 要不要補背景
) {
    val typography = MaterialTheme.typography
    val color = if (secondaryTone) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurface
    val baseStyle = if (large) typography.headlineSmall else typography.titleMedium

    val bgModifier = if (sticky) {
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    } else modifier

    Column(bgModifier) {
        Text(
            text = text,
            style = if (bold) baseStyle.copy(fontWeight = FontWeight.SemiBold) else baseStyle,
            color = color
        )
        if (bottomSpace > 0.dp) Spacer(Modifier.height(bottomSpace))
    }
}
