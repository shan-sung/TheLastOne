// ui/screens/ExploreScreen.kt
package com.example.thelastone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.Trip
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.TripCard
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreViewModel
import kotlinx.coroutines.delay
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    padding: PaddingValues,
    openPlace: (String) -> Unit,
    openTrip: (String) -> Unit = {},
    // 你可以把定位結果由呼叫端傳進來（或在此用 Location API 取得後呼叫 vm.loadNearby）
    currentLat: Double? = null,
    currentLng: Double? = null
) {
    val vm: ExploreViewModel = hiltViewModel()
    val ui by vm.state.collectAsState()

    // 一進畫面若有定位就載入 Nearby
    LaunchedEffect(currentLat, currentLng) {
        if (currentLat != null && currentLng != null) {
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
            ui.error != null -> ErrorState(Modifier.fillMaxSize(), ui.error!!, onRetry = vm::retry)
            else -> {
                // 1) Popular Trips（你原本的）
                TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4000L
                )

                Spacer(Modifier.height(24.dp))

                // 2) Nearby Places
                Text("Nearby Spots", style = MaterialTheme.typography.titleLarge)
                if (ui.nearbyError != null) {
                    ErrorState(
                        modifier = Modifier.fillMaxWidth(),
                        message = ui.nearbyError!!,
                        onRetry = {
                            if (currentLat != null && currentLng != null) {
                                vm.loadNearby(currentLat, currentLng, 3000.0)
                            }
                        }
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(ui.nearby, key = { it.placeId }) { p ->
                            PlaceCard(
                                place = p,
                                onClick = { openPlace(p.placeId) } // 你已有 openPlace
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TripsSection(
    title: String = "Popular Trips",
    trips: List<Trip>,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
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