package com.example.phambili_ma_africa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phambili_ma_africa.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(repository.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess: StateFlow<String?> = _authSuccess
    
    init {
        // Check if user is already logged in
        _currentUser.value = repository.getCurrentUser()
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.signIn(email, password)
                
                result.fold(
                    onSuccess = { userId ->
                        _currentUser.value = repository.getCurrentUser()
                        _authSuccess.value = "Signed in successfully"
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to sign in"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to sign in: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun signUp(email: String, password: String, fullName: String, phone: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.signUp(email, password, fullName, phone)
                
                result.fold(
                    onSuccess = { userId ->
                        _currentUser.value = repository.getCurrentUser()
                        _authSuccess.value = "Account created successfully"
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to create account"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to create account: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _currentUser.value = null
        _authSuccess.value = "Signed out successfully"
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.resetPassword(email)
                
                result.fold(
                    onSuccess = { success ->
                        _authSuccess.value = "Password reset email sent"
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to send password reset email"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to send password reset email: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.updateEmail(newEmail)
                
                result.fold(
                    onSuccess = { success ->
                        _currentUser.value = repository.getCurrentUser()
                        _authSuccess.value = "Email updated successfully"
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to update email"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to update email: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.updatePassword(newPassword)
                
                result.fold(
                    onSuccess = { success ->
                        _authSuccess.value = "Password updated successfully"
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to update password"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to update password: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.deleteAccount()
                
                result.fold(
                    onSuccess = { success ->
                        _currentUser.value = null
                        _authSuccess.value = "Account deleted successfully"
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to delete account"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to delete account: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun clearMessages() {
        _error.value = null
        _authSuccess.value = null
    }
    
    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }
    
    // Factory for creating the ViewModel with dependencies
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(AuthRepository()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
