// ui/screens/ExploreScreen.kt
package com.example.thelastone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.Trip
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.TripCard
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreUiState
import com.example.thelastone.vm.ExploreViewModel
import kotlinx.coroutines.delay
import kotlin.math.ceil

// --- ExploreScreen 允許用 previewUi 覆寫狀態（不破壞原本行為） ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    padding: PaddingValues,
    openPlace: (String) -> Unit,
    openTrip: (String) -> Unit = {},
    currentLat: Double? = null,
    currentLng: Double? = null,
    // ✅ 新增：預覽用覆寫狀態；正常運行時保持 null
    previewUi: ExploreUiState? = null,
) {
    val vm: ExploreViewModel = hiltViewModel()
    val ui = previewUi ?: vm.state.collectAsState().value

    LaunchedEffect(currentLat, currentLng) {
        if (previewUi == null && currentLat != null && currentLng != null) {
            vm.loadNearby(currentLat, currentLng, radiusMeters = 3000.0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
    ) {
        when {
            ui.isLoading -> LoadingState(Modifier.fillMaxSize(), "載入熱門行程中…")
            ui.error != null -> ErrorState(Modifier.fillMaxSize(), ui.error!!, onRetry = { if (previewUi == null) vm.retry() })
            else -> {
                TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4000L
                )

                Spacer(Modifier.height(24.dp))

                NearbySection(
                    title = "Nearby Spots",
                    isLoading = ui.nearbyLoading,
                    error = ui.nearbyError,
                    places = ui.nearby,
                    onOpenPlace = openPlace,
                    onRetry = {
                        if (previewUi == null && currentLat != null && currentLng != null) {
                            vm.loadNearby(currentLat, currentLng, radiusMeters = 3000.0)
                        }
                    }
                )
            }
        }
    }
}



@Composable
fun TripsSection(
    modifier: Modifier = Modifier,
    title: String = "Popular Trips",
    trips: List<Trip>,
    onTripClick: (String) -> Unit,
    itemsPerPage: Int = 3,
    autoScroll: Boolean = true,
    autoScrollMillis: Long = 4_000L,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        if (trips.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("目前沒有推薦行程", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return
        }

        val pageCount = remember(trips, itemsPerPage) {
            maxOf(1, ceil(trips.size / itemsPerPage.toFloat()).toInt())
        }
        val pagerState = rememberPagerState(pageCount = { pageCount })

        // 自動輪播
        LaunchedEffect(pageCount, autoScroll, autoScrollMillis) {
            if (!autoScroll || pageCount <= 1) return@LaunchedEffect
            while (true) {
                delay(autoScrollMillis)
                if (!pagerState.isScrollInProgress) {
                    val next = (pagerState.currentPage + 1) % pageCount
                    pagerState.animateScrollToPage(next)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fill, // ✅ 一頁佔滿
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1) // ✅ 放手就貼齊到整頁
            ),
            // 移除 pageSpacing（或設為 0.dp）
            pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
                pagerState, Orientation.Horizontal
            ),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val start = page * itemsPerPage
            val end = minOf(start + itemsPerPage, trips.size)
            val slice: List<Trip> = trips.subList(start, end)

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                slice.forEach { trip ->
                    TripCard(
                        trip = trip,
                        onClick = { onTripClick(trip.id) },
                        imageUrl = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 補齊高度（保持各頁一致）
                repeat(itemsPerPage - slice.size) { Spacer(Modifier.height(0.dp)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
}

@Composable
fun NearbySection(
    modifier: Modifier = Modifier,
    title: String = "Nearby Spots",
    isLoading: Boolean,
    error: String?,
    places: List<com.example.thelastone.data.model.PlaceLite>,
    onOpenPlace: (String) -> Unit,
    onRetry: () -> Unit = {},
    itemsPerPage: Int = 3,
    autoScroll: Boolean = true,
    autoScrollMillis: Long = 4_000L
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

        when {
            isLoading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    message = "附近景點載入中…"
                )
                return
            }
            error != null -> {
                ErrorState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    message = error,
                    onRetry = onRetry
                )
                return
            }
            places.isEmpty() -> {
                EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),    // ✅ 保持和 loading/error 區塊一致的高度
                    title = "目前找不到附近景點",
                    description = "建議調整搜尋範圍或檢查定位設定"
                )
                return
            }
        }

        // ---- 與 TripsSection 相同的分頁/輪播邏輯 ----
        val pageCount = remember(places, itemsPerPage) {
            maxOf(1, ceil(places.size / itemsPerPage.toFloat()).toInt())
        }
        val pagerState = rememberPagerState(pageCount = { pageCount })

        LaunchedEffect(pageCount, autoScroll, autoScrollMillis) {
            if (!autoScroll || pageCount <= 1) return@LaunchedEffect
            while (true) {
                delay(autoScrollMillis)
                if (!pagerState.isScrollInProgress) {
                    val next = (pagerState.currentPage + 1) % pageCount
                    pagerState.animateScrollToPage(next)
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
                        onClick = { onOpenPlace(p.placeId) }
                    )
                }
                // 補齊高度（保持各頁一致）
                repeat(itemsPerPage - slice.size) { Spacer(Modifier.height(0.dp)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
}