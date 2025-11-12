package com.example.phambili_ma_africa.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String
    ): Result<String> {
        return try {
            // Create auth user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid 
                ?: return Result.failure(Exception("Failed to create user"))
            
            // Create customer profile in Firestore
            val customerData = hashMapOf(
                "ID" to userId,
                "Full_Name" to fullName,
                "Email" to email,
                "Phone" to phone,
                "Registration_Date" to FieldValue.serverTimestamp()
            )
            
            firestore.collection("customers")
                .document(userId)
                .set(customerData)
                .await()
            
            Result.success(userId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid 
                ?: return Result.failure(Exception("Failed to sign in"))
            
            Result.success(userId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun getCurrentUser() = auth.currentUser
    
    fun isUserLoggedIn() = auth.currentUser != null
    
    suspend fun resetPassword(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateEmail(newEmail: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            user.updateEmail(newEmail).await()
            
            // Update email in Firestore as well
            firestore.collection("customers")
                .document(user.uid)
                .update("Email", newEmail)
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePassword(newPassword: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            user.updatePassword(newPassword).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAccount(): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            // Delete user data from Firestore first
            firestore.collection("customers")
                .document(user.uid)
                .delete()
                .await()
                
            // Then delete the authentication account
            user.delete().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
