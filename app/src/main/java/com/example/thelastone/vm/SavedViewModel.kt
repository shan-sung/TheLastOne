package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.SavedPlace
import com.example.thelastone.data.repo.SavedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<SavedPlace> = emptyList(),
    val savedIds: Set<String> = emptySet()
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repo: SavedRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavedUiState())
    val state: StateFlow<SavedUiState> = _state

    init {
        viewModelScope.launch {
            combine(
                repo.observeAll(),
                repo.observeIds()
            ) { list, ids -> list to ids }
                .onStart { _state.update { it.copy(loading = true) } }
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collect { (list, ids) ->
                    _state.value = SavedUiState(
                        loading = false,
                        error = null,
                        items = list,
                        savedIds = ids
                    )
                }
        }
    }

    fun toggle(place: PlaceLite) = viewModelScope.launch {
        repo.toggle(place)
    }
}
