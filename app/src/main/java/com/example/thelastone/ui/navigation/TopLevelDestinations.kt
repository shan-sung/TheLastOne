package com.example.thelastone.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.thelastone.R

data class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val label: Int
)

val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(Routes.Explore.route, Icons.Filled.TravelExplore, R.string.tab_explore),
    TopLevelDestination(Routes.MyTrips.route, Icons.Filled.Explore, R.string.tab_mytrips),
    TopLevelDestination(Routes.Friends.route, Icons.Filled.Group, R.string.tab_friends),
    TopLevelDestination(Routes.Saved.route, Icons.Filled.Star, R.string.tab_saved),
    TopLevelDestination(Routes.Profile.route, Icons.Filled.Person, R.string.tab_profile),
)