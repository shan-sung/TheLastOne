package com.example.thelastone.ui.screens.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.SectionHeader
import com.example.thelastone.ui.state.AskLocationState
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreUiState
import kotlin.math.ceil

enum class SpotsTrailing { Refresh, More }

/* ========== 1) State 定義 ========== */
sealed class SpotsStateView {
    data object Loading : SpotsStateView()
    data class Error(val message: String) : SpotsStateView()
    data class Empty(val title: String, val description: String? = null) : SpotsStateView()
    data object AskLocation : SpotsStateView()
    data class Content(val places: List<PlaceLite>) : SpotsStateView()
}

/* ========== 2) State Builder（純函式） ========== */
fun buildPopularState(ui: ExploreUiState): SpotsStateView = when {
    ui.spotsLoading || !ui.spotsInitialized -> SpotsStateView.Loading
    ui.spotsError != null -> SpotsStateView.Error(ui.spotsError ?: "熱門景點載入失敗")
    ui.spots.isEmpty() -> SpotsStateView.Empty("目前找不到推薦景點", "建議稍後再試或調整條件")
    else -> SpotsStateView.Content(ui.spots)
}

fun buildNearbyState(
    ui: ExploreUiState,
    hasPermission: Boolean
): SpotsStateView {
    if (!hasPermission) return SpotsStateView.AskLocation
    return when {
        ui.nearbyLoading || !ui.nearbyInitialized -> SpotsStateView.Loading
        ui.nearbyError != null -> SpotsStateView.Error(ui.nearbyError ?: "附近景點載入失敗")
        ui.nearby.isEmpty() -> SpotsStateView.Empty("附近沒有可推薦的景點", "請稍後再試或檢查網路／定位狀態")
        else -> SpotsStateView.Content(ui.nearby)
    }
}

@Composable
fun SpotsPanel(
    title: String,
    state: SpotsStateView,
    onRefresh: () -> Unit,
    onRetry: () -> Unit = onRefresh,
    onRequestPermission: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    savedIds: Set<String>,
    onToggleSave: (PlaceLite) -> Unit,
    onOpenPlace: (PlaceLite) -> Unit,
    trailingType: SpotsTrailing,
    onClickTrailing: () -> Unit
) {
    // 統一 enabled 規則
    val trailingEnabled = when (trailingType) {
        SpotsTrailing.More -> state is SpotsStateView.Content && state.places.isNotEmpty()
        SpotsTrailing.Refresh -> state !is SpotsStateView.Loading && state !is SpotsStateView.AskLocation
    }

    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            text = title,
            large = false,
            secondaryTone = true,
            bottomSpace = 12.dp,
            sticky = false,
            trailing = {
                IconButton(onClick = onClickTrailing, enabled = trailingEnabled) {
                    // 只有 Loading 顯示轉圈
                    if (state is SpotsStateView.Loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        when (trailingType) {
                            SpotsTrailing.Refresh -> Icon(Icons.Outlined.Refresh, contentDescription = "Refresh $title")
                            SpotsTrailing.More    -> Icon(Icons.Filled.ArrowForward, contentDescription = "More $title")
                        }
                    }
                }
            }
        )

        when (state) {
            is SpotsStateView.Loading -> LoadingState(
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                message = "景點載入中…"
            )
            is SpotsStateView.Error -> ErrorState(
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                title = "載入失敗",
                message = state.message,
                onRetry = onRetry
            )
            is SpotsStateView.Empty -> EmptyState(
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                title = state.title,
                description = state.description
            )
            is SpotsStateView.AskLocation -> AskLocationState(
                onRequestPermission = { onRequestPermission?.invoke() },
                onOpenSettings = { onOpenSettings?.invoke() }
            )
            is SpotsStateView.Content -> SpotsPager(
                places = state.places,
                savedIds = savedIds,
                onToggleSave = onToggleSave,
                onOpenPlace = onOpenPlace
            )
        }
    }
}

/* ========== 4) SpotsPager：分頁卡片 + 指示點 ========== */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotsPager(
    places: List<PlaceLite>,
    savedIds: Set<String>,
    onToggleSave: (PlaceLite) -> Unit,
    onOpenPlace: (PlaceLite) -> Unit,
    itemsPerPage: Int = 3,
    autoScroll: Boolean = true,
    autoScrollMillis: Long = 4_000L,
) {
    if (places.isEmpty()) return

    val pageCount = remember(places, itemsPerPage) {
        maxOf(1, ceil(places.size / itemsPerPage.toFloat()).toInt())
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(pageCount, autoScroll, autoScrollMillis) {
        if (!autoScroll || pageCount <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(autoScrollMillis)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pageCount)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fill,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1)
        ),
        pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
            pagerState, Orientation.Horizontal
        ),
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val start = page * itemsPerPage
        val end = minOf(start + itemsPerPage, places.size)
        val slice = places.subList(start, end)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            slice.forEach { p ->
                PlaceCard(
                    place = p,
                    onClick = { onOpenPlace(p) },  // ← 直接傳 PlaceLite
                    isSaved = p.placeId in savedIds,
                    onToggleSave = { onToggleSave(p) }
                )
            }
            // 對齊高度
            repeat(itemsPerPage - slice.size) { Spacer(Modifier.height(0.dp)) }
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { i ->
            val selected = pagerState.currentPage == i
            val size = if (selected) 8.dp else 6.dp
            val alpha = if (selected) 1f else 0.45f
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(size)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )
        }
    }
}