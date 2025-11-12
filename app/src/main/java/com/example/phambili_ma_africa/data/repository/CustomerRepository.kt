package com.example.phambili_ma_africa.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.phambili_ma_africa.data.model.Customer
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class CustomerRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val COLLECTION_CUSTOMERS = "customers"
    }

    // Save or update customer data - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun saveCustomer(customer: Customer): Boolean {
        return try {
            val customerData = mapOf<String, Any>(
                "Full_Name" to customer.Full_Name,
                "Email" to customer.Email,
                "Phone" to customer.Phone,
                "Address" to customer.Address,
                "Registration_Date" to customer.Registration_Date
            )

            // If customer ID is provided, update existing, else create new
            if (customer.ID.isNotEmpty()) {
                firestore.collection(COLLECTION_CUSTOMERS)
                    .document(customer.ID)
                    .set(customerData, SetOptions.merge())
                    .await()
            } else {
                firestore.collection(COLLECTION_CUSTOMERS)
                    .add(customerData)
                    .await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Get customer by ID - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun getCustomerById(customerId: String): Customer? {
        return try {
            val document = firestore.collection(COLLECTION_CUSTOMERS)
                .document(customerId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(Customer::class.java)?.copy(ID = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Get customer by email - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun getCustomerByEmail(email: String): Customer? {
        return try {
            val query = firestore.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("Email", email)
                .limit(1)
                .get()
                .await()

            if (!query.isEmpty) {
                val document = query.documents[0]
                document.toObject(Customer::class.java)?.copy(ID = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Update customer profile - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun updateCustomerProfile(customerId: String, fullName: String, phone: String, address: String = ""): Boolean {
        return try {
            val updateData = mapOf<String, Any>(
                "Full_Name" to fullName,
                "Phone" to phone,
                "Address" to address
            )

            firestore.collection(COLLECTION_CUSTOMERS)
                .document(customerId)
                .update(updateData)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Check if customer exists and create if not - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun createCustomerIfNotExists(userId: String, email: String, fullName: String): Boolean {
        return try {
            val existingCustomer = getCustomerById(userId)
            if (existingCustomer == null) {
                val newCustomer = Customer(
                    ID = userId,
                    Full_Name = fullName,
                    Email = email,
                    Phone = "",
                    Address = "",
                    Registration_Date = "" // You might want to set this to current date
                )
                saveCustomer(newCustomer)
            } else {
                true // Customer already exists
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Alternative method using direct Firestore operations - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun saveCustomerDirect(userId: String, fullName: String, email: String, phone: String = "", address: String = ""): Boolean {
        return try {
            val customerData = mapOf<String, Any>(
                "Full_Name" to fullName,
                "Email" to email,
                "Phone" to phone,
                "Address" to address,
                "Registration_Date" to Timestamp.now().toDate().toString() // Set current date as registration date
            )

            firestore.collection(COLLECTION_CUSTOMERS)
                .document(userId)
                .set(customerData)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Update specific customer fields - MATCHING YOUR EXISTING CUSTOMER MODEL
    suspend fun updateCustomerField(customerId: String, field: String, value: Any): Boolean {
        return try {
            val updateData = mapOf<String, Any>(
                field to value
            )

            firestore.collection(COLLECTION_CUSTOMERS)
                .document(customerId)
                .update(updateData)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}