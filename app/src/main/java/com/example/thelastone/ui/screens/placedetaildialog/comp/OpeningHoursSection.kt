package com.example.thelastone.ui.screens.placedetaildialog.comp
//
//import androidx.compose.animation.animateContentSize
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AccessTime
//import androidx.compose.material.icons.filled.KeyboardArrowDown
//import androidx.compose.material.icons.filled.KeyboardArrowUp
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.example.thelastone.utils.formatTimeRange24h
//import com.example.thelastone.utils.getOpeningStatusInfo
//import java.time.LocalDate
//import java.time.LocalTime
//
//@Composable
//fun OpeningHoursSection(
//    hours: List<String>,
//    statusText: String?,                      // ✅ 新增：直接吃算好的文案
//    modifier: Modifier = Modifier
//) {
//    var expanded by remember { mutableStateOf(false) }
//    val colorScheme = MaterialTheme.colorScheme
//
//    val header = statusText ?: run {
//        // 只有真的沒有算好的文案時，才用舊邏輯做 fallback
//        val now = remember { LocalTime.now() }
//        getOpeningStatusInfo(hours, now, colorScheme).text
//    }
//
//    Column(
//        modifier = modifier.fillMaxWidth().animateContentSize()
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.fillMaxWidth()
//                .clickable { expanded = !expanded }
//                .padding(vertical = 8.dp)
//        ) {
//            Icon(Icons.Default.AccessTime, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
//            Spacer(Modifier.width(8.dp))
//            Text(text = header, color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
//            Spacer(modifier = Modifier.weight(1f))
//            Icon(
//                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                contentDescription = null,
//                tint = colorScheme.onSurfaceVariant,
//                modifier = Modifier.size(32.dp)
//            )
//        }
//
//        if (expanded) {
//            // 展開時仍顯示 weekdayDescriptions（可搭配 formatTimeRange24h 做美化）
//            Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
//                val today = LocalDate.now().dayOfWeek.name
//                hours.forEach { line ->
//                    val parts = line.split(":", limit = 2).map { it.trim() }
//                    val dayEn = parts.getOrNull(0) ?: return@forEach
//                    val time = parts.getOrNull(1) ?: ""
//                    val isToday = dayEn.uppercase() == today
//                    val color = if (isToday) colorScheme.onSurface else colorScheme.onSurfaceVariant
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 12.dp)
//                    ) {
//                        Text(dayEn.toChineseDay(), color = color)
//                        Text(formatTimeRange24h(time), color = color)
//                    }
//                }
//            }
//        }
//    }
//}
//
//// 小工具
//private fun String.toChineseDay(): String = when (uppercase()) {
//    "MONDAY" -> "星期一"; "TUESDAY" -> "星期二"; "WEDNESDAY" -> "星期三"
//    "THURSDAY" -> "星期四"; "FRIDAY" -> "星期五"; "SATURDAY" -> "星期六"; "SUNDAY" -> "星期日"
//    else -> this
//}