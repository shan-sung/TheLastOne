package com.example.thelastone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.thelastone.ui.navigation.Routes
import com.example.thelastone.ui.navigation.TOP_LEVEL_DESTINATIONS
import com.example.thelastone.ui.screens.EditProfileScreen
import com.example.thelastone.ui.screens.ExploreScreen
import com.example.thelastone.ui.screens.FriendsScreen
import com.example.thelastone.ui.screens.MyTripsScreen
import com.example.thelastone.ui.screens.PlaceDetailScreen
import com.example.thelastone.ui.screens.ProfileScreen
import com.example.thelastone.ui.screens.SavedScreen
import com.example.thelastone.ui.screens.SearchPlacesScreen
import com.example.thelastone.ui.screens.SearchUsersScreen
import com.example.thelastone.ui.screens.TripChatScreen
import com.example.thelastone.ui.screens.TripDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    // 是否為頂層頁面（無返回鍵）
    val isTopLevel = remember(currentDest) {
        TOP_LEVEL_DESTINATIONS.any { it.route == currentDest?.route }
    }

    val scrollBehavior = pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppTopBar(
                destination = currentDest,
                isTopLevel = isTopLevel,
                onBack = { navController.navigateUp() },
                onExploreSearch = { navController.navigate(Routes.SearchPlaces.route) },
                onFriendsSearch = { navController.navigate(Routes.SearchUsers.route) },
                onEditProfile   = { navController.navigate(Routes.EditProfile.route) },
                onOpenTripChat  = {
                    val tripId = backStackEntry?.arguments?.getString("tripId") ?: return@AppTopBar
                    navController.navigate(Routes.TripChat.create(tripId))
                },
                onOpenTripMore  = {
                    // 先留空，之後你可以在 TripDetail 裡打開 BottomSheet / OverflowMenu
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            AppBottomBar(
                currentDestination = currentDest,
                onTabSelected = { route ->
                    navController.navigate(route) {
                        // 官方建議：多 back stack
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Explore.route
        ) {
            // Top-level
            composable(Routes.Explore.route) {
                ExploreScreen(
                    padding = innerPadding,
                    openPlace = { /* 暫時不使用：之後可改為直接在搜尋頁導詳情 */ }
                )
            }
            composable(Routes.MyTrips.route) {
                MyTripsScreen(
                    padding = innerPadding,
                    openTrip = { tripId -> navController.navigate(Routes.TripDetail.create(tripId)) }
                )
            }
            composable(Routes.Friends.route) { FriendsScreen(padding = innerPadding) }
            composable(Routes.Saved.route) {
                SavedScreen(
                    padding = innerPadding,
                    openPlace = { /* 同上，先留空 */ }
                )
            }
            composable(Routes.Profile.route) { ProfileScreen(padding = innerPadding) }

            // 子頁
            composable(Routes.TripDetail.route) { TripDetailScreen(padding = innerPadding) }

            // 新增頁（先放空殼，之後你填功能）
            composable(Routes.SearchPlaces.route) { SearchPlacesScreen(padding = innerPadding) }
            composable(Routes.SearchUsers.route)  { SearchUsersScreen(padding = innerPadding) }
            composable(Routes.EditProfile.route)  { EditProfileScreen(padding = innerPadding) }
            composable(Routes.TripChat.route)     { TripChatScreen(padding = innerPadding) }
        }

    }

    // Android 返回鍵：若不是頂層，走 navigateUp()
    BackHandler(enabled = !isTopLevel) {
        navController.navigateUp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    destination: NavDestination?,
    isTopLevel: Boolean,
    onBack: () -> Unit,
    onExploreSearch: () -> Unit,
    onFriendsSearch: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenTripChat: () -> Unit,
    onOpenTripMore: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val route = destination?.route ?: ""

    val title = when (route) {
        Routes.Explore.route      -> "Explore"
        Routes.MyTrips.route      -> "My Trips"
        Routes.Friends.route      -> "Friends"
        Routes.Saved.route        -> "Saved"
        Routes.Profile.route      -> "Profile"
        Routes.TripDetail.route   -> "Trip Detail"
        Routes.SearchPlaces.route -> "Search Places"
        Routes.SearchUsers.route  -> "Search Users"
        Routes.EditProfile.route  -> "Edit Profile"
        Routes.TripChat.route     -> "Trip Chat"
        else -> ""
    }

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (!isTopLevel) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            when (route) {
                Routes.Explore.route -> {
                    IconButton(onClick = onExploreSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search places")
                    }
                }
                Routes.Friends.route -> {
                    IconButton(onClick = onFriendsSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search users")
                    }
                }
                Routes.Profile.route -> {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                    }
                }
                Routes.TripDetail.route -> {
                    IconButton(onClick = onOpenTripChat) {
                        Icon(Icons.Default.Message, contentDescription = "Trip chat")
                    }
                    IconButton(onClick = onOpenTripMore) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun AppBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar {
        TOP_LEVEL_DESTINATIONS.forEach { dest ->
            val selected = currentDestination.isInHierarchy(dest.route)
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(dest.route) },
                icon = { Icon(dest.icon, null) },
                label = { Text(text = androidx.compose.ui.res.stringResource(dest.label)) }
            )
        }
    }
}

private fun NavDestination?.isInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}