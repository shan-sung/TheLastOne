package com.example.thelastone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.User
import com.example.thelastone.ui.screens.comp.Avatar
import com.example.thelastone.vm.SearchUsersViewModel

@Composable
fun SearchUsersScreen(padding: PaddingValues, vm: SearchUsersViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("搜尋名稱或 Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            when {
                ui.loading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ui.error != null -> {
                    Text("搜尋失敗：${ui.error}", color = MaterialTheme.colorScheme.error)
                }
                ui.results.isEmpty() && ui.query.isNotBlank() -> {
                    Text("找不到相關使用者")
                }
                else -> {
                    LazyColumn {
                        items(ui.results, key = { it.id }) { u ->
                            UserRow(user = u, onClick = { vm.openDialog(u) })
                            Divider()
                        }
                    }
                }
            }
        }

        // ✅ 用 let 或先存變數，避免 delegated property 的 smart cast 問題
        ui.dialogUser?.let { dlg ->
            UserPreviewDialog(
                user = dlg,
                sending = ui.sending,
                sentSuccess = ui.sentSuccess,
                onSend = { vm.sendRequest() },
                onDismiss = { vm.closeDialog() }
            )
        }
    }
}

@Composable
private fun UserRow(user: User, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Avatar(imageUrl = user.avatarUrl, size = 40.dp) }, // ✅ 參數名修正
        headlineContent = { Text(user.name) },
        supportingContent = { Text(user.email) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        trailingContent = { /* 可放更多操作 */ },
    )
}

@Composable
private fun UserPreviewDialog(
    user: User,
    sending: Boolean,
    sentSuccess: Boolean,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (sentSuccess) {
                TextButton(onClick = onDismiss) { Text("關閉") }
            } else {
                TextButton(onClick = onSend, enabled = !sending) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("送出好友邀請")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        icon = { Avatar(imageUrl = user.avatarUrl, size = 48.dp) }, // ✅ 一致使用 imageUrl
        title = { Text(user.name) },
        text = { Text("好友數：${user.friends.size}") }
    )
}
