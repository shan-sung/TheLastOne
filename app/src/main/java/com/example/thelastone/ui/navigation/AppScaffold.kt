package com.example.thelastone.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.example.thelastone.ui.screens.AddActivityScreen
import com.example.thelastone.ui.screens.form.CreateTripFormScreen
import com.example.thelastone.ui.screens.EditProfileScreen
import com.example.thelastone.ui.screens.ExploreScreen
import com.example.thelastone.ui.screens.FriendsScreen
import com.example.thelastone.ui.screens.auth.LoginScreen
import com.example.thelastone.ui.screens.PickPlaceScreen
import com.example.thelastone.ui.screens.myTrips.MyTripsScreen
import com.example.thelastone.ui.screens.PreviewTripScreen
import com.example.thelastone.ui.screens.ProfileScreen
import com.example.thelastone.ui.screens.auth.RegisterScreen
import com.example.thelastone.ui.screens.SavedScreen
import com.example.thelastone.ui.screens.SearchPlacesScreen
import com.example.thelastone.ui.screens.SearchUsersScreen
import com.example.thelastone.ui.screens.TripChatScreen
import com.example.thelastone.ui.screens.TripDetailScreen
import com.example.thelastone.utils.encodePlaceArg
import com.example.thelastone.vm.RootViewModel
import com.example.thelastone.vm.TripFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val rootVm: RootViewModel = hiltViewModel()
    val auth by rootVm.auth.collectAsState()

    if (auth == null) {
        // 未登入：自己一顆 NavController
        val authNav = rememberNavController()
        AuthNavHost(nav = authNav)
    } else {
        // 已登入：自己一顆 NavController
        val mainNav = rememberNavController()
        MainScaffold(nav = mainNav)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    val isTopLevel = remember(currentDest) {
        TOP_LEVEL_DESTINATIONS.any { it.route == currentDest?.route }
    }
    val scroll = pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            if (currentDest?.route != MiscRoutes.SearchPlaces) {
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
                    onOpenTripMore = { /* TODO */ },
                    scrollBehavior = scroll
                )
            }
        },
        bottomBar = {
            if (currentDest?.route !in NO_BOTTOM_BAR_ROUTES) {
                AppBottomBar(nav = nav, currentDestination = currentDest)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            if (currentDest?.route == Root.MyTrips.route) {
                FloatingActionButton(onClick = {
                    nav.navigate(TripRoutes.Flow) { launchSingleTop = true }
                }) { Icon(Icons.Filled.Add, null) }
            }
        }
    ) { padding ->
        MainNavHost(nav = nav, padding = padding)
    }

    BackHandler(enabled = !isTopLevel) { nav.navigateUp() }
}

