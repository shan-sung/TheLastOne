package com.example.thelastone.ui.screens.explore

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/* ========== 權限工具 ========== */
private fun Context.hasLocationPermission(): Boolean {
    val fine = androidx.core.content.ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

/* ========== Explore 畫面 ========== */
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
    var preview by rememberSaveable { mutableStateOf<PlaceLite?>(null) }

    // --- 權限請求 Launcher ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            scope.launch { refreshNearby(ctx, viewModel) }
        } else {
            viewModel.markNearbyAsUnavailable()
        }
    }

    val askedOnce = rememberSaveable { mutableStateOf(false) }

    // --- 首次載入：熱門 + 附近 ---
    LaunchedEffect(Unit) {
        viewModel.loadSpotsTaiwan()
        if (ctx.hasLocationPermission()) {
            refreshNearby(ctx, viewModel)
        } else if (!askedOnce.value) {
            askedOnce.value = true
            askForLocation(permissionLauncher)
        } else {
            viewModel.markNearbyAsUnavailable()
        }
    }

    // --- 回到前景後，若剛授權，補抓一次 ---
    val lifecycleOwner = LocalLifecycleOwner.current
    val lastGranted = remember { mutableStateOf(ctx.hasLocationPermission()) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                val now = ctx.hasLocationPermission()
                val was = lastGranted.value
                lastGranted.value = now
                if (now && !was) {
                    scope.launch { refreshNearby(ctx, viewModel) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // --- UI ---
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
                ui.error != null -> ErrorState(
                    Modifier.fillMaxWidth(),
                    "載入熱門行程失敗",
                    ui.error!!,
                    onRetry = { viewModel.retry() }
                )
                else -> TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4_000L
                )
            }
        }

        // 2) Popular Spots（統一 UI）
        // Popular Spots（統一 UI）
        item {
            val state = buildPopularState(ui)
            // NEW: 控制是否顯示篩選面板
            var showFilter by rememberSaveable { mutableStateOf(false) }

            SpotsPanel(
                title = "Popular Spots",
                state = state,
                onRefresh = { viewModel.loadSpotsTaiwan() },
                onRetry = { viewModel.loadSpotsTaiwan() },
                savedIds = savedUi.savedIds,
                onToggleSave = { place -> savedVm.toggle(place) },
                onOpenPlace = { place -> preview = place },   // ← 直接放進 preview
                trailingType = SpotsTrailing.Filter,
                onClickTrailing = { showFilter = true },
                trailingEnabled = state !is SpotsStateView.Loading
            )

            // 這裡你可以放 BottomSheet/Dialog 做篩選 UI（示意）
            if (showFilter) {
                // FilterBottomSheet(...) { appliedFilters ->
                //     viewModel.applySpotFilters(appliedFilters)
                //     showFilter = false
                // }
                // 先佔位：
                LaunchedEffect(Unit) {
                    // TODO: 開你自己的篩選介面
                    showFilter = false
                }
            }
        }


        // 3) Nearby Spots（統一 UI + 權限 StateView）
        item {
            val hasPerm = ctx.hasLocationPermission()
            val state = buildNearbyState(ui, hasPerm)

            SpotsPanel(
                title = "Nearby Spots",
                state = state,
                onRefresh = { if (hasPerm) scope.launch { refreshNearby(ctx, viewModel) } },
                onRetry   = { if (hasPerm) scope.launch { refreshNearby(ctx, viewModel) } },
                onRequestPermission = { askForLocation(permissionLauncher) },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply { data = Uri.fromParts("package", ctx.packageName, null) }
                    ctx.startActivity(intent)
                },
                savedIds = savedUi.savedIds,
                onToggleSave = { place -> savedVm.toggle(place) },
                onOpenPlace = { place -> preview = place },   // ← 同樣直接設置
                trailingType = SpotsTrailing.Refresh,
                onClickTrailing = { if (hasPerm) scope.launch { refreshNearby(ctx, viewModel) } },
                trailingEnabled = state !is SpotsStateView.Loading
            )
        }

        // 4) Dialog
        item {
            preview?.let { p ->
                val isSaved = p.placeId in savedUi.savedIds
                val mode = if (isSaved) PlaceActionMode.REMOVE_FROM_FAVORITE
                else PlaceActionMode.ADD_TO_FAVORITE
                PlaceDetailDialog(
                    place = p,
                    mode = mode,
                    onDismiss = { preview = null },
                    onAddToFavorite = {
                        savedVm.toggle(p); preview = null
                    },
                    onRemoveFromFavorite = {
                        savedVm.toggle(p); preview = null
                    }
                )
            }
        }
    }
}

/* ========== 小小的流程輔助 ========== */
private fun askForLocation(
    launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    launcher.launch(arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ))
}

/* ========== 定位（重試策略） ========== */
data class LatLng(val lat: Double, val lng: Double)

@SuppressLint("MissingPermission")
private suspend fun getLatLngWithRetries(
    ctx: Context,
    tries: Int = 5,
    intervalMs: Long = 600
): LatLng? = withContext(Dispatchers.Main) {
    val client = LocationServices.getFusedLocationProviderClient(ctx)
    repeat(tries) {
        // 1) 快速：lastLocation
        val last = withContext(Dispatchers.IO) { client.lastLocation.awaitNullable() }
        if (last != null) return@withContext LatLng(last.latitude, last.longitude)

        // 2) 次快：getCurrentLocation（有超時）
        val cur = withTimeoutOrNull(1_500L) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .awaitNullable()
        }
        if (cur != null) return@withContext LatLng(cur.latitude, cur.longitude)

        kotlinx.coroutines.delay(intervalMs)
    }
    null
}

private suspend fun <T> Task<T>.awaitNullable(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) {} }
    addOnFailureListener { cont.resume(null) {} }
    addOnCanceledListener { cont.cancel() }
}

private suspend fun refreshNearby(ctx: Context, vm: ExploreViewModel) {
    val p = getLatLngWithRetries(ctx, tries = 5, intervalMs = 600)
    if (p != null) {
        vm.loadNearbyAroundMe(lat = p.lat, lng = p.lng)
    } else {
        val p2 = getLatLngWithRetries(ctx, tries = 3, intervalMs = 800)
        if (p2 != null) vm.loadNearbyAroundMe(lat = p2.lat, lng = p2.lng)
        else vm.markNearbyAsUnavailable()
    }
}