package com.example.thelastone.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.thelastone.ui.screens.*
import com.example.thelastone.ui.screens.MyTrips.MyTripsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    // 是否為頂層分頁（控制返回鍵顯示）
    val isTopLevel = remember(currentDest) {
        TOP_LEVEL_DESTINATIONS.any { it.route == currentDest?.route }
    }
    val scroll = pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppTopBar(
                destination = currentDest,
                isTopLevel = isTopLevel,
                onBack = { nav.navigateUp() },
                onExploreSearch = { nav.navigate(MiscRoutes.SearchPlaces) },
                onFriendsSearch = { nav.navigate(MiscRoutes.SearchUsers) },
                onEditProfile = { nav.navigate(MiscRoutes.EditProfile) },
                onOpenTripChat = {
                    val tripId = backStack?.arguments?.getString("tripId") ?: return@AppTopBar
                    nav.navigate(TripRoutes.chat(tripId))
                },
                onOpenTripMore = { /* TODO: 開啟 BottomSheet / Menu */ },
                scrollBehavior = scroll
            )
        },
        bottomBar = {
            AppBottomBar(
                nav = nav,
                currentDestination = currentDest
            )
        },
        // ✅ FAB 放在頂層，依路由顯示
        floatingActionButtonPosition = FabPosition.End,
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            if (currentDest?.route == Root.MyTrips.route) {
                FloatingActionButton(
                    onClick = { nav.navigate(TripRoutes.Flow) },
                    modifier = Modifier
                        .navigationBarsPadding() // 不被手勢列擋住
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create trip")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Root.Explore.route
        ) {
            // ===== 頂層分頁 =====
            composable(Root.Explore.route) {
                ExploreScreen(
                    padding = padding,
                    openPlace = { /* 之後可導到 Place Dialog 或詳情 */ }
                )
            }
            composable(Root.MyTrips.route) {
                MyTripsScreen(
                    padding = padding,
                    openTrip = { id -> nav.navigate(TripRoutes.detail(id)) }
                )
            }
            composable(Root.Friends.route) { FriendsScreen(padding) }
            composable(Root.Saved.route) {
                SavedScreen(
                    padding = padding,
                    openPlace = { /* 之後可導到 Trip 新增流程 */ }
                )
            }
            composable(Root.Profile.route) { ProfileScreen(padding) }

            // ===== Trip 巢狀流程：Create → Preview （共享 VM、一次 pop 退出）=====
            navigation(
                startDestination = TripRoutes.Create,
                route = TripRoutes.Flow
            ) {
                composable(TripRoutes.Create) {
                    CreateTripFormScreen(
                        padding = padding,
                        onPreview = { nav.navigate(TripRoutes.Preview) },
                        onCancel = { nav.navigateUp() }
                    )
                }
                composable(TripRoutes.Preview) {
                    PreviewTripScreen(
                        padding = padding,
                        onConfirmSaved = { tripId ->
                            // 儲存成功 → 退出整個 Flow → 進 Detail
                            nav.navigate(TripRoutes.detail(tripId)) {
                                popUpTo(TripRoutes.Flow) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onBack = { nav.navigateUp() }
                    )
                }
            }

            // ===== Trip 細節頁（可深連）=====
            composable(
                route = TripRoutes.Detail,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { TripDetailScreen(padding = padding) }

            composable(
                route = TripRoutes.Chat,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { TripChatScreen(padding = padding) }

            // ===== 其他功能頁 =====
            composable(MiscRoutes.SearchPlaces) { SearchPlacesScreen(padding) }
            composable(MiscRoutes.SearchUsers)  { SearchUsersScreen(padding) }
            composable(MiscRoutes.EditProfile)  { EditProfileScreen(padding) }
        }
    }

    // 非頂層頁顯示系統返回行為
    BackHandler(enabled = !isTopLevel) { nav.navigateUp() }
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

    val title = when {
        route == Root.Explore.route      -> "Explore"
        route == Root.MyTrips.route      -> "My Trips"
        route == Root.Friends.route      -> "Friends"
        route == Root.Saved.route        -> "Saved"
        route == Root.Profile.route      -> "Profile"
        route == TripRoutes.Create       -> "Create Trip"
        route == TripRoutes.Preview      -> "Preview Trip"
        route == TripRoutes.Detail       -> "Trip Detail"
        route == TripRoutes.Chat         -> "Trip Chat"
        route == MiscRoutes.SearchPlaces -> "Search Places"
        route == MiscRoutes.SearchUsers  -> "Search Users"
        route == MiscRoutes.EditProfile  -> "Edit Profile"
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
                Root.Explore.route -> {
                    IconButton(onClick = onExploreSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search places")
                    }
                }
                Root.Friends.route -> {
                    IconButton(onClick = onFriendsSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search users")
                    }
                }
                Root.Profile.route -> {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                    }
                }
                TripRoutes.Detail -> {
                    IconButton(onClick = onOpenTripChat) {
                        Icon(Icons.Filled.Message, contentDescription = "Trip chat")
                    }
                    IconButton(onClick = onOpenTripMore) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun AppBottomBar(
    nav: NavHostController,
    currentDestination: NavDestination?
) {
    NavigationBar {
        TOP_LEVEL_DESTINATIONS.forEach { dest ->
            val selected = currentDestination.isInHierarchy(dest.route)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(dest.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    }
                },
                icon = { Icon(dest.icon, null) },
                label = { Text(text = stringResource(dest.labelRes)) }
            )
        }
    }
}

private fun NavDestination?.isInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}