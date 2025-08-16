package com.example.thelastone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.Message
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.Trip
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.utils.isAtBottom
import com.example.thelastone.utils.rememberKeyboardOpen
import com.example.thelastone.vm.ChatUiState
import com.example.thelastone.vm.TripChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripChatScreen(
    padding: PaddingValues,
    viewModel: TripChatViewModel = hiltViewModel()
) {
    val s by viewModel.state.collectAsState()

    when (val st = s) {
        is ChatUiState.Loading -> LoadingState(modifier = Modifier.fillMaxSize().padding(padding))
        is ChatUiState.Error   -> ErrorState(modifier = Modifier.fillMaxSize().padding(padding), message = st.message, onRetry = {})
        is ChatUiState.Data -> {
            if (st.showTripSheet) {
                TripSheet(trip = st.trip, onDismiss = { viewModel.toggleTripSheet(false) })
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)              // ← 關鍵：只吃 Scaffold 的 padding
            ) {
                // 訊息清單
                MessagesList(
                    modifier = Modifier.weight(1f),
                    messages = st.messages,
                    onSelectSuggestion = viewModel::onSelectSuggestion
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.analyze() },
                        enabled = !st.analyzing,
                        label = { Text(if (st.analyzing) "分析中…" else "分析") },
                        leadingIcon = { Icon(Icons.Default.TipsAndUpdates, null) }
                    )
                    AssistChip(
                        onClick = { viewModel.toggleTripSheet(true) },
                        label = { Text("行程") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }
                    )
                }

                // 輸入列：加上 imePadding 讓鍵盤頂起來時不被遮住
                ChatInputBar(
                    value = st.input,
                    onValueChange = viewModel::updateInput,
                    onSend = viewModel::send,
                    modifier = Modifier
                        .imePadding()               // ← 鍵盤彈出時自動避讓
                        .navigationBarsPadding()    // ← 全螢幕下靠近底部也能避開
                )
            }
        }
    }
}

@Composable
private fun MessagesList(
    modifier: Modifier = Modifier,
    messages: List<Message>,
    onSelectSuggestion: (PlaceLite) -> Unit
) {
    val listState = rememberLazyListState()
    val keyboardOpen by rememberKeyboardOpen()

    // 第一次進入畫面 → 捲到底（用 scrollToItem: 同步、不閃爍）
    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    // 新訊息 &/or 鍵盤狀態改變；若目前在底部就自動捲到最後一則
    LaunchedEffect(messages.size, keyboardOpen) {
        if (messages.isNotEmpty() && listState.isAtBottom()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            val isAi = msg.isAi
            Surface(
                tonalElevation = if (isAi) 1.dp else 0.dp,
                shape = MaterialTheme.shapes.medium,
                color = if (isAi) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = if (isAi) "Trip AI" else "You",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(msg.text)

                    val sug = msg.suggestions
                    if (isAi && !sug.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sug.forEach { p ->
                                SuggestionCard(place = p, onClick = { onSelectSuggestion(p) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    place: PlaceLite,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                place.address?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            place.rating?.let {
                Text("★ $it", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("輸入訊息…") },
            singleLine = true
        )
        Button(onClick = onSend) { Text("送出") }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripSheet(
    trip: Trip?,
    onDismiss: () -> Unit
) {
    if (trip == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(trip.name, style = MaterialTheme.typography.titleLarge)
            Text("${trip.startDate} ~ ${trip.endDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            trip.days.forEachIndexed { idx, day ->
                Text("Day ${idx + 1} - ${day.date}", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.fillMaxWidth().padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    day.activities.forEach { act ->
                        Text("• ${act.startTime} ~ ${act.endTime}  ${act.place.name}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}