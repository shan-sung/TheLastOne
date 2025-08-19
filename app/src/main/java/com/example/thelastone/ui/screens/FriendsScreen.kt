package com.example.thelastone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.User
import com.example.thelastone.ui.screens.comp.Avatar
import com.example.thelastone.vm.FriendsViewModel
import com.example.thelastone.vm.IncomingItem

@Composable
fun FriendsScreen(padding: PaddingValues, vm: FriendsViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()

    // ★ Dialog 狀態（兩種身分分開記）
    var previewIncoming by remember { mutableStateOf<IncomingItem?>(null) }
    var previewFriend by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (ui.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (ui.error != null) {
            Text("載入失敗：${ui.error}", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.refresh() }) { Text("重試") }
            return
        }

        // ===== Section 1：等待回覆 =====
        Text("Waiting for reply", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (ui.incoming.isEmpty()) {
            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.large) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("目前沒有新的好友邀請")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(ui.incoming, key = { it.request.id }) { item ->
                    IncomingRow(
                        item = item,
                        onClick = { previewIncoming = item }, // ★ 點整列開 Dialog
                        onAccept = { vm.accept(item.request.id) },
                        onReject = { vm.reject(item.request.id) }
                    )
                    Divider()
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== Section 2：我的好友 =====
        Text("My Friends", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (ui.friends.isEmpty()) {
            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.large) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("還沒有好友，點右上搜尋加好友！")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(ui.friends, key = { it.id }) { u ->
                    ListItem(
                        headlineContent = { Text(u.name) },
                        supportingContent = { Text(u.email) },
                        leadingContent = { Avatar(imageUrl = u.avatarUrl, size = 40.dp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { previewFriend = u } // ★ 點整列開 Dialog
                    )
                    Divider()
                }
            }
        }
    }

    // ===== Dialogs =====

    // 1) 別人寄來的邀請（等待回覆）
    previewIncoming?.let { item ->
        IncomingRequestDialog(
            user = item.fromUser,
            onAccept = {
                vm.accept(item.request.id)
                previewIncoming = null
            },
            onReject = {
                vm.reject(item.request.id)
                previewIncoming = null
            },
            onDismiss = { previewIncoming = null }
        )
    }

    // 2) 已是好友
    previewFriend?.let { user ->
        FriendInfoDialog(
            user = user,
            onDismiss = { previewFriend = null }
        )
    }
}

@Composable
private fun IncomingRow(
    item: IncomingItem,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    ListItem(
        leadingContent = { Avatar(imageUrl = item.fromUser.avatarUrl, size = 40.dp) },
        headlineContent = { Text(item.fromUser.name) },
        supportingContent = { Text("向你發出好友邀請") },
        trailingContent = {
            Row {
                IconButton(onClick = onReject) { Icon(Icons.Filled.Close, contentDescription = "拒絕") }
                IconButton(onClick = onAccept) { Icon(Icons.Filled.Check, contentDescription = "同意") }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // ★ 點整列開 Dialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3Dialog(
    onDismissRequest: () -> Unit,
    tonalElevation: Dp = 6.dp,
    headline: @Composable () -> Unit,
    supportingText: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = tonalElevation
        ) {
            Column(Modifier.padding(24.dp)) {
                // 標題區
                headline()

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                if (supportingText != null) {
                    Spacer(Modifier.height(12.dp))
                    supportingText()
                }

                Spacer(Modifier.height(20.dp))

                // 動作列：靠右排列（次要在前、主要在後）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    actions()
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestDialog(
    user: User,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    M3Dialog(
        onDismissRequest = onDismiss,
        headline = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(imageUrl = user.avatarUrl, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${user.friends.size} friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        supportingText = {
            Text("向你發出好友邀請", style = MaterialTheme.typography.bodyMedium)
        },
        actions = {
            TextButton(onClick = onReject) { Text("拒絕") }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onAccept) { Text("同意") }
        }
    )
}

@Composable
private fun FriendInfoDialog(
    user: User,
    onDismiss: () -> Unit
) {
    M3Dialog(
        onDismissRequest = onDismiss,
        headline = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(imageUrl = user.avatarUrl, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${user.friends.size} friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        supportingText = null, // 這個不用內文就不放
        actions = {
            TextButton(onClick = onDismiss) { Text("關閉") }
        }
    )
}