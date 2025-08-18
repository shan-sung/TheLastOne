package com.example.thelastone.ui.screens

// ⚠️ 依你的檔案路徑調整 import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel

@Composable
fun PickPlaceScreen(
    padding: PaddingValues,
    onSearchClick: () -> Unit,          // 進入全螢幕搜尋頁
    onPick: (PlaceLite) -> Unit         // 按「加入行程」後回傳
) {
    val tabs = listOf("Popular", "Saved")
    var selected by remember { mutableStateOf(0) }

    // ★ 收藏 VM（讓兩個分頁都能顯示愛心＋切換）
    val savedVm: SavedViewModel = hiltViewModel()
    val savedUi by savedVm.state.collectAsState()

    // ★ Dialog 狀態：目前挑選中的地點
    var preview by remember { mutableStateOf<PlaceLite?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // 搜尋欄（點擊跳搜尋頁）
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            readOnly = true,
            placeholder = { Text("搜尋景點、地址…") },
            trailingIcon = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            }
        )

        TabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = selected == i,
                    onClick = { selected = i },
                    text = { Text(t) }
                )
            }
        }

        when (selected) {
            0 -> PopularTab(
                onCardClick = { preview = it },
                isSaved = { id -> savedUi.savedIds.contains(id) },
                onToggleSave = { savedVm.toggle(it) }
            )
            1 -> SavedTab(
                onCardClick = { preview = it },
                onToggleSave = { savedVm.toggle(it) }
            )
        }
    }

    // ★ 詳情 Dialog：按右下角「加入行程」→ 呼叫 onPick，並關閉
    if (preview != null) {
        PlaceDetailDialog(
            place = preview,
            mode = PlaceActionMode.ADD_TO_ITINERARY,
            onDismiss = { preview = null },
            onAddToItinerary = {
                preview?.let(onPick)
                preview = null
            }
        )
    }
}

@Composable
private fun PopularTab(
    onCardClick: (PlaceLite) -> Unit,
    isSaved: (String) -> Boolean,
    onToggleSave: (PlaceLite) -> Unit,
    vm: ExploreViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsState()
    when {
        ui.nearbyLoading -> LoadingState(Modifier.fillMaxSize(), "載入中…")
        ui.nearbyError != null -> ErrorState(Modifier.fillMaxSize(), ui.nearbyError!!, onRetry = { /* TODO: 重新抓 */ })
        else -> {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                ui.nearby.forEach { p ->
                    PlaceCard(
                        place = p,
                        onClick = { onCardClick(p) },
                        isSaved = isSaved(p.placeId),
                        onToggleSave = { onToggleSave(p) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SavedTab(
    onCardClick: (PlaceLite) -> Unit,
    onToggleSave: (PlaceLite) -> Unit,
    vm: SavedViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsState()
    when {
        ui.loading -> LoadingState(Modifier.fillMaxSize(), "載入收藏中…")
        ui.error != null -> ErrorState(Modifier.fillMaxSize(), ui.error!!, onRetry = { })
        ui.items.isEmpty() -> EmptyState(Modifier.fillMaxSize(), "尚未收藏景點")
        else -> {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                ui.items.forEach { sp ->
                    val p = PlaceLite(
                        placeId = sp.place.placeId,
                        name = sp.place.name,
                        lat = sp.place.lat,
                        lng = sp.place.lng,
                        address = sp.place.address,
                        rating = sp.place.rating,
                        userRatingsTotal = sp.place.userRatingsTotal,
                        photoUrl = sp.place.photoUrl
                    )
                    PlaceCard(
                        place = p,
                        onClick = { onCardClick(p) },
                        isSaved = true,
                        onToggleSave = { onToggleSave(p) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}