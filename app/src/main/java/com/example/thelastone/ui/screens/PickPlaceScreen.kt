package com.example.thelastone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.ui.navigation.TripRoutes
import com.example.thelastone.utils.encodePlaceArg

@Composable
fun PickPlaceScreen(
    padding: PaddingValues,
    nav: NavHostController,
    tripId: String,
) {
    var showDialog by remember { mutableStateOf(false) }

    // 這裡的 query 僅作 UI 外觀，實際不觸發搜尋（你之後再接 API）
    var query by remember { mutableStateOf("") }

    // 測試用的一筆 PlaceLite（只為了驗證導航，不做任何 API）
    val testPlace = remember {
        PlaceLite(
            placeId = "ChIJm7DSv__pQjQREiC8jZL4Aok", // 任意字串即可
            name = "Taipei 101",
            lat = 25.033968,
            lng = 121.564468,
            address = "110台北市信義區信義路五段7號",
            rating = 4.5,
            userRatingsTotal = 100000,
            photoUrl = null
        )
    }

    fun goAddActivityWith(place: PlaceLite) {
        val encoded = encodePlaceArg(place)
        nav.navigate(TripRoutes.addActivity(tripId, encoded))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜尋地點（目前不查 API）") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("測試：用「台北 101」新增") }

        // 模擬「點到搜尋結果 item → 跳 Dialog」
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("新增地點") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(testPlace.name, style = MaterialTheme.typography.titleMedium)
                        if (testPlace.address != null) Text(testPlace.address!!)
                        if (testPlace.rating != null && testPlace.userRatingsTotal != null) {
                            Text("評分：${testPlace.rating}（${testPlace.userRatingsTotal} 則）")
                        }
                        Text("座標：${testPlace.lat}, ${testPlace.lng}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        goAddActivityWith(testPlace)
                    }) { Text("新增") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("取消") }
                }
            )
        }
    }
}