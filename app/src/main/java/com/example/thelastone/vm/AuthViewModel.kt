// vm/AuthViewModel.kt
package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.thelastone.data.repo.UserRepository

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun updateEmail(v: String)    { _state.value = _state.value.copy(email = v) }
    fun updatePassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun updateName(v: String)     { _state.value = _state.value.copy(name = v) }
    fun clearError()              { _state.value = _state.value.copy(error = null) }

    fun login() = viewModelScope.launch {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "請輸入 Email/密碼"); return@launch
        }
        _state.value = s.copy(loading = true, error = null)
        runCatching { userRepo.login(s.email, s.password) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "登入失敗") }
            .onSuccess  { _state.value = _state.value.copy(loading = false) }
        // 成功時不導頁：Session 變更會讓 AppScaffold 自動切到 Main
    }

    fun register() = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank() || s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "請完整填寫"); return@launch
        }
        _state.value = s.copy(loading = true, error = null)
        runCatching { userRepo.register(s.name, s.email, s.password) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "註冊失敗") }
            .onSuccess  { _state.value = _state.value.copy(loading = false) }
    }
}
