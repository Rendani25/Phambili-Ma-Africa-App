package com.example.phambili_ma_africa.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

/**
 * Singleton class to manage Firebase configurations and instances
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    
    /**
     * Initialize Firebase with proper settings
     */
    fun initialize(context: Context) {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(context)
            
            // Get instances
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            storage = FirebaseStorage.getInstance()
            
            // Configure Firestore settings
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Enable offline persistence
                .build()
            firestore.firestoreSettings = settings
            
            // Enable Firestore logging for debug builds
            if (isDebugBuild(context)) {
                enableFirestoreLogging()
            }
            
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }
    }
    
    /**
     * Enable Firestore logging for debugging
     */
    fun enableFirestoreLogging() {
        FirebaseFirestore.setLoggingEnabled(true)
        Log.d(TAG, "Firebase logging enabled")
    }
    
    /**
     * Disable Firestore logging
     */
    fun disableFirestoreLogging() {
        FirebaseFirestore.setLoggingEnabled(false)
        Log.d(TAG, "Firebase logging disabled")
    }
    
    /**
     * Check if running in debug mode
     */
    private fun isDebugBuild(context: Context): Boolean {
        return context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    
    /**
     * Get Firestore instance
     */
    fun getFirestore(): FirebaseFirestore {
        return firestore
    }
    
    /**
     * Get Auth instance
     */
    fun getAuth(): FirebaseAuth {
        return auth
    }
    
    /**
     * Get Storage instance
     */
    fun getStorage(): FirebaseStorage {
        return storage
    }
    
    /**
     * Clear local Firestore cache
     */
    fun clearFirestoreCache() {
        try {
            firestore.clearPersistence()
            Log.d(TAG, "Firestore cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Firestore cache: ${e.message}", e)
        }
    }
}
