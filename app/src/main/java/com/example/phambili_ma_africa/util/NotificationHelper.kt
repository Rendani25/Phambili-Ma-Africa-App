package com.example.phambili_ma_africa.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.phambili_ma_africa.BookingHistoryActivity
import com.example.phambili_ma_africa.MainActivity
import com.example.phambili_ma_africa.R

/**
 * Helper class for creating and displaying notifications
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"
    
    // Notification channel IDs
    const val CHANNEL_BOOKING_UPDATES = "booking_updates_channel"
    const val CHANNEL_GENERAL = "general_notifications_channel"
    
    /**
     * Create notification channels for Android O and above
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Booking updates channel (high priority, like WhatsApp)
            val bookingChannel = NotificationChannel(
                CHANNEL_BOOKING_UPDATES,
                "Booking Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for booking status changes"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications from Phambili Ma Africa"
                enableLights(true)
                enableVibration(true)
            }
            
            // Register the channels
            notificationManager.createNotificationChannel(bookingChannel)
            notificationManager.createNotificationChannel(generalChannel)
            
            Log.d(TAG, "Notification channels created")
        }
    }
    
    /**
     * Show a WhatsApp-style booking status update notification
     */
    fun showBookingStatusNotification(
        context: Context,
        bookingId: String,
        serviceName: String,
        oldStatus: String?,
        newStatus: String
    ) {
        val notificationId = bookingId.hashCode()
        
        // Create an intent to open the BookingHistoryActivity
        val intent = Intent(context, BookingHistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("BOOKING_ID", bookingId)
        }
        
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, pendingIntentFlag
        )
        
        // Format the status for display
        val formattedStatus = newStatus.replaceFirstChar { it.uppercase() }
        val statusMessage = if (oldStatus != null) {
            "Changed from ${oldStatus.replaceFirstChar { it.uppercase() }} to $formattedStatus"
        } else {
            "Status: $formattedStatus"
        }
        
        // Create a messaging-style notification (WhatsApp-like)
        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder()
                .setName("You")
                .build()
        ).setConversationTitle("Booking Update")
        
        // Add a message from Phambili Ma Africa
        val sender = Person.Builder()
            .setName("Phambili Ma Africa")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .build()
        
        messagingStyle.addMessage(
            "Your booking for \"$serviceName\" has been updated.\n$statusMessage",
            System.currentTimeMillis(),
            sender
        )
        
        // Build the notification
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_BOOKING_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Booking Update")
            .setContentText("Your booking status has been updated to: $formattedStatus")
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            
        // Add a "View Booking" action
        val viewActionIntent = Intent(context, BookingHistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("BOOKING_ID", bookingId)
            putExtra("ACTION", "VIEW_DETAILS")
        }
        
        val viewPendingIntent = PendingIntent.getActivity(
            context, notificationId + 1, viewActionIntent, pendingIntentFlag
        )
        
        notificationBuilder.addAction(
            R.drawable.ic_notification,
            "View Booking",
            viewPendingIntent
        )
        
        // Show the notification
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Booking status notification shown for booking $bookingId: $newStatus")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }
    
    /**
     * Show a general notification
     */
    fun showGeneralNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, pendingIntentFlag
        )
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "General notification shown: $title")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }
}
