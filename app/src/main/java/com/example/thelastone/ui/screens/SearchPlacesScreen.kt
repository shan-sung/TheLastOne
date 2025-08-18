package com.example.thelastone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.screens.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.vm.PlaceSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPlacesScreen(
    viewModel: PlaceSearchViewModel = hiltViewModel(),
    onPlaceSelected: (PlaceLite) -> Unit = {},
    onBack: () -> Unit = {}                // 👈 新增
) {
    val s = viewModel.state
    var active by rememberSaveable { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var selected by remember { mutableStateOf<PlaceLite?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    // 系統返回鍵（手勢/實體鍵）
    BackHandler { onBack() }               // 👈 新增

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(Modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            query = s.query,
            onQueryChange = viewModel::updateQuery,
            onSearch = {
                viewModel.searchNow()
                keyboard?.hide()
            },
            active = active,
            onActiveChange = { isActive ->  // 👇 不再觸發返回，只控管展開狀態
                active = isActive
            },
            placeholder = { Text("搜尋景點、餐廳、地址…") },
            leadingIcon = {
                IconButton(onClick = onBack) { // 👈 直接調用 onBack()
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            trailingIcon = {
                if (s.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            }
        ) {
            if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }

            LazyColumn(Modifier.fillMaxSize().imePadding()) {
                items(s.results, key = { it.placeId }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = {
                            Column {
                                p.address?.let { Text(it) }
                                if (p.rating != null && p.userRatingsTotal != null) {
                                    Text("★ ${"%.1f".format(p.rating)}（${p.userRatingsTotal}）",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = p
                                showDialog = true
                            }
                            .padding(horizontal = 4.dp)
                    )
                    Divider()
                }
            }
        }
    }
    if (showDialog) {
        PlaceDetailDialog(
            place = selected,
            mode = PlaceActionMode.ADD_TO_FAVORITE,
            onDismiss = { showDialog = false },
            onAddToFavorite = { /* TODO: 收藏 */ }
        )
    }
}