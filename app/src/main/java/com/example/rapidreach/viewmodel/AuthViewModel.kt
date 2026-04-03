package com.example.rapidreach.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rapidreach.data.model.User
import com.example.rapidreach.data.model.EmergencyContact
import com.example.rapidreach.data.model.MedicalInfo
import com.example.rapidreach.data.repository.AuthRepository
import com.example.rapidreach.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
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

    private val prefs = application.getSharedPreferences("rapidreach_prefs", android.content.Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", repo.isLoggedIn()))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        // Bug 4 — Session persisting
        val supabaseUser = SupabaseClient.client.auth.currentUserOrNull()
        if (supabaseUser != null) {
            fetchUserFromSupabase(supabaseUser.id)
        }
    }

    private fun mapError(exception: Throwable?): String {
        val message = exception?.message ?: return "An unexpected error occurred"
        return when {
            // Priority 1: Specific Authentication Failures
            message.contains("Invalid login credentials", ignoreCase = true) || 
            message.contains("Invalid credentials", ignoreCase = true) -> 
                "Incorrect email or password. Please try again."
            
            message.contains("User already exists", ignoreCase = true) -> 
                "An account with this email already exists."
            
            message.contains("Password should be", ignoreCase = true) -> 
                "Password is too weak. Please use a stronger password."

            // Priority 2: Network Issues
            message.contains("Unable to resolve host", ignoreCase = true) || 
            message.contains("Failed to connect", ignoreCase = true) ||
            message.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> 
                "No internet connection. Please check your network and try again."

            message.contains("rate limit", ignoreCase = true) -> 
                "Too many attempts. Please wait a moment before trying again."
                
            // Fallback for human-readable codes or messages
            message.length < 80 -> message
            else -> "An error occurred. Please try again."
        }
    }

    fun fetchUserFromSupabase(uid: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repo.getUserData(uid)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                _isLoggedIn.value = true
                prefs.edit().putBoolean("is_logged_in", true).putString("user_id", uid).apply()
                if (user != null) {
                    _uiState.value = AuthUiState.Success(user)
                }
            } else {
                _isLoggedIn.value = false
                prefs.edit().putBoolean("is_logged_in", false).remove("user_id").apply()
                _uiState.value = AuthUiState.Error(mapError(result.exceptionOrNull()))
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
                prefs.edit().putBoolean("is_logged_in", true).putString("user_id", user.id).apply()
                _uiState.value = AuthUiState.Success(user)
            } else {
                _uiState.value = AuthUiState.Error(mapError(result.exceptionOrNull()))
            }
        }
    }

    fun signup(
        name: String,
        email: String,
        phone: String,
        age: Int,
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
                age = age,
                gender = gender,
                userType = userType,
                emergencyContacts = emergencyContacts,
                medicalInfo = MedicalInfo()
            )

            val result = repo.signup(email.trim(), password, user)
            if (result.isSuccess) {
                val saved = result.getOrThrow()
                _currentUser.value = saved
                _isLoggedIn.value = true
                prefs.edit().putBoolean("is_logged_in", true).putString("user_id", saved.id).apply()
                _uiState.value = AuthUiState.Success(saved)
            } else {
                _uiState.value = AuthUiState.Error(mapError(result.exceptionOrNull()))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _currentUser.value = null
            _isLoggedIn.value = false
            prefs.edit().putBoolean("is_logged_in", false).remove("user_id").apply()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
