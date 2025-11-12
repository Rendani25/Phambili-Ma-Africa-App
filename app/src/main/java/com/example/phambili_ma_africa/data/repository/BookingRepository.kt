package com.example.phambili_ma_africa.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.phambili_ma_africa.data.model.Booking
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookingsCollection = firestore.collection("bookings")
    
    // Submit a new booking
    suspend fun submitBooking(
        serviceId: String,
        date: String,
        time: String,
        address: String,
        specialInstructions: String? = null,
        propertyType: String? = null,
        propertySize: String? = null,
        cleaningFrequency: String? = null
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Check for duplicate booking
            val existingBooking = bookingsCollection
                .whereEqualTo("Customer_ID", userId)
                .whereEqualTo("Service_ID", serviceId)
                .whereEqualTo("Date", date)
                .whereNotIn("Status", listOf("cancelled", "rejected", "declined"))
                .get()
                .await()
            
            if (!existingBooking.isEmpty) {
                return Result.failure(Exception("You already have a booking for this service on this date"))
            }
            
            // Create booking
            val bookingData = hashMapOf(
                "Customer_ID" to userId,
                "Service_ID" to serviceId,
                "Date" to date,
                "Time" to time,
                "Address" to address,
                "Special_Instructions" to specialInstructions,
                "Status" to "requested",
                "Property_Type" to propertyType,
                "Property_Size" to propertySize,
                "Cleaning_Frequency" to cleaningFrequency,
                "Created_At" to FieldValue.serverTimestamp()
            )
            
            val docRef = bookingsCollection.add(bookingData).await()
            
            // Admin will see this instantly on the website dashboard!
            Result.success(docRef.id)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get customer bookings with real-time updates
    fun getCustomerBookings(): Flow<List<Booking>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }
        
        // Try to get bookings with both field naming conventions
        val listener = bookingsCollection
            .whereEqualTo("Customer_ID", userId)
            .orderBy("Created_At", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(bookings)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get a specific booking by ID
    suspend fun getBookingById(bookingId: String): Booking? {
        return try {
            val doc = bookingsCollection.document(bookingId).get().await()
            doc.toObject(Booking::class.java)?.copy(ID = doc.id)
        } catch (e: Exception) {
            null
        }
    }
    
    // Cancel a booking
    suspend fun cancelBooking(bookingId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not authenticated"))
                
            // Verify the booking belongs to this user
            val booking = getBookingById(bookingId)
            if (booking == null || booking.Customer_ID != userId) {
                return Result.failure(Exception("Booking not found or not authorized"))
            }
            
            // Update booking status
            bookingsCollection.document(bookingId)
                .update("Status", "cancelled")
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get bookings by status
    fun getBookingsByStatus(status: String): Flow<List<Booking>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }
        
        val listener = bookingsCollection
            .whereEqualTo("Customer_ID", userId)
            .whereEqualTo("Status", status)
            .orderBy("Date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(bookings)
            }
        
        awaitClose { listener.remove() }
    }
}
