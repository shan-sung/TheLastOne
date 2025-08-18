package com.example.thelastone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.vm.PlaceSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPlacesScreen(
    viewModel: PlaceSearchViewModel = hiltViewModel(),
    onPlaceSelected: (PlaceLite) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val s by viewModel.state.collectAsState()
    var active by rememberSaveable { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var selected by remember { mutableStateOf<PlaceLite?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var requestedFocus by rememberSaveable { mutableStateOf(false) }

    // 返回鍵優先處理 UI 狀態
    BackHandler(enabled = showDialog || active || s.query.isNotEmpty()) {
        when {
            showDialog -> showDialog = false
            active -> active = false
            s.query.isNotEmpty() -> viewModel.updateQuery("")
            else -> onBack()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    LaunchedEffect(active) {
        if (active && !requestedFocus) {
            requestedFocus = true
            focusRequester.requestFocus()
            keyboard?.show()
        }
        if (!active) keyboard?.hide()
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
            onActiveChange = { active = it },
            placeholder = { Text("搜尋景點、餐廳、地址…") },
            leadingIcon = {
                IconButton(onClick = {
                    if (active) active = false else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            trailingIcon = {
                when {
                    s.loading -> {
                        // 小的 loading 指示（避免搶版面）
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    s.query.isNotEmpty() -> {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                }
            }
        ) {
            if (s.error != null) {
                ErrorState(
                    message = s.error!!,
                    onRetry = { viewModel.searchNow() }   // 或清空再搜
                )
            }else if (!s.loading && s.results.isEmpty() && s.query.isNotBlank()) {
                EmptyState(
                    title = "找不到與「${s.query}」相符的地點",
                    description = "試試別的關鍵字，或加入地區、類型，例如：\"台大 咖啡\""
                )
            }else {

                LazyColumn(Modifier.fillMaxSize().imePadding()) {
                    items(s.results, key = { it.placeId }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = {
                                Column {
                                    p.address?.let { Text(it) }
                                    if (p.rating != null && p.userRatingsTotal != null) {
                                        Text(
                                            "★ ${"%.1f".format(p.rating)}（${p.userRatingsTotal}）",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
    }

    if (showDialog) {
        PlaceDetailDialog(
            place = selected,
            mode = PlaceActionMode.ADD_TO_FAVORITE,
            onDismiss = { showDialog = false },
            onAddToFavorite = {
                showDialog = false
                selected?.let(onPlaceSelected)
            }
        )
    }
}
