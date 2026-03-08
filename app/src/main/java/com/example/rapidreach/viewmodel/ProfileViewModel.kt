package com.example.rapidreach.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rapidreach.data.model.User
import com.example.rapidreach.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.getUserData(userId)
            result.onSuccess { user ->
                _user.value = user
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message
                _user.value = null
            }
            _isLoading.value = false
        }
    }

    fun updateEmergencyContacts(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.updateUser(user)
            result.onSuccess {
                _user.value = user
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
            _isLoading.value = false
        }
    }
}
