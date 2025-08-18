package com.example.thelastone.ui.screens.comp.placedetaildialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.ActionButtonsRow
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.ImgSection
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.MapSection
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.OpeningHoursSection
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.RatingSection

@Composable
private fun ColumnScope.Section(
    visible: Boolean,
    topSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return
    Spacer(Modifier.height(topSpacing))
    content()
}

@Composable
fun PlaceDetailDialog(
    place: PlaceLite?,                       // ← 改用你現有的 PlaceLite
    mode: PlaceActionMode = PlaceActionMode.ADD_TO_ITINERARY, // 預設用途：加入行程
    onDismiss: () -> Unit,
    onAddToItinerary: () -> Unit = {},
    onRemoveFromFavorite: () -> Unit = {},
    onAddToFavorite: () -> Unit = {}
) {
    if (place == null) {
        // 與原版相同：loading 外觀
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.9f)      // ✅ 可選：避免溢出
                    .heightIn(max = 600.dp),  // 與上面互相保護
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 76.dp) // 給底部按鈕列空間
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1) 圖片：沒有就完全不佔位
                    place.photoUrl?.let { url ->
                        ImgSection(url = url)
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        // 2) 名稱（一定有）
                        Text(place.name, style = MaterialTheme.typography.titleLarge)

                        // 3) 地址：只有有值才顯示，也只在顯示時加間距
                        Section(visible = !place.address.isNullOrBlank(), topSpacing = 4.dp) {
                            Text(place.address.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        }

                        // 4) 營業資訊：兩者有其一才顯示
                        Section(
                            visible = place.openingHours.isNotEmpty() || place.openStatusText != null,
                            topSpacing = 8.dp
                        ) {
                            OpeningHoursSection(
                                hours = place.openingHours,
                                statusText = place.openStatusText
                            )
                        }

                        // 5) 評分：有 rating 才顯示
                        Section(visible = place.rating != null, topSpacing = 8.dp) {
                            RatingSection(
                                rating = place.rating ?: 0.0,
                                totalReviews = place.userRatingsTotal ?: 0
                            )
                        }

                        // 6) 地圖：照舊（通常會想 always 顯示）
                        Section(visible = true, topSpacing = 12.dp) {
                            MapSection(lat = place.lat, lng = place.lng)
                        }
                    }
                }

                // 底部固定按鈕列
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ActionButtonsRow(
                        place = place,
                        rightButtonLabel = when (mode) {
                            PlaceActionMode.ADD_TO_ITINERARY -> "加入行程"
                            PlaceActionMode.ADD_TO_FAVORITE -> "加入最愛"
                            PlaceActionMode.REMOVE_FROM_FAVORITE -> "移除最愛"
                        },
                        onRightButtonClick = {
                            when (mode) {
                                PlaceActionMode.ADD_TO_ITINERARY -> onAddToItinerary()
                                PlaceActionMode.ADD_TO_FAVORITE -> onAddToFavorite()
                                PlaceActionMode.REMOVE_FROM_FAVORITE -> onRemoveFromFavorite()
                            }
                        },
                        onLeftCancel = onDismiss
                    )
                }
            }
        }
    }
}