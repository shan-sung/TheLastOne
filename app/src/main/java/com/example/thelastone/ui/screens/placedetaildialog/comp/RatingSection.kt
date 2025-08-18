package com.example.thelastone.ui.screens.placedetaildialog.comp
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateContentSize
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.KeyboardArrowDown
//import androidx.compose.material.icons.filled.KeyboardArrowUp
//import androidx.compose.material.icons.filled.Star
//import androidx.compose.material3.Icon
//import androidx.compose.material3.LinearProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//
//@Composable
//fun RatingSection(
//    rating: Double,
//    totalReviews: Int,
//    modifier: Modifier = Modifier,
//    ratingDistribution: Map<Int, Float> = mapOf(
//        5 to 0.4f,
//        4 to 0.3f,
//        3 to 0.15f,
//        2 to 0.1f,
//        1 to 0.05f
//    )
//) {
//    var expanded by remember { mutableStateOf(false) }
//    val colorScheme = MaterialTheme.colorScheme
//
//    Column(
//        modifier = modifier
//            .fillMaxWidth()
//            .animateContentSize()
//    ) {
//        // ⏰ 營業狀態列（點擊可展開）
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { expanded = !expanded }
//                .padding(vertical = 8.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Star,
//                contentDescription = "Rating",
//                tint = Color(0xFFFFC107),
//                modifier = Modifier.size(20.dp)
//            )
//            Spacer(Modifier.width(8.dp))
//            Text(
//                text = String.format("%.1f", rating),
//                style = MaterialTheme.typography.bodyMedium,
//                color = colorScheme.onSurface
//            )
//            Spacer(Modifier.width(8.dp))
//            Text(
//                text = "$totalReviews 則評價",
//                style = MaterialTheme.typography.bodySmall,
//                color = colorScheme.onSurfaceVariant
//            )
//            Spacer(modifier = Modifier.weight(1f))
//            Icon(
//                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                contentDescription = null,
//                tint = colorScheme.onSurfaceVariant,
//                modifier = Modifier.size(32.dp).padding(end = 8.dp)
//            )
//        }
//
//        // 📊 展開區：評分分布列
//        AnimatedVisibility(visible = expanded) {
//            Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
//                (5 downTo 1).forEach { stars ->
//                    val percent = ratingDistribution[stars] ?: 0f
//                    RatingBar(stars = stars, percent = percent)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun RatingBar(stars: Int, percent: Float) {
//    val starColor = Color(0xFFFFC107)
//
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 2.dp, horizontal = 4.dp)
//    ) {
//        // ⭐ 數字 + icon
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.width(48.dp)
//        ) {
//            Text(
//                text = stars.toString(),
//                style = MaterialTheme.typography.labelMedium
//            )
//            Spacer(Modifier.width(2.dp))
//            Icon(
//                imageVector = Icons.Default.Star,
//                contentDescription = null,
//                tint = starColor,
//                modifier = Modifier.size(16.dp)
//            )
//        }
//
//        // 🔁 進度條
//        LinearProgressIndicator(
//            progress = { percent },
//            modifier = Modifier
//                .weight(1f)
//                .height(6.dp)
//                .clip(RoundedCornerShape(3.dp)),
//            color = starColor,
//            trackColor = MaterialTheme.colorScheme.surfaceVariant
//        )
//    }
//}
