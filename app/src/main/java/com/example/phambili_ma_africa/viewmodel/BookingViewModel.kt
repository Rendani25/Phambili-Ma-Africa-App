package com.example.phambili_ma_africa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phambili_ma_africa.data.model.Booking
import com.example.phambili_ma_africa.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookingViewModel(
    private val repository: BookingRepository
) : ViewModel() {
    
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _bookingSuccess = MutableStateFlow<String?>(null)
    val bookingSuccess: StateFlow<String?> = _bookingSuccess
    
    init {
        loadBookings()
    }
    
    private fun loadBookings() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.getCustomerBookings().collect { bookingList ->
                    _bookings.value = bookingList
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load bookings: ${e.message}"
                _loading.value = false
            }
        }
    }
    
    fun submitBooking(
        serviceId: String,
        date: String,
        time: String,
        address: String,
        specialInstructions: String? = null,
        propertyType: String? = null,
        propertySize: String? = null,
        cleaningFrequency: String? = null
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.submitBooking(
                    serviceId,
                    date,
                    time,
                    address,
                    specialInstructions,
                    propertyType,
                    propertySize,
                    cleaningFrequency
                )
                
                result.fold(
                    onSuccess = { bookingId ->
                        _bookingSuccess.value = "Booking submitted successfully!"
                        loadBookings() // Refresh the bookings list
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to submit booking"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to submit booking: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.cancelBooking(bookingId)
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            _bookingSuccess.value = "Booking cancelled successfully"
                            loadBookings() // Refresh the bookings list
                        } else {
                            _error.value = "Failed to cancel booking"
                        }
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to cancel booking"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to cancel booking: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun getBookingsByStatus(status: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.getBookingsByStatus(status).collect { bookingList ->
                    _bookings.value = bookingList
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load bookings: ${e.message}"
                _loading.value = false
            }
        }
    }
    
    fun clearMessages() {
        _error.value = null
        _bookingSuccess.value = null
    }
    
    // Factory for creating the ViewModel with dependencies
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BookingViewModel(BookingRepository()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
