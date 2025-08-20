package com.example.thelastone.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.Activity
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.TripDetailUiState
import com.example.thelastone.vm.TripDetailViewModel

@Composable
fun TripDetailScreen(
    padding: PaddingValues,
    viewModel: TripDetailViewModel = hiltViewModel(),
    onAddActivity: (tripId: String) -> Unit = {},
    onEditActivity: (tripId: String, dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _,_,_,_ -> },
    onDeleteActivity: (tripId: String, dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _,_,_,_ -> }
) {
    val state by viewModel.state.collectAsState()
    val perms = viewModel.perms.collectAsState().value
    val context = LocalContext.current

    when (val s = state) {
        is TripDetailUiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
        is TripDetailUiState.Error -> ErrorState(
            modifier = Modifier.padding(padding),
            message = s.message,
            onRetry = viewModel::reload
        )
        is TripDetailUiState.Data -> {
            val trip = s.trip
            var selected by rememberSaveable { mutableIntStateOf(0) }
            data class SheetData(val dayIndex: Int, val activityIndex: Int, val activity: Activity)
            var sheet by remember { mutableStateOf<SheetData?>(null) }

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { TripInfoCard(trip) }
                        dayTabsAndActivities(
                            trip = trip,
                            selected = selected,
                            onSelect = { selected = it },
                            onActivityClick = { dayIdx, actIdx, act ->
                                sheet = SheetData(dayIdx, actIdx, act)
                            }
                        )
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }

                // 只有建立者顯示 FAB
                if (perms?.canEditTrip == true) {
                    FloatingActionButton(
                        onClick = { onAddActivity(trip.id) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) { Icon(Icons.Filled.Add, null) }
                }
            }

            // 活動細節 Sheet：依權限切換唯讀/可編輯
            sheet?.let { data ->
                ActivityBottomSheet(
                    activity = data.activity,
                    readOnly = perms?.readOnly == true,            // ← 關鍵
                    canEdit = perms?.canEditTrip == true,          // 只給建立者
                    onDismiss = { sheet = null },
                    onEdit = {
                        onEditActivity(trip.id, data.dayIndex, data.activityIndex, data.activity)
                        sheet = null
                    },
                    onDelete = {
                        viewModel.removeActivity(data.dayIndex, data.activityIndex)
                        sheet = null
                    },
                    onGoMaps = { openInMaps(context, data.activity) },
                    onStart = {
                        // TODO: start flow
                        sheet = null
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityBottomSheet(
    activity: Activity,
    readOnly: Boolean,          // ← 新增：非 owner/member = true
    canEdit: Boolean,           // ← 新增：只有 owner = true
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGoMaps: () -> Unit,
    onStart: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var menuOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(activity.place.name, style = MaterialTheme.typography.titleLarge)

                // 仍保留 more-vert（你說要先留著），但唯讀時不顯示編輯/刪除
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (canEdit) {
                            DropdownMenuItem(text = { Text("編輯") }, onClick = { menuOpen = false; onEdit() })
                            DropdownMenuItem(text = { Text("刪除") }, onClick = { menuOpen = false; onDelete() })
                        } else {
                            // 你想放別的唯讀功能可在這裡加
                        }
                    }
                }
            }

            activity.place.address?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val time = listOfNotNull(activity.startTime, activity.endTime)
                .takeIf { it.isNotEmpty() }?.joinToString(" ~ ") ?: "未設定時間"
            Text(time, style = MaterialTheme.typography.bodyMedium)

            activity.place.rating?.let { r ->
                val txt = String.format("★ %.1f（%d）", r, activity.place.userRatingsTotal)
                Text(txt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))

            // 唯讀時不顯示操作按鈕
            if (!readOnly) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onGoMaps, modifier = Modifier.weight(1f)) { Text("Go to Maps") }
                    Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("Start") }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}


private fun openInMaps(context: Context, activity: Activity) {
    val lat = activity.place.lat
    val lng = activity.place.lng
    val name = activity.place.name

    val uri = when {
        lat != null && lng != null ->
            Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")
        else ->
            Uri.parse("geo:0,0?q=${Uri.encode(name)}")
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps") // 若裝了 Google Maps 就優先用
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // 沒有 Google Maps 時退回一般瀏覽器
        val web = if (lat != null && lng != null)
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        else
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(name)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, web))
    }
}