package com.example.phambili_ma_africa.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.phambili_ma_africa.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")
    
    // Get all available products with real-time updates
    fun getAvailableProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("Is_Available", true)
            .orderBy("Name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get products by category
    fun getProductsByCategory(category: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("Category", category)
            .whereEqualTo("Is_Available", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(ID = doc.id)
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get a specific product by ID
    suspend fun getProductById(productId: String): Product? {
        return try {
            val doc = productsCollection.document(productId).get().await()
            doc.toObject(Product::class.java)?.copy(ID = doc.id)
        } catch (e: Exception) {
            null
        }
    }
    
    // Get product categories
    fun getProductCategories(): Flow<List<String>> = callbackFlow {
        val listener = productsCollection
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
    
    // Search products
    suspend fun searchProducts(query: String): List<Product> {
        return try {
            // Firebase doesn't support direct text search, so we'll fetch all and filter
            val snapshot = productsCollection
                .whereEqualTo("Is_Available", true)
                .get()
                .await()
                
            val searchLower = query.lowercase()
            
            snapshot.documents.mapNotNull { doc ->
                val product = doc.toObject(Product::class.java)?.copy(ID = doc.id)
                if (product != null && 
                    (product.Name.lowercase().contains(searchLower) || 
                     product.Description.lowercase().contains(searchLower) ||
                     product.Category.lowercase().contains(searchLower))) {
                    product
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
