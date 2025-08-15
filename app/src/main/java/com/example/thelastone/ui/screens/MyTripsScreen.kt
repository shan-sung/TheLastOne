package com.example.thelastone.ui.screens

// ui/screens/mytrips/MyTripsScreen.kt
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thelastone.data.model.StateView
import com.example.thelastone.viewmodel.MyTripsViewModel

@Composable
fun MyTripsScreen(
    padding: PaddingValues,
    viewModel: MyTripsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTrips()
    }

    StateView(
        state = state,
        onRetry = { viewModel.loadTrips() }
    ) { trips ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(trips) { trip ->
                Text(trip.name)
            }
        }
    }
}
