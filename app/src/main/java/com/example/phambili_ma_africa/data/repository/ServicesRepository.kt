package com.example.phambili_ma_africa.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.phambili_ma_africa.data.model.Service
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ServicesRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val servicesCollection = firestore.collection("services")
    
    // Real-time listener - updates automatically when admin changes services
    fun getAvailableServices(): Flow<List<Service>> = callbackFlow {
        val listener = servicesCollection
            .whereEqualTo("Is_Available", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(services)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get all services regardless of availability
    fun getAllServices(): Flow<List<Service>> = callbackFlow {
        val listener = servicesCollection
            .orderBy("Name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(services)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get services by category
    fun getServicesByCategory(category: String): Flow<List<Service>> = callbackFlow {
        val listener = servicesCollection
            .whereEqualTo("Category", category)
            .whereEqualTo("Is_Available", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(services)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get single service
    suspend fun getServiceById(serviceId: String): Service? {
        return try {
            val doc = servicesCollection.document(serviceId).get().await()
            doc.toObject(Service::class.java)?.copy(ID = doc.id)
        } catch (e: Exception) {
            null
        }
    }
    
    // Get service categories
    fun getServiceCategories(): Flow<List<String>> = callbackFlow {
        val listener = servicesCollection
            .whereEqualTo("Is_Available", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val categories = snapshot?.documents
                    ?.mapNotNull { it.getString("Category") }
                    ?.distinct()
                    ?: emptyList()
                
                trySend(categories)
            }
        
        awaitClose { listener.remove() }
    }
}
