package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.SavedViewModel

@Composable
fun SavedScreen(
    padding: PaddingValues,
    openPlace: (String) -> Unit
) {
    val vm: SavedViewModel = hiltViewModel()
    val ui by vm.state.collectAsState()

    // ★ 加：點卡片後要開的預覽 Dialog 狀態
    var preview by remember { mutableStateOf<com.example.thelastone.data.model.PlaceLite?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        when {
            ui.loading -> LoadingState(Modifier.fillMaxSize(), "載入收藏中…")
            ui.error != null -> ErrorState(Modifier.fillMaxSize(), ui.error!!, onRetry = { /* 可觸發重載 */ })
            ui.items.isEmpty() -> EmptyState(Modifier.fillMaxSize(), "尚未收藏景點")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.items) { sp ->
                        // ★ 確保 PlaceLite 欄位齊全（Dialog 會用到 openingHours / openStatusText）
                        val p = PlaceLite(
                            placeId = sp.place.placeId,
                            name = sp.place.name,
                            lat = sp.place.lat,
                            lng = sp.place.lng,
                            address = sp.place.address,
                            rating = sp.place.rating,
                            userRatingsTotal = sp.place.userRatingsTotal,
                            photoUrl = sp.place.photoUrl,
                            openingHours = sp.place.openingHours ?: emptyList(),
                            openStatusText = sp.place.openStatusText
                        )

                        PlaceCard(
                            place = p,
                            // ★ 點擊改為開 Dialog（不直接導頁）
                            onClick = { preview = p },
                            isSaved = true,
                            onToggleSave = { vm.toggle(p) }
                        )
                    }
                }
            }
        }
    }

    // ★ 和 Explore 一樣的 Dialog，但固定用「移除最愛」模式
    if (preview != null) {
        PlaceDetailDialog(
            place = preview,
            mode = PlaceActionMode.REMOVE_FROM_FAVORITE,
            onDismiss = { preview = null },
            onRemoveFromFavorite = {
                preview?.let { vm.toggle(it) }  // 取消收藏
                preview = null
            }
        )
    }
}
