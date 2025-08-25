package com.example.thelastone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.Trip
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.TripCard
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel
import kotlinx.coroutines.delay
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    padding: PaddingValues,
    openPlace: (String) -> Unit,
    openTrip: (String) -> Unit = {},
) {
    val vm: ExploreViewModel = hiltViewModel()
    val ui by vm.state.collectAsState()

    // 收藏 VM
    val savedVm: SavedViewModel = hiltViewModel()
    val savedUi by savedVm.state.collectAsState()
    var preview by remember { mutableStateOf<PlaceLite?>(null) }

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
                // 行程區（維持原樣）
                TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4000L
                )

                // 只顯示 Popular Spots
                SpotsSection(
                    title = "Popular Spots",
                    isLoading = ui.spotsLoading,          // ← 改這裡
                    error = ui.spotsError,                // ← 改這裡
                    places = ui.spots,                    // ← 改這裡
                    onOpenPlace = { id -> preview = ui.spots.firstOrNull { it.placeId == id } }, // ← 改這裡
                    savedIds = savedUi.savedIds,
                    onToggleSave = { place -> savedVm.toggle(place) },
                    onRetry = vm::loadSpots               // ← 改這裡
                )


                // 收藏 dialog（維持原樣）
                if (preview != null) {
                    val isSaved = savedUi.savedIds.contains(preview!!.placeId)
                    val mode = if (isSaved) PlaceActionMode.REMOVE_FROM_FAVORITE
                    else PlaceActionMode.ADD_TO_FAVORITE

                    PlaceDetailDialog(
                        place = preview,
                        mode = mode,
                        onDismiss = { preview = null },
                        onAddToFavorite = {
                            preview?.let { savedVm.toggle(it) }
                            preview = null
                        },
                        onRemoveFromFavorite = {
                            preview?.let { savedVm.toggle(it) }
                            preview = null
                        }
                    )
                }
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
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        if (trips.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("目前沒有推薦行程", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return
        }
        val pageCount = remember(trips, itemsPerPage) { maxOf(1, ceil(trips.size / itemsPerPage.toFloat()).toInt()) }
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
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, pagerSnapDistance = PagerSnapDistance.atMost(1)),
            pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Horizontal),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val start = page * itemsPerPage
            val end = minOf(start + itemsPerPage, trips.size)
            val slice = trips.subList(start, end)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                slice.forEach { trip ->
                    TripCard(trip = trip, onClick = { onTripClick(trip.id) }, imageUrl = null, modifier = Modifier.fillMaxWidth())
                }
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
}

@Composable
fun SpotsSection(
    modifier: Modifier = Modifier,
    title: String = "Popular Spots",
    isLoading: Boolean,
    error: String?,
    places: List<PlaceLite>,
    onOpenPlace: (String) -> Unit,
    onRetry: () -> Unit = {},
    itemsPerPage: Int = 3,
    autoScroll: Boolean = true,
    autoScrollMillis: Long = 4_000L,
    savedIds: Set<String> = emptySet(),
    onToggleSave: (PlaceLite) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

        when {
            isLoading -> {
                LoadingState(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    message = "景點載入中…"
                )
                return
            }
            error != null -> {
                ErrorState(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    message = error,
                    onRetry = onRetry
                )
                return
            }
            places.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    title = "目前找不到推薦景點",
                    description = "建議稍後再試或調整條件"
                )
                return
            }
        }

        val pageCount = remember(places, itemsPerPage) { maxOf(1, ceil(places.size / itemsPerPage.toFloat()).toInt()) }
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
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, pagerSnapDistance = PagerSnapDistance.atMost(1)),
            pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Horizontal),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val start = page * itemsPerPage
            val end = minOf(start + itemsPerPage, places.size)
            val slice = places.subList(start, end)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                slice.forEach { p ->
                    PlaceCard(
                        place = p,
                        onClick = { onOpenPlace(p.placeId) },
                        isSaved = savedIds.contains(p.placeId),
                        onToggleSave = { onToggleSave(p) }
                    )
                }
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
}