@Composable
private fun MainNavHost(
    nav: NavHostController,
    padding: PaddingValues
) {
    NavHost(
        navController = nav,
        startDestination = Root.Explore.route
    ) {
        // ===== 頂層分頁 =====
        composable(Root.Explore.route) {
            ExploreScreen(
                padding = padding,
                openPlace = { placeId -> /* 之後做附近景點用 */ },
                openTrip = { tripId -> nav.navigate(TripRoutes.detail(tripId)) }
            )
        }
        composable(Root.MyTrips.route) {
            MyTripsScreen(padding = padding, openTrip = { id -> nav.navigate(TripRoutes.detail(id)) })
        }
        composable(Root.Friends.route) { FriendsScreen(padding) }
        composable(Root.Saved.route)   { SavedScreen(padding = padding, openPlace = { /* TODO */ }) }
        composable(Root.Profile.route) { ProfileScreen(padding) }

        // ===== Trip 巢狀流程 =====
        navigation(startDestination = TripRoutes.Create, route = TripRoutes.Flow) {
            composable(TripRoutes.Create) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(TripRoutes.Flow) }
                val vm: TripFormViewModel = hiltViewModel(parent)
                CreateTripFormScreen(padding = padding, onPreview = { nav.navigate(TripRoutes.Preview) }, viewModel = vm)
            }
            composable(TripRoutes.Preview) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(TripRoutes.Flow) }
                val vm: TripFormViewModel = hiltViewModel(parent)
                PreviewTripScreen(
                    padding = padding,
                    onConfirmSaved = { tripId ->
                        nav.navigate(TripRoutes.detail(tripId)) {
                            popUpTo(TripRoutes.Flow) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { nav.navigateUp() },
                    viewModel = vm
                )
            }
        }

        // ===== Trip 細節與聊天 =====
        composable(
            route = TripRoutes.Detail,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { entry ->
            TripDetailScreen(
                padding = padding,
                onAddActivity = { id -> nav.navigate(TripRoutes.pickPlace(id)) },
                onEditActivity = { id, dayIndex, activityIndex, _ ->
                    nav.navigate(TripRoutes.editActivity(id, dayIndex, activityIndex))
                }
            )
        }

        // MainNavHost() 裡 TripRoutes.PickPlace 的 composable 區塊改成：
        composable(
            route = TripRoutes.PickPlace,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { entry ->
            val tripId = entry.arguments?.getString("tripId")!!

            PickPlaceScreen(
                padding = padding,
                onSearchClick = { nav.navigate(MiscRoutes.searchPlacesPick(tripId)) },
                onPick = { place ->
                    // 將 PlaceLite → JSON → Uri encode
                    val placeJson = android.net.Uri.encode(
                        encodePlaceArg(place)
                    )
                    nav.navigate(TripRoutes.addActivity(tripId, placeJson))
                }
            )
        }

        // 挑地點（從 Trip → PickPlace 進來）
        composable(
            route = MiscRoutes.SearchPlacesPick,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { entry ->
            val tripId = entry.arguments!!.getString("tripId")!!
            SearchPlacesScreen(
                onBack = { nav.navigateUp() },
                isPickingForTrip = true,
                onPick = { place ->
                    val placeJson = android.net.Uri.encode(encodePlaceArg(place))
                    nav.navigate(TripRoutes.addActivity(tripId, placeJson))
                }
            )
        }

        composable(
            route = TripRoutes.AddActivity,
            arguments = listOf(
                navArgument("tripId")    { type = NavType.StringType },
                navArgument("placeJson") { type = NavType.StringType }
            )
        ) { entry ->
            AddActivityScreen(
                padding = padding,
                tripId = entry.arguments!!.getString("tripId")!!,
                placeJson = entry.arguments!!.getString("placeJson")!!,
                nav = nav
            )
        }

        composable(
            route = TripRoutes.EditActivity,
            arguments = listOf(
                navArgument("tripId")       { type = NavType.StringType },
                navArgument("dayIndex")     { type = NavType.IntType },
                navArgument("activityIndex"){ type = NavType.IntType }
            )
        ) {
            AddActivityScreen(
                padding = padding,
                tripId = it.arguments!!.getString("tripId")!!,
                placeJson = null,   // 編輯不帶 placeJson
                nav = nav
            )
        }

        composable(
            route = TripRoutes.Chat,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) {
            TripChatScreen(padding = padding)
        }

        // ===== 其他 =====
        composable(MiscRoutes.SearchPlaces) {
            SearchPlacesScreen(
                onBack = { nav.navigateUp() },
                isPickingForTrip = false
            )
        }
        composable(MiscRoutes.SearchUsers)  { SearchUsersScreen(padding) }
        composable(MiscRoutes.EditProfile)  { EditProfileScreen(padding) }
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

    val title = when {
        route == Root.Explore.route      -> "Explore"
        route == Root.MyTrips.route      -> "My Trips"
        route == Root.Friends.route      -> "Friends"
        route == Root.Saved.route        -> "Saved"
        route == Root.Profile.route      -> "Profile"
        route == TripRoutes.Create       -> "Create Trip"
        route == TripRoutes.Preview      -> "Preview Trip"
        route == TripRoutes.Detail       -> "Trip Detail"
        route == TripRoutes.PickPlace -> "Pick Place"
        route == TripRoutes.AddActivity -> "Add Activity"
        route == TripRoutes.EditActivity -> "Edit Activity"
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

private val NO_BOTTOM_BAR_ROUTES = setOf(
    TripRoutes.Create,
    TripRoutes.Preview,
    TripRoutes.Detail,
    TripRoutes.Chat,
    MiscRoutes.SearchPlaces,
    MiscRoutes.SearchUsers,
    MiscRoutes.EditProfile,
    TripRoutes.PickPlace,
    TripRoutes.AddActivity,
    TripRoutes.EditActivity
)
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