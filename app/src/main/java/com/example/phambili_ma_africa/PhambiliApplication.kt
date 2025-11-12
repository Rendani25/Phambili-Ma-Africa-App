package com.example.phambili_ma_africa

import android.app.Application
import com.example.phambili_ma_africa.util.FirebaseManager
import com.example.phambili_ma_africa.util.NotificationHelper

class PhambiliApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase with proper settings
        FirebaseManager.initialize(this)
        
        // Create notification channels
        NotificationHelper.createNotificationChannels(this)
    }
}
