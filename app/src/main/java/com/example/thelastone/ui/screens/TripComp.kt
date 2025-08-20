package com.example.thelastone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.AgeBand
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.User
import com.example.thelastone.ui.screens.comp.Avatar
import com.example.thelastone.ui.state.EmptyState
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripInfoCard(trip: Trip, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(trip.name, style = MaterialTheme.typography.titleLarge)
            Text("${trip.startDate} → ${trip.endDate}", style = MaterialTheme.typography.bodyMedium)

            if (trip.activityStart != null && trip.activityEnd != null) {
                Text(
                    "活動時間：${trip.activityStart} ~ ${trip.activityEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 旅程屬性 chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (trip.totalBudget != null) {
                    AssistChip(onClick = {}, label = { Text("NT$${trip.totalBudget}") })
                }
                if (trip.avgAge != AgeBand.IGNORE) {
                    AssistChip(onClick = {}, label = { Text(trip.avgAge.label()) })
                }
                trip.styles.forEach { s -> AssistChip(onClick = {}, label = { Text(s) }) }
                trip.transportPreferences.forEach { t -> AssistChip(onClick = {}, label = { Text(t) }) }
            }

            // ==== NEW: Members ====
            if (trip.members.isNotEmpty()) {
                MembersSection(members = trip.members)
            }
        }
    }
}
// MembersSection：把 Avatar 換成 AvatarWithTooltip
@Composable
private fun MembersSection(
    members: List<User>,
    maxShown: Int = 5
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Members（${members.size}）",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            members.take(maxShown).forEach { user ->
                AvatarNameHint(user = user, size = 32.dp)
            }
            val more = members.size - maxShown
            if (more > 0) {
                AssistChip(onClick = {}, label = { Text("+$more") })
            }
        }
    }
}
@Composable
private fun AvatarNameHint(user: User, size: Dp) {
    var show by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .semantics { contentDescription = user.name } // a11y：等同 alt
    ) {
        // 頭貼：點一下顯示 1.5 秒
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    show = true
                    scope.launch {
                        kotlinx.coroutines.delay(1500)
                        show = false
                    }
                }
        ) {
            Avatar(imageUrl = user.avatarUrl, size = size)
        }

        // 簡易 tooltip：浮在頭貼上方一點點
        if (show) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = user.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}


private fun AgeBand.label(): String = when (this) {
    AgeBand.IGNORE -> "不列入"
    AgeBand.UNDER_17 -> "17以下"
    AgeBand.A18_25 -> "18–25"
    AgeBand.A26_35 -> "26–35"
    AgeBand.A36_45 -> "36–45"
    AgeBand.A46_55 -> "46–55"
    AgeBand.A56_PLUS -> "56以上"
}

@Composable
private fun ActivityRow(activity: Activity, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側文字內容
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(activity.place.name, style = MaterialTheme.typography.titleMedium)

                val time = listOfNotNull(activity.startTime, activity.endTime)
                    .takeIf { it.isNotEmpty() }?.joinToString(" ~ ") ?: "未設定時間"
                Text(time, style = MaterialTheme.typography.bodyMedium)

                val rating = activity.place.rating?.let {
                    String.format("★ %.1f（%d）", it, activity.place.userRatingsTotal ?: 0)
                }
                if (rating != null) {
                    Text(
                        rating,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    activity.place.address.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右側圖片
            if (!activity.place.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = activity.place.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun LazyListScope.dayTabsAndActivities(
    trip: Trip,
    selected: Int,
    onSelect: (Int) -> Unit,
    onActivityClick: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit
) {
    val monthDayFormatter = DateTimeFormatter.ofPattern("MM-dd")
    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    stickyHeader {
        ScrollableTabRow(
            selectedTabIndex = selected,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp,
            indicator = { pos ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(pos[selected]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            trip.days.forEachIndexed { i, d ->
                // 將 d.date 轉成 LocalDate 後再格式化（兼容 String / LocalDate / LocalDateTime）
                val monthDayText = when (val date = d.date) {
                    is java.time.LocalDate -> date.format(monthDayFormatter)
                    is java.time.LocalDateTime -> date.toLocalDate().format(monthDayFormatter)
                    is String -> java.time.LocalDate.parse(date, isoFormatter).format(monthDayFormatter)
                    else -> d.date.toString() // 萬一是奇怪型別，至少不會崩
                }

                Tab(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    text = {
                        Text(
                            text = "第 ${i + 1} 天\n$monthDayText",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    val day = trip.days.getOrNull(selected)
    if (day == null || day.activities.isEmpty()) {
        item {
            EmptyState(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                title = "沒有行程",
                description = "尚未產生任何每日活動"
            )
        }
    } else {
        items(day.activities.size, key = { idx -> day.activities[idx].id }) { idx ->
            val act = day.activities[idx]
            ActivityRow(activity = act) { onActivityClick(selected, idx, act) }
        }
    }
}