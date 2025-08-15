// ui/screens/MyTripsScreen.kt
package com.example.thelastone.ui.screens.MyTrips

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.Trip
import com.example.thelastone.ui.screens.MyTrips.comp.TripList
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.MyTripsUiState
import com.example.thelastone.vm.MyTripsViewModel

@Composable
fun MyTripsScreen(
    padding: PaddingValues,
    openTrip: (String) -> Unit,
    vm: MyTripsViewModel = hiltViewModel()
) {
    val userId = "demo-user"
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load(userId) }
    data class TabSpec(
        val title: String,
        val select: (all: List<Trip>, userId: String) -> List<Trip>,
        val emptyTitle: String,
        val emptyDesc: String
    )

    val tabs = remember {
        listOf(
            TabSpec(
                title = "我建立的",
                select = { all, uid -> all.filter { it.createdBy == uid } },
                emptyTitle = "還沒有建立任何行程",
                emptyDesc = "點擊右下角「＋」建立你的第一個行程"
            ),
            TabSpec(
                title = "我參加的",
                select = { all, uid -> all.filter { it.createdBy != uid && it.members.any { m -> m.id == uid } } },
                emptyTitle = "你還沒有加入任何行程",
                emptyDesc = "邀請朋友或加入他人分享的行程吧"
            )
        )
    }

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = Modifier.padding(padding)) {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, t ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(t.title) }
                )
            }
        }

        // 分頁內容：依全局 state 與當前分頁規則顯示
        when (val s = state) {
            is MyTripsUiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxWidth())
            }

            is MyTripsUiState.Error -> {
                ErrorState(
                    modifier = Modifier.fillMaxWidth(),
                    title = "載入失敗",
                    message = s.message,
                    onRetry = { vm.load(userId) }
                )
            }

            is MyTripsUiState.Empty -> {
                // 全部都空時，顯示「每個分頁自己的 Empty 文案」
                val tab = tabs[selectedTab]
                EmptyState(
                    modifier = Modifier.fillMaxWidth(),
                    title = tab.emptyTitle,
                    description = tab.emptyDesc
                )
            }

            is MyTripsUiState.Data -> {
                val tab = tabs[selectedTab]
                val tabTrips = remember(s.trips, selectedTab, userId) {
                    tab.select(s.trips, userId)
                }
                if (tabTrips.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        title = tab.emptyTitle,
                        description = tab.emptyDesc
                    )
                } else {
                    TripList(trips = tabTrips, openTrip = openTrip)
                }
            }
        }
    }
}