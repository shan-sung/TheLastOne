package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thelastone.data.repo.UserRepository
import com.example.thelastone.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val session: SessionManager,
    private val userRepo: UserRepository
) : ViewModel() {

    data class Ui(
        val name: String = "",
        val email: String = "",
        val avatarUrl: String? = null,
        val saving: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(Ui())
    val state: StateFlow<Ui> = _state

    init {
        val me = session.auth.value?.user
        _state.value = Ui(
            name = me?.name.orEmpty(),
            email = me?.email.orEmpty(),
            avatarUrl = me?.avatarUrl
        )
    }

    fun onNameChange(s: String) { _state.update { it.copy(name = s) } }

    fun onAvatarPicked(uriString: String) {
        // 先立即預覽；真正上傳可放到 save 時做
        _state.update { it.copy(avatarUrl = uriString) }
    }

    fun save(onSuccess: () -> Unit) {
        val name = state.value.name.trim()
        if (name.isBlank()) {
            _state.update { it.copy(error = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                _state.update { it.copy(saving = true, error = null) }
                userRepo.updateProfile(name = name, avatarUrl = state.value.avatarUrl)
                _state.update { it.copy(saving = false) }
                onSuccess()
            } catch (t: Throwable) {
                _state.update { it.copy(saving = false, error = t.message ?: "Save failed") }
            }
        }
    }

    fun consumeError() { _state.update { it.copy(error = null) } }
}
