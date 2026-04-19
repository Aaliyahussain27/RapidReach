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

    fun updatePin(pin: String) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val updatedUser = currentUser.copy(sosPin = pin)
            val result = authRepository.updateUser(updatedUser)
            result.onSuccess {
                _user.value = updatedUser
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
            _isLoading.value = false
        }
    }

    fun updateCustomAudio(uri: android.net.Uri, context: android.content.Context) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sosRepo = com.example.rapidreach.data.repository.SosRepository(context)
                
                // Copy URI to a temporary file because Supabase upload needs a file or bytes
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = java.io.File(context.cacheDir, "temp_custom_audio.mp4")
                tempFile.outputStream().use { inputStream?.copyTo(it) }
                
                val uploadResult = sosRepo.uploadAudioFile(currentUser.id, tempFile.absolutePath)
                uploadResult.onSuccess { url ->
                    val updatedUser = currentUser.copy(customAudioUrl = url)
                    authRepository.updateUser(updatedUser)
                    _user.value = updatedUser
                }.onFailure { 
                    _errorMessage.value = it.message 
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
            _isLoading.value = false
        }
    }
}
