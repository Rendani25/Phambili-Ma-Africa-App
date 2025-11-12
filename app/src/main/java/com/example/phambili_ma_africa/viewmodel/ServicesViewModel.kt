package com.example.phambili_ma_africa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phambili_ma_africa.data.model.Service
import com.example.phambili_ma_africa.data.repository.ServicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServicesViewModel(
    private val repository: ServicesRepository
) : ViewModel() {
    
    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services
    
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadServices()
        loadCategories()
    }
    
    private fun loadServices() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.getAvailableServices().collect { serviceList ->
                    _services.value = serviceList
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load services: ${e.message}"
                _loading.value = false
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getServiceCategories().collect { categoryList ->
                    _categories.value = categoryList
                }
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }
    
    fun getServicesByCategory(category: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                if (category.isEmpty() || category == "All") {
                    repository.getAvailableServices().collect { serviceList ->
                        _services.value = serviceList
                        _loading.value = false
                    }
                } else {
                    repository.getServicesByCategory(category).collect { serviceList ->
                        _services.value = serviceList
                        _loading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load services: ${e.message}"
                _loading.value = false
            }
        }
    }
    
    fun refreshServices() {
        loadServices()
    }
    
    // Factory for creating the ViewModel with dependencies
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ServicesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ServicesViewModel(ServicesRepository()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
