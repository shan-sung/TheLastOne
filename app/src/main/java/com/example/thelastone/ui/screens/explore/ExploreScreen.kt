package com.example.thelastone.ui.screens.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel

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
                TripsSection(
                    title = "Popular Trips",
                    trips = ui.popularTrips,
                    onTripClick = openTrip,
                    itemsPerPage = 3,
                    autoScroll = true,
                    autoScrollMillis = 4000L
                )

                SpotsSection(
                    title = "Popular Spots",
                    isLoading = ui.spotsLoading,
                    error = ui.spotsError,
                    places = ui.spots,
                    onOpenPlace = { id -> preview = ui.spots.firstOrNull { it.placeId == id } }, // ← 改這裡
                    savedIds = savedUi.savedIds,
                    onToggleSave = { place -> savedVm.toggle(place) },
                    onRetry = vm::loadSpots
                )

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