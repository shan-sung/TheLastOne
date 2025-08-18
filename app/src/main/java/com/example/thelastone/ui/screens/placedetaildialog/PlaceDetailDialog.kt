package com.example.thelastone.ui.screens.placedetaildialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.placedetaildialog.comp.ActionButtonsRow
import com.example.thelastone.ui.screens.placedetaildialog.comp.ImgSection
import com.example.thelastone.ui.screens.placedetaildialog.comp.MapSection
import com.example.thelastone.ui.screens.placedetaildialog.comp.OpeningHoursSection
import com.example.thelastone.ui.screens.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.screens.placedetaildialog.comp.RatingSection
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
                        .padding(bottom = 76.dp) // 讓底部按鈕列不被內容遮到
                        .verticalScroll(rememberScrollState())
                ) {
                    // 圖片區（沿用原本 ImgSection 的位置）
                    place.photoUrl?.let { ImgSection(url = it) }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(place.address.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))

                        if (place.openingHours.isNotEmpty() || place.openStatusText != null) {
                            OpeningHoursSection(
                                hours = place.openingHours,
                                statusText = place.openStatusText
                            )
                        }

                        place.rating?.let { rating ->
                            RatingSection(rating = rating, totalReviews = place.userRatingsTotal ?: 0)
                        }
                        MapSection(lat = place.lat, lng = place.lng)
                    }
                }

                // 底部固定操作列（沿用 ActionButtonsRow 的位置與行為）
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