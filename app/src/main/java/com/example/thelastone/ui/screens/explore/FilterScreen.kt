// ui/screens/explore/FilterScreen.kt
package com.example.thelastone.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.*
import com.example.thelastone.ui.screens.comp.PlaceCard
import com.example.thelastone.ui.screens.comp.placedetaildialog.PlaceDetailDialog
import com.example.thelastone.ui.screens.comp.placedetaildialog.comp.PlaceActionMode
import com.example.thelastone.ui.state.EmptyState
import com.example.thelastone.ui.state.ErrorState
import com.example.thelastone.ui.state.LoadingState
import com.example.thelastone.vm.ExploreViewModel
import com.example.thelastone.vm.SavedViewModel

@Composable
fun FilterScreen(
    padding: PaddingValues,
    viewModel: ExploreViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onApply: () -> Unit
) {
    val ui by viewModel.state.collectAsState()

    val savedVm: SavedViewModel = hiltViewModel()
    val savedUi by savedVm.state.collectAsState()

    var preview by rememberSaveable { mutableStateOf<PlaceLite?>(null) }

    // 延用 SpotFilters（但 UI 限定單選）
    var filters by rememberSaveable(stateSaver = SaverSpotFilters) { mutableStateOf(ui.filters) }

    // 面板展開狀態
    var cityExpanded by rememberSaveable { mutableStateOf(false) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var ratingExpanded by rememberSaveable { mutableStateOf(false) }

    // 評分輸入字串（改成按「套用」時才生效）
    var minText by rememberSaveable { mutableStateOf(filters.minRating.toString()) }
    var maxText by rememberSaveable { mutableStateOf(filters.maxRating.toString()) }

    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ====== 城市（單選，含「全部」） ======
        item {
            CollapsibleSection(
                title = "地點",
                expanded = cityExpanded,
                onToggle = { cityExpanded = !cityExpanded },
            ) {
                val selectedCity: City? = filters.cities.firstOrNull()
                SingleChoiceChipsWithAll(
                    options = City.values().toList(),
                    selected = selectedCity,                         // null = 全部
                    label = { it.label },
                    onSelect = { cOrNull ->
                        filters = if (cOrNull == null) {
                            filters.copy(cities = emptySet())        // 全部
                        } else {
                            filters.copy(cities = setOf(cOrNull))    // 單選
                        }
                    }
                )
            }
        }

        // ====== 類別（單選，含「全部」） ======
        item {
            CollapsibleSection(
                title = "類別",
                expanded = categoryExpanded,
                onToggle = { categoryExpanded = !categoryExpanded },
            ) {
                val selectedCat: Category? = filters.categories.firstOrNull()
                SingleChoiceChipsWithAll(
                    options = Category.values().toList(),
                    selected = selectedCat,                          // null = 全部
                    label = { it.label },
                    onSelect = { catOrNull ->
                        filters = if (catOrNull == null) {
                            filters.copy(categories = emptySet())    // 全部
                        } else {
                            filters.copy(categories = setOf(catOrNull))
                        }
                    }
                )
            }
        }

        // ====== 評分（兩個輸入框） ======
        item {
            CollapsibleSection(
                title = "Google 評分",
                expanded = ratingExpanded,
                onToggle = { ratingExpanded = !ratingExpanded },
            ) {
                RatingInputs(
                    minText = minText,
                    maxText = maxText,
                    onMinChange = { minText = it },
                    onMaxChange = { maxText = it }
                )
            }
        }

        // ====== 營業狀態：標題右側 Switch（不自動套用） ======
        item {
            SectionRow(
                title = "營業狀態",
                trailing = {
                    Switch(
                        checked = filters.openNow == true,
                        onCheckedChange = { checked ->
                            filters = filters.copy(openNow = if (checked) true else null)
                        }
                    )
                }
            )
        }

        // ====== 「套用」按鈕（位於結果上方，按下才套用） ======
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val (minF, maxF) = coerceRatingRange(minText, maxText)
                    minText = trimTrailingZeros(minF)
                    maxText = trimTrailingZeros(maxF)
                    filters = filters.copy(minRating = minF, maxRating = maxF)

                    viewModel.applyFilters(filters, limit = 30)
                    // ← 不要呼叫 onApply()
                }
            ) { Text("套用") }

        }

        // ====== 結果列表 ======
        item {
            when {
                ui.filtering -> LoadingState(Modifier.fillMaxWidth(), "套用篩選中…")
                ui.filteringError != null -> ErrorState(
                    modifier = Modifier.fillMaxWidth(),
                    title = "篩選失敗",
                    message = ui.filteringError!!,
                    onRetry = { viewModel.applyFilters(filters) }
                )
                ui.filtered.isEmpty() -> EmptyState(
                    Modifier.fillMaxWidth(), "沒有符合的景點", "換個條件試試看"
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.filtered.forEach { p ->
                        PlaceCard(
                            place = p,
                            onClick = { preview = p },
                            isSaved = p.placeId in savedUi.savedIds,
                            onToggleSave = { savedVm.toggle(p) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // 詳情 Dialog
    preview?.let { p ->
        val isSaved = p.placeId in savedUi.savedIds
        val mode = if (isSaved) PlaceActionMode.REMOVE_FROM_FAVORITE
        else PlaceActionMode.ADD_TO_FAVORITE
        PlaceDetailDialog(
            place = p,
            mode = mode,
            onDismiss = { preview = null },
            onAddToFavorite = { savedVm.toggle(p); preview = null },
            onRemoveFromFavorite = { savedVm.toggle(p); preview = null }
        )
    }
}

/* ---------- 共用 UI 元件 ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SingleChoiceChipsWithAll(
    options: List<T>,
    selected: T?,                   // null 代表「全部」
    label: (T) -> String,
    onSelect: (T?) -> Unit          // 傳入 null 表示選「全部」
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 「全部」chip
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("全部") }
        )
        // 其他選項
        options.forEach { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelect(item) },
                label = { Text(label(item)) }
            )
        }
    }
}


@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowSingleChoice(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { item ->
            FilterChip(
                selected = isSelected(item),
                onClick = { onSelect(item) },
                label = { Text(label(item)) }
            )
        }
    }
}

/** 評分兩個文字欄位（不自動套用；由上方「套用」按鈕統一觸發） */
@Composable
private fun RatingInputs(
    minText: String,
    maxText: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = minText,
            onValueChange = onMinChange,
            label = { Text("Min") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = maxText,
            onValueChange = onMaxChange,
            label = { Text("Max") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

/* ---------- 小工具 ---------- */

private fun coerceRatingRange(minStr: String, maxStr: String): Pair<Float, Float> {
    val minRaw = minStr.toFloatOrNull() ?: 0f
    val maxRaw = maxStr.toFloatOrNull() ?: 5f
    var lo = minRaw.coerceIn(0f, 5f)
    var hi = maxRaw.coerceIn(0f, 5f)
    if (lo > hi) lo = hi
    return lo to hi
}
private fun trimTrailingZeros(v: Float): String {
    val s = v.toString()
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

/* ---------- Saver 保持不變 ---------- */

val SaverSpotFilters: Saver<SpotFilters, Any> = listSaver(
    save = {
        listOf(
            it.cities.map(City::name),
            it.categories.map(Category::name),
            it.minRating,
            it.maxRating,
            when (it.openNow) { true -> 1; false -> 0; null -> -1 }
        )
    },
    restore = { raw ->
        val cities = (raw[0] as List<*>).map { City.valueOf(it as String) }.toSet()
        val cats   = (raw[1] as List<*>).map { Category.valueOf(it as String) }.toSet()
        fun numToFloat(v: Any) = when (v) {
            is Float -> v; is Double -> v.toFloat(); is Number -> v.toFloat(); else -> 0f
        }
        val minR = numToFloat(raw[2])
        val maxR = numToFloat(raw[3])
        val openNowEncoded: Int = when (val v = raw.getOrNull(4)) {
            is Int -> v
            is Boolean -> if (v) 1 else 0
            is String -> when {
                v.equals("null", true) -> -1
                v.equals("true", true) -> 1
                v.equals("false", true) -> 0
                else -> -1
            }
            else -> -1
        }
        val openNow: Boolean? = when (openNowEncoded) { 1 -> true; 0 -> false; else -> null }
        SpotFilters(cities = cities, categories = cats, minRating = minR, maxRating = maxR, openNow = openNow)
    }
)
