package com.example.thelastone.ui.navigation

sealed class Routes(val route: String) {
    // Top-level
    data object Explore : Routes("explore")
    data object MyTrips : Routes("mytrips")
    data object Friends : Routes("friends")
    data object Saved   : Routes("saved")
    data object Profile : Routes("profile")

    // 子頁
    data object TripDetail : Routes("trip/{tripId}") {
        fun create(tripId: String) = "trip/$tripId"
    }

    // 新增：全螢幕搜尋 / 編輯 / 聊天
    data object SearchPlaces : Routes("search/places")
    data object SearchUsers  : Routes("search/users")
    data object EditProfile  : Routes("profile/edit")
    data object TripChat     : Routes("trip/{tripId}/chat") {
        fun create(tripId: String) = "trip/$tripId/chat"
    }
}
