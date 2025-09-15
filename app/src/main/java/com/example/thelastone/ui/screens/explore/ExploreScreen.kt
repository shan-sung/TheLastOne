package com.example.thelastone.ui.screens.explore

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.SectionHeader
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.AskLocationState
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreUiState
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil

// 放在 ExploreScreen 同檔或 ui/state 資料夾
sealed class SpotsStateView {
    data object Loading : SpotsStateView()
    data class Error(val message: String) : SpotsStateView()
    data class Empty(val title: String, val description: String? = null) : SpotsStateView()
    data object AskLocation : SpotsStateView()
    data class Content(val places: List<PlaceLite>) : SpotsStateView()
}

@Composable
private fun buildPopularState(ui: ExploreUiState): SpotsStateView {
    return when {
        ui.spotsLoading || !ui.spotsInitialized -> SpotsStateView.Loading
        ui.spotsError != null -> SpotsStateView.Error(ui.spotsError ?: "熱門景點載入失敗")
        ui.spots.isEmpty() -> SpotsStateView.Empty("目前找不到推薦景點", "建議稍後再試或調整條件")
        else -> SpotsStateView.Content(ui.spots)
    }
}

@Composable
private fun buildNearbyState(
    ui: ExploreUiState,
    hasPermission: Boolean
): SpotsStateView {
    if (!hasPermission) return SpotsStateView.AskLocation
    return when {
        !ui.nearbyInitialized && !ui.nearbyLoading -> SpotsStateView.Loading
        ui.nearbyLoading -> SpotsStateView.Loading
        ui.nearbyError != null -> SpotsStateView.Error(ui.nearbyError ?: "附近景點載入失敗")
        ui.nearby.isEmpty() -> SpotsStateView.Empty("附近沒有可推薦的景點", "建議稍後再試或檢查網路／定位狀態")
        else -> SpotsStateView.Content(ui.nearby)
    }
}
@Composable
private fun SpotsPanel(
    title: String,
    state: SpotsStateView,
    // 動作
    onRefresh: () -> Unit,
    onRetry: () -> Unit = onRefresh,
    onRequestPermission: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    // 列表互動
    savedIds: Set<String>,
    onToggleSave: (PlaceLite) -> Unit,
    onOpenPlace: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // 標題只畫一次
        SectionHeader(
            text = title,
            large = false,
            secondaryTone = true,
            bottomSpace = 12.dp,
            sticky = false,
            trailing = {
                IconButton(
                    onClick = onRefresh,
                    enabled = state is SpotsStateView.Content || state is SpotsStateView.Empty || state is SpotsStateView.Error
                ) {
                    if (state is SpotsStateView.Loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh $title")
                    }
                }
            }
        )

        when (state) {
            is SpotsStateView.Loading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    message = "景點載入中…"
                )
            }
            is SpotsStateView.Error -> {
                ErrorState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    title = "載入失敗",
                    message = state.message,
                    onRetry = onRetry
                )
            }
            is SpotsStateView.Empty -> {
                EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    title = state.title,
                    description = state.description
                )
            }
            is SpotsStateView.AskLocation -> {
                AskLocationState(
                    onRequestPermission = { onRequestPermission?.invoke() },
                    onOpenSettings = { onOpenSettings?.invoke() }
                )
            }
            is SpotsStateView.Content -> {
                SpotsPager(
                    places = state.places,
                    savedIds = savedIds,
                    onToggleSave = onToggleSave,
                    onOpenPlace = onOpenPlace
                )
            }
        }
    }
}

