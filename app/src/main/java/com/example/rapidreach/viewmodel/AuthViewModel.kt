package com.example.rapidreach.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rapidreach.data.model.User
import com.example.rapidreach.data.model.EmergencyContact
import com.example.rapidreach.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AuthRepository()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(repo.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        // On app start, if user is already logged in, load their data
        val uid = repo.getCurrentUserId()
        if (uid != null) {
            viewModelScope.launch {
                val result = repo.getUserData(uid)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repo.login(email.trim(), password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _currentUser.value = user
                _isLoggedIn.value = true
                _uiState.value = AuthUiState.Success(user)
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun signup(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        userType: String,
        gender: String,
        emergencyContacts: List<EmergencyContact> = emptyList()
    ) {
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val user = User(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                gender = gender,
                userType = userType,
                emergencyContacts = emergencyContacts
            )
            val result = repo.signup(email.trim(), password, user)
            if (result.isSuccess) {
                val saved = result.getOrThrow()
                _currentUser.value = saved
                _isLoggedIn.value = true
                _uiState.value = AuthUiState.Success(saved)
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Signup failed"
                )
            }
        }
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
        _isLoggedIn.value = false
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
