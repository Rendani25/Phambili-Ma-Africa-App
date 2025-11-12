package com.example.phambili_ma_africa.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Helper class for testing notifications
 * This is only for development and testing purposes
 */
object NotificationTestHelper {
    private const val TAG = "NotificationTestHelper"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Simulate a booking status change to test notifications
     */
    fun simulateBookingStatusChange(
        context: Context,
        bookingId: String,
        newStatus: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Cannot simulate booking status change: No user logged in")
            return
        }
        
        // First, get the booking to ensure it belongs to this user
        db.collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.e(TAG, "Booking not found: $bookingId")
                    return@addOnSuccessListener
                }
                
                val customerId = document.getString("Customer_ID")
                if (customerId != currentUser.uid) {
                    Log.e(TAG, "Cannot modify booking: Not owned by current user")
                    return@addOnSuccessListener
                }
                
                // Update the booking status
                db.collection("bookings")
                    .document(bookingId)
                    .update("Status", newStatus)
                    .addOnSuccessListener {
                        Log.d(TAG, "Booking status updated to: $newStatus")
                        
                        // Show a local notification for testing
                        val serviceName = document.getString("Service_Name") ?: "Service"
                        val oldStatus = document.getString("Status") ?: "unknown"
                        
                        NotificationHelper.showBookingStatusNotification(
                            context,
                            bookingId,
                            serviceName,
                            oldStatus,
                            newStatus
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating booking status", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting booking", e)
            }
    }
}
