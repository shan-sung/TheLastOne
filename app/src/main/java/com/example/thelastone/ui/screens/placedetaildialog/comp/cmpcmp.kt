package com.example.thelastone.ui.screens.placedetaildialog.comp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.thelastone.utils.formatTimeRange24h
import com.example.thelastone.utils.getOpeningStatusInfo
import java.time.LocalDate
import java.time.LocalTime

/* =============== 共用樣式 token =============== */

private val SectionHPadding = 16.dp
private val SectionVPadding = 12.dp
private val HeaderIconSize = 20.dp
private val HeaderChevronSize = 20.dp
private val RowGap = 8.dp
private val InnerRowHPadding = 12.dp
private val BarHeight = 6.dp
private val BarRadius = 3.dp

/* =============== 共用 Header =============== */

@Composable
private fun ExpandableHeader(
    leadingIcon: @Composable () -> Unit,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = SectionVPadding)
    ) {
        leadingIcon()
        Spacer(Modifier.width(RowGap))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        // 統一用旋轉的 Chevron 表示展開狀態
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(HeaderChevronSize)
                .graphicsLayer { rotationZ = if (expanded) 90f else 0f }
        )
    }
}

/* =============== OpeningHoursSection =============== */

@Composable
fun OpeningHoursSection(
    hours: List<String>,
    statusText: String?,                      // 已計算好的文案（優先）
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    val headerText = statusText ?: run {
        val now = remember { LocalTime.now() }
        getOpeningStatusInfo(hours, now, colorScheme).text
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        ExpandableHeader(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(HeaderIconSize)
                )
            },
            title = headerText,
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )

        AnimatedVisibility(expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SectionHPadding, end = SectionHPadding, bottom = SectionVPadding)
            ) {
                val today = LocalDate.now().dayOfWeek.name
                hours.forEach { line ->
                    val parts = line.split(":", limit = 2).map { it.trim() }
                    val dayEn = parts.getOrNull(0) ?: return@forEach
                    val time = parts.getOrNull(1) ?: ""
                    val isToday = dayEn.uppercase() == today
                    val color = if (isToday) colorScheme.onSurface else colorScheme.onSurfaceVariant

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = InnerRowHPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dayEn.toChineseDay(), color = color, style = MaterialTheme.typography.bodyLarge)
                        Text(formatTimeRange24h(time), color = color, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private fun String.toChineseDay(): String = when (uppercase()) {
    "MONDAY" -> "星期一"
    "TUESDAY" -> "星期二"
    "WEDNESDAY" -> "星期三"
    "THURSDAY" -> "星期四"
    "FRIDAY" -> "星期五"
    "SATURDAY" -> "星期六"
    "SUNDAY" -> "星期日"
    else -> this
}

/* =============== RatingSection =============== */

@Composable
fun RatingSection(
    rating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier,
    ratingDistribution: Map<Int, Float> = mapOf(
        5 to 0.4f, 4 to 0.3f, 3 to 0.15f, 2 to 0.1f, 1 to 0.05f
    )
) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    val header = "${String.format("%.1f", rating)} · ${totalReviews}則評價"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        ExpandableHeader(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(HeaderIconSize)
                )
            },
            title = header,
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )

        AnimatedVisibility(expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SectionHPadding, end = SectionHPadding, bottom = SectionVPadding)
            ) {
                (5 downTo 1).forEach { stars ->
                    val percent = ratingDistribution[stars] ?: 0f
                    RatingBar(stars = stars, percent = percent)
                }
            }
        }
    }
}

@Composable
private fun RatingBar(stars: Int, percent: Float) {
    val colorScheme = MaterialTheme.colorScheme
    val starColor = Color(0xFFFFC107)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = InnerRowHPadding)
    ) {
        // 左側：幾顆星
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(56.dp)) {
            Text(text = stars.toString(), style = MaterialTheme.typography.bodyLarge, color = colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = starColor,
                modifier = Modifier.size(16.dp)
            )
        }
        // 右側：Progress Bar
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                .clip(RoundedCornerShape(BarRadius)),
            color = starColor,
            trackColor = colorScheme.surfaceVariant
        )
    }
}
