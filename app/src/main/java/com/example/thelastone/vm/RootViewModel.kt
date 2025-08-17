package com.example.thelastone.vm

import androidx.lifecycle.ViewModel
import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// vm/RootViewModel.kt: 觀察 Session 用
@HiltViewModel
class RootViewModel @Inject constructor(
    session: SessionManager
) : ViewModel() {
    val auth: StateFlow<AuthUser?> = session.auth
}
