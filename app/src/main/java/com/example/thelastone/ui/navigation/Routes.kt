package com.example.thelastone.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import com.example.thelastone.R

// 頂層分頁（Bottom Bar）
sealed class Root(val route: String) {
    data object Explore : Root("explore")
    data object MyTrips : Root("mytrips")
    data object Friends : Root("friends")
    data object Saved   : Root("saved")
    data object Profile : Root("profile")
}

// Trip 主線：以「巢狀 NavGraph」表達
object TripRoutes {
    // 巢狀圖本身（用於 navController.getBackStackEntry 供共用 VM）
    const val Flow = "trip_flow"

    // Flow 內頁面（Create → Preview）
    const val Create = "trip/create/form"
    const val Preview = "trip/preview"

    // Flow 外（或同層）細節頁（建議不放在 Flow 內，以便直接深連）
    const val Detail = "trip/{tripId}"
    fun detail(tripId: String) = "trip/$tripId"

    const val Chat = "trip/{tripId}/chat"
    fun chat(tripId: String) = "trip/$tripId/chat"
}

// 其他單頁功能
object MiscRoutes {
    const val SearchPlaces = "search/places"
    const val SearchUsers  = "search/users"
    const val EditProfile  = "profile/edit"
}

data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)


val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(Root.Explore.route, R.string.tab_explore, Icons.Filled.TravelExplore),
    TopLevelDestination(Root.MyTrips.route, R.string.tab_mytrips, Icons.Filled.Explore),
    TopLevelDestination(Root.Friends.route, R.string.tab_friends, Icons.Filled.Group),
    TopLevelDestination(Root.Saved.route,   R.string.tab_saved,   Icons.Filled.Star),
    TopLevelDestination(Root.Profile.route, R.string.tab_profile, Icons.Filled.Person),
)
