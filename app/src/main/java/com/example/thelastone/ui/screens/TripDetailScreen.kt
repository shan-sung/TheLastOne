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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.OpeningHoursSection
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.RatingSection
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
    readOnly: Boolean,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGoMaps: () -> Unit,
    onStart: () -> Unit,
    note: String = "",
    onNoteChange: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var menuOpen by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp) // 由各區塊自行控距
        ) {
            // Header：店名 + 更多選單
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.place.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                if (canEdit) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("編輯") },
                                onClick = { menuOpen = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("刪除") },
                                onClick = {
                                    menuOpen = false
                                    showConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            // Supporting text：地址（與標題距離更近）
            activity.place.address?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp)) // 關鍵：縮短與標題距離
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 時間 & Google 評分摘要（屬於 metadata，使用較淡層級）
            Spacer(Modifier.height(8.dp))
            val time = listOfNotNull(activity.startTime, activity.endTime)
                .takeIf { it.isNotEmpty() }?.joinToString(" ~ ") ?: "未設定時間"
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            OpeningHoursSection(
                hours = activity.place.openingHours ?: emptyList(),
                statusText = null
            )

            activity.place.rating?.let { r ->
                RatingSection(
                    rating = r,
                    totalReviews = activity.place.userRatingsTotal ?: 0   // ← ?: 0
                )
            }

            // 備註區：遵循表單與閱讀混合的 M3 風格
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium
            )

            // 行動按鈕列
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onGoMaps,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Go to Maps")
                }
                if (!readOnly) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }

    // 確認刪除對話框
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("確認刪除") },
            text = { Text("你確定要刪除此活動嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDelete()
                }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("取消")
                }
            }
        )
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