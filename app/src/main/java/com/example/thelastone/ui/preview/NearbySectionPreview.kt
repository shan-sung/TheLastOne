package com.example.thelastone.ui.preview

// --- Previews ---
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.thelastone.ui.screens.ExploreScreen
import com.example.thelastone.vm.ExploreUiState

@Preview(showBackground = true, name = "Explore - Loading")
@Composable
private fun ExploreScreenPreview_Loading() {
    MaterialTheme {
        ExploreScreen(
            padding = PaddingValues(0.dp),
            openPlace = {},
            openTrip = {},
            previewUi = ExploreUiState(
                isLoading = true,
                error = null,
                popularTrips = emptyList(),
                isRefreshing = false,
                nearby = emptyList(),
                nearbyError = null,
                nearbyLoading = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Explore - Error")
@Composable
private fun ExploreScreenPreview_Error() {
    MaterialTheme {
        ExploreScreen(
            padding = PaddingValues(0.dp),
            openPlace = {},
            openTrip = {},
            previewUi = ExploreUiState(
                isLoading = false,
                error = "熱門行程載入失敗",
                popularTrips = emptyList(),
                isRefreshing = false,
                nearby = emptyList(),
                nearbyError = null,
                nearbyLoading = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Explore - Empty")
@Composable
private fun ExploreScreenPreview_Empty() {
    MaterialTheme {
        ExploreScreen(
            padding = PaddingValues(0.dp),
            openPlace = {},
            openTrip = {},
            previewUi = ExploreUiState(
                isLoading = false,
                error = null,
                popularTrips = emptyList(),  // 會顯示「目前沒有推薦行程」
                isRefreshing = false,
                nearby = emptyList(),        // 會走 Nearby 的 EmptyState
                nearbyError = null,
                nearbyLoading = false
            )
        )
    }
}
