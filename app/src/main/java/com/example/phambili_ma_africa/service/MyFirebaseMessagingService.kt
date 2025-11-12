package com.example.phambili_ma_africa.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.phambili_ma_africa.BookingHistoryActivity
import com.example.phambili_ma_africa.MainActivity
import com.example.phambili_ma_africa.util.FCMHelper
import com.example.phambili_ma_africa.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val TAG = "FirebaseMsgService"
    
    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationHelper.createNotificationChannels(this)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Handle notification when app is in foreground
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            NotificationHelper.showGeneralNotification(
                this,
                notification.title ?: "Phambili Ma Africa",
                notification.body ?: ""
            )
        }
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Check if data needs special handling
            val type = remoteMessage.data["type"]
            when (type) {
                "booking_update" -> {
                    val bookingId = remoteMessage.data["booking_id"] ?: return
                    val status = remoteMessage.data["status"] ?: return
                    val serviceName = remoteMessage.data["service_name"] ?: "Service"
                    val oldStatus = remoteMessage.data["old_status"]
                    
                    // Show WhatsApp-like notification for booking updates
                    NotificationHelper.showBookingStatusNotification(
                        this,
                        bookingId,
                        serviceName,
                        oldStatus,
                        status
                    )
                    
                    // If we have the booking ID, fetch more details for a richer notification
                    FCMHelper.getBookingDetails(bookingId) { booking ->
                        if (booking != null) {
                            // We could enhance the notification with more booking details if needed
                            Log.d(TAG, "Got booking details for notification: ${booking.ID}")
                        }
                    }
                }
                "new_service" -> {
                    val serviceId = remoteMessage.data["service_id"]
                    val serviceName = remoteMessage.data["service_name"]
                    val title = "New Service Available"
                    val message = "Check out our new service: $serviceName"
                    NotificationHelper.showGeneralNotification(this, title, message)
                }
                else -> {
                    // Handle generic data message
                    val title = remoteMessage.data["title"] ?: "Phambili Ma Africa"
                    val message = remoteMessage.data["message"] ?: "You have a new notification"
                    NotificationHelper.showGeneralNotification(this, title, message)
                }
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // If you want to send messages to this application instance or
        // manage this app's subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }
    
    private fun sendRegistrationToServer(token: String) {
        // Use FCMHelper to register the token with Firestore
        Log.d(TAG, "Sending FCM token to server: $token")
        FCMHelper.registerFCMToken(applicationContext)
    }
    
    // We're now using NotificationHelper instead of this method
}