@Composable
private fun SpotsPager(
    places: List<PlaceLite>,
    savedIds: Set<String>,
    onToggleSave: (PlaceLite) -> Unit,
    onOpenPlace: (String) -> Unit,
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

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    savedVm: SavedViewModel = hiltViewModel(),
    padding: PaddingValues = PaddingValues(0.dp),
    openTrip: (String) -> Unit = {},
    openPlace: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val ui by viewModel.state.collectAsState()
    val savedUi by savedVm.state.collectAsState()
    var preview by remember { mutableStateOf<PlaceLite?>(null) }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            scope.launch {
                val p = getLatLngWithRetries(ctx, tries = 5, intervalMs = 600)
                if (p != null) viewModel.loadNearbyAroundMe(lat = p.lat, lng = p.lng) else viewModel.markNearbyAsUnavailable()
            }
        } else {
            viewModel.markNearbyAsUnavailable()
        }
    }
    val askedOnce = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSpotsTaiwan()
        if (hasLocationPermission()) {
            val p = getLatLngWithRetries(ctx, tries = 5, intervalMs = 600)
            if (p != null) viewModel.loadNearbyAroundMe(lat = p.lat, lng = p.lng) else viewModel.markNearbyAsUnavailable()
        } else if (!askedOnce.value) {
            askedOnce.value = true
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            viewModel.markNearbyAsUnavailable()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lastGranted = remember { mutableStateOf(hasLocationPermission()) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                val now = hasLocationPermission()
                val was = lastGranted.value
                lastGranted.value = now
                if (now && !was) {
                    scope.launch {
                        val p = getLatLngWithRetries(ctx, tries = 5, intervalMs = 600)
                        if (p != null) viewModel.loadNearbyAroundMe(lat = p.lat, lng = p.lng)
                        else {
                            launch {
                                delay(2000)
                                val p2 = getLatLngWithRetries(ctx, tries = 3, intervalMs = 800)
                                if (p2 != null) viewModel.loadNearbyAroundMe(lat = p2.lat, lng = p2.lng)
                                else viewModel.markNearbyAsUnavailable()
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // ---- LazyColumn ----
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1) Popular Trips
        item {
            when {
                ui.isLoading -> LoadingState(Modifier.fillMaxWidth(), "載入熱門行程中…")
                ui.error != null -> ErrorState(Modifier.fillMaxWidth(), "載入熱門行程失敗", ui.error!!, onRetry = { viewModel.retry() })
                else -> TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4000L
                )
            }
        }

        // 2) Popular Spots（統一 UI）
        item {
            val state = buildPopularState(ui)
            SpotsPanel(
                title = "Popular Spots",
                state = state,
                onRefresh = { viewModel.loadSpotsTaiwan() },
                onRetry = { viewModel.loadSpotsTaiwan() },
                savedIds = savedUi.savedIds,
                onToggleSave = { place -> savedVm.toggle(place) },
                onOpenPlace = { id -> preview = ui.spots.firstOrNull { it.placeId == id } }
            )
        }

        // 3) Nearby Spots（統一 UI + 權限 StateView）
        item {
            val hasPerm = hasLocationPermission()
            val state = buildNearbyState(ui, hasPerm)
            SpotsPanel(
                title = "Nearby Spots",
                state = state,
                onRefresh = {
                    if (hasPerm) {
                        scope.launch {
                            val p = getLatLngWithRetries(ctx, tries = 5, intervalMs = 600)
                            if (p != null) viewModel.loadNearbyAroundMe(lat = p.lat, lng = p.lng)
                            else viewModel.markNearbyAsUnavailable()
                        }
                    }
                },
                onRetry = {
                    if (hasPerm) {
                        scope.launch {
                            val p = getLatLngWithRetries(ctx, tries = 5, intervalMs = 600)
                            if (p != null) viewModel.loadNearbyAroundMe(lat = p.lat, lng = p.lng)
                            else viewModel.markNearbyAsUnavailable()
                        }
                    }
                },
                onRequestPermission = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", ctx.packageName, null)
                    }
                    ctx.startActivity(intent)
                },
                savedIds = savedUi.savedIds,
                onToggleSave = { place -> savedVm.toggle(place) },
                onOpenPlace = { id -> preview = ui.nearby.firstOrNull { it.placeId == id } }
            )
        }

        // Dialog 區
        item {
            if (preview != null) {
                val isSaved = savedUi.savedIds.contains(preview!!.placeId)
                val mode = if (isSaved) PlaceActionMode.REMOVE_FROM_FAVORITE else PlaceActionMode.ADD_TO_FAVORITE
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

/* =================== 以下是「兩階段取位 + 重試」工具 =================== */

data class LatLng(val lat: Double, val lng: Double)

@SuppressLint("MissingPermission")
private suspend fun getLatLngWithRetries(
    ctx: Context,
    tries: Int = 5,
    intervalMs: Long = 600
): LatLng? = withContext(Dispatchers.Main) {
    val client = LocationServices.getFusedLocationProviderClient(ctx)
    repeat(tries) {
        // 1) 先試 lastLocation（快）
        val last = withContext(Dispatchers.IO) { client.lastLocation.awaitNullable() }
        if (last != null) return@withContext LatLng(last.latitude, last.longitude)

        // 2) 再試 single fix（可能要等）
        val cur = withTimeoutOrNull(1500L) {
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, /* cancellationToken = */ null
            ).awaitNullable()
        }
        if (cur != null) return@withContext LatLng(cur.latitude, cur.longitude)

        // 3) 下一輪
        delay(intervalMs)
    }
    return@withContext null
}

// Task<T> → suspend（nullable 版）
private suspend fun <T> Task<T>.awaitNullable(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) {} }
    addOnFailureListener { cont.resume(null) {} }
    addOnCanceledListener { cont.cancel() }
}