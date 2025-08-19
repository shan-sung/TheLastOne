package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.ui.screens.comp.Avatar
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ProfileViewModel

@Composable
fun ProfileScreen(padding: PaddingValues) {
    val vm: ProfileViewModel = hiltViewModel()
    val ui by vm.state.collectAsState()

    if (ui.loading) {
        LoadingState()
        return
    }

    val user = ui.me ?: return

    // M3 建議版面：上方個人資訊，接統計卡片與次要資訊
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 你的 Avatar 組件已存在：ui/screens/comp/Avatar.kt
                    Avatar(imageUrl = user.avatarUrl, size = 80.dp)

                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (user.friends.isNotEmpty()) {
                            Text(
                                "${user.friends.size} friends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            // 計數卡片：created / participating
            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(title = "Created", value = ui.createdCount.toString())
                    Divider(
                        modifier = Modifier.height(48.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    StatChip(title = "Participating", value = ui.participatingCount.toString())
                }
            }
        }

        // 可加：帳號與安全、通知設定等分組
        // item { ... }
    }
}

@Composable
private fun StatChip(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
