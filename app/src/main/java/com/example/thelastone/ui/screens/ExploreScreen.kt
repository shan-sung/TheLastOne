package com.example.thelastone.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.example.thelastone.utils.getLastKnownLatLngOrNull
import com.example.thelastone.vm.ExploreMode
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // ★ 收藏 VM
    val savedVm: SavedViewModel = hiltViewModel()
    val savedUi by savedVm.state.collectAsState()
    var preview by remember { mutableStateOf<com.example.thelastone.data.model.PlaceLite?>(null) }

    val context = LocalContext.current
    val hasLocationPermission = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission.value = granted
        vm.onLocationPermissionResult(granted)   // ★ 通知 VM 切換模式 / 載入 popular fallback
    }
    val scope = rememberCoroutineScope()

    // 首次進入時：檢查 & 請求
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission.value = granted
        vm.onLocationPermissionResult(granted)   // ★ 讓 VM 先決定 mode & 可能載入 popular

        if (!granted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 權限拿到後：取最後一次定位並載入 Nearby
    LaunchedEffect(hasLocationPermission.value) {
        if (hasLocationPermission.value) {
            val ll = getLastKnownLatLngOrNull(context)

            if (ll == null) {
                // ★ 拿不到定位 → 直接切到 Popular 並載入
                vm.onLocationPermissionResult(false)
                vm.loadPopularSpotsIfNeeded()
                return@LaunchedEffect
            }

            val (lat, lng) = ll
            Log.d("Explore", "loadNearby with ($lat,$lng)")
            vm.loadNearby(lat, lng, radiusMeters = 6000) // 半徑加大一點，比較不容易空清單
        }
    }


    // ========= 依 mode 決定「要顯示的標題 / 資料 / 載入與錯誤」 =========
    val sectionTitle: String
    val placesToShow: List<com.example.thelastone.data.model.PlaceLite>
    val sectionLoading: Boolean
    val sectionError: String?

    when (ui.mode) {
        is ExploreMode.Nearby -> {
            sectionTitle = "Nearby Spots"
            placesToShow = ui.nearby
            sectionLoading = ui.nearbyLoading
            sectionError = ui.nearbyError
        }
        is ExploreMode.Popular -> {
            sectionTitle = "Popular Spots"
            placesToShow = ui.popularSpots
            sectionLoading = ui.popularSpotsLoading
            sectionError = ui.popularSpotsError
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
                // 你的行程卡片維持原樣
                TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4000L
                )

                // ========= 這裡改成吃「動態 mode」的資料 =========
                NearbySection(
                    title = sectionTitle,
                    isLoading = sectionLoading,
                    error = sectionError,
                    places = placesToShow,
                    onOpenPlace = { id -> preview = placesToShow.firstOrNull { it.placeId == id } },
                    savedIds = savedUi.savedIds,
                    onToggleSave = { place -> savedVm.toggle(place) },
                    onRetry = {
                        if (ui.mode is ExploreMode.Nearby && hasLocationPermission.value) {
                            scope.launch {
                                val ll = getLastKnownLatLngOrNull(context) ?: return@launch
                                vm.loadNearby(ll.first, ll.second, radiusMeters = 3000)
                            }
                        } else {
                            // Popular 模式 → 重試熱門景點載入
                            vm.loadPopularSpots()
                        }
                    }
                )

                // ★ 收藏 dialog（原樣）
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
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("目前沒有推薦行程", style = MaterialTheme.typography.bodyMedium) } }
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
                    modifier = Modifier.padding(horizontal = 4.dp).size(size)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = alpha), shape = MaterialTheme.shapes.extraSmall)
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
                    message = "附近景點載入中…"
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
                val (emptyTitle, emptyDesc) =
                    if (title == "Nearby Spots") {
                        "目前找不到附近景點" to "建議開啟定位或稍後再試"
                    } else {
                        "目前找不到熱門景點" to "建議調整搜尋條件或稍後再試"
                    }

                EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    title = emptyTitle,
                    description = emptyDesc
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
                slice.forEach { p -> PlaceCard(
                    place = p,
                    onClick = { onOpenPlace(p.placeId) },
                    isSaved = savedIds.contains(p.placeId),       // ★ 顯示愛心狀態
                    onToggleSave = { onToggleSave(p) }             // ★ 切換收藏
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
                    modifier = Modifier.padding(horizontal = 4.dp).size(size)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = alpha), shape = MaterialTheme.shapes.extraSmall)
                )
            }
        }
    }
}
