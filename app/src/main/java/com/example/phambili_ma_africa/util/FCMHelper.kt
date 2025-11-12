package com.example.phambili_ma_africa.util

import android.content.Context
import android.util.Log
import com.example.phambili_ma_africa.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.example.phambili_ma_africa.util.NotificationHelper

/**
 * Helper class for Firebase Cloud Messaging operations
 */
object FCMHelper {
    private const val TAG = "FCMHelper"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Register the current user's FCM token with Firestore
     */
    fun registerFCMToken(context: Context) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "Cannot register FCM token: No user logged in")
            return
        }
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            
            // Get new FCM registration token
            val token = task.result
            
            // Save token to Firestore
            val userId = currentUser.uid
            val tokenData = hashMapOf(
                "fcm_token" to token,
                "device" to "android",
                "updated_at" to com.google.firebase.Timestamp.now()
            )
            
            // Store token in the user's document
            db.collection("users")
                .document(userId)
                .update("fcm_tokens", com.google.firebase.firestore.FieldValue.arrayUnion(tokenData))
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully registered")
                }
                .addOnFailureListener { e ->
                    // If update fails, try to set the document (in case it doesn't exist)
                    db.collection("users")
                        .document(userId)
                        .set(
                            hashMapOf("fcm_tokens" to listOf(tokenData)),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM token successfully registered (new document)")
                        }
                        .addOnFailureListener { e2 ->
                            Log.e(TAG, "Error registering FCM token", e2)
                        }
                }
            
            // Also store in the customers collection for compatibility
            db.collection("customers")
                .document(userId)
                .update("fcm_token", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully registered in customers collection")
                }
                .addOnFailureListener { e ->
                    // If update fails, try to set the document (in case it doesn't exist)
                    db.collection("customers")
                        .document(userId)
                        .set(
                            hashMapOf("fcm_token" to token),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM token successfully registered in customers collection (merge)")
                        }
                        .addOnFailureListener { e2 ->
                            Log.e(TAG, "Error registering FCM token in customers collection", e2)
                        }
                }
        }
    }
    
    /**
     * Subscribe to booking updates for the current user
     */
    fun subscribeToBookingUpdates() {
        val currentUser = auth.currentUser ?: return
        
        // Subscribe to a topic for this user's bookings
        val userTopic = "user_${currentUser.uid}"
        FirebaseMessaging.getInstance().subscribeToTopic(userTopic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to booking updates for user")
                } else {
                    Log.e(TAG, "Failed to subscribe to booking updates", task.exception)
                }
            }
    }
    
    /**
     * Unsubscribe from booking updates
     */
    fun unsubscribeFromBookingUpdates() {
        val currentUser = auth.currentUser ?: return
        
        // Unsubscribe from the user's topic
        val userTopic = "user_${currentUser.uid}"
        FirebaseMessaging.getInstance().unsubscribeFromTopic(userTopic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from booking updates")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from booking updates", task.exception)
                }
            }
    }
    
    /**
     * Listen for booking status changes for the current user
     * This sets up a Firestore listener that will be triggered when any booking status changes
     */
    fun listenForBookingStatusChanges(
        context: Context,
        onStatusChange: (bookingId: String, newStatus: String) -> Unit
    ): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null
        
        return db.collection("bookings")
            .whereEqualTo("Customer_ID", currentUser.uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                
                if (snapshots == null) {
                    Log.d(TAG, "No bookings found")
                    return@addSnapshotListener
                }
                
                for (dc in snapshots.documentChanges) {
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                        val bookingData = dc.document.data
                        val bookingId = dc.document.id
                        val oldStatus = dc.document.metadata.hasPendingWrites()
                        val newStatus = bookingData["Status"] as? String ?: bookingData["status"] as? String
                        val serviceName = bookingData["Service_Name"] as? String ?: "Service"
                        
                        if (newStatus != null) {
                            Log.d(TAG, "Booking $bookingId status changed to: $newStatus")
                            onStatusChange(bookingId, newStatus)
                            
                            // Show WhatsApp-like notification outside the app
                            NotificationHelper.showBookingStatusNotification(
                                context,
                                bookingId,
                                serviceName,
                                null, // We don't have the old status
                                newStatus
                            )
                        }
                    }
                }
            }
    }
    
    /**
     * Get booking details by ID
     */
    fun getBookingDetails(bookingId: String, onComplete: (Booking?) -> Unit) {
        db.collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { document ->
                val booking = document.toObject(Booking::class.java)?.copy(ID = document.id)
                onComplete(booking)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting booking details", e)
                onComplete(null)
            }
    }
}
