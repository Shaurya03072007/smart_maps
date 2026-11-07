package com.smartmaps.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmaps.app.data.repository.AuthRepository
import com.smartmaps.app.data.repository.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val session: UserSession? = null,
    val error: String? = null,
    val signupSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    val session = authRepository.getSessionOnce()
                    _uiState.update { it.copy(isLoggedIn = true, session = session) }
                } else {
                    _uiState.update { it.copy(isLoggedIn = false, session = null) }
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(username.trim(), password)
                .onSuccess { session ->
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, session = session) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun signup(username: String, password: String, name: String, phone: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signup(username.trim(), password, name.trim(), phone.trim(), role)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(isLoading = false, isLoggedIn = true, session = session, signupSuccess = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
