package com.example.phambili_ma_africa.data.model

import com.google.firebase.Timestamp


data class Booking(
    val ID: String = "",
    val Customer_ID: String = "",
    val Customer_Name: String = "",
    val Service_ID: String = "",
    val Service_Name: String = "",
    val Service_Type: String = "",
    val Date: String = "",
    val Time: String = "",
    val Address: String = "",
    val Special_Instructions: String? = null,
    val Status: String = "requested",  // requested, contacted, quoted, confirmed, completed, cancelled, declined
    val Total_Amount: Double? = null,
    val Duration: Int = 0,
    val Property_Type: String? = null,
    val Property_Size: String? = null,
    val Cleaning_Frequency: String? = null,
    val Created_At: Any? = null,
    val Updated_At: Any? = null
) {
    // Helper functions to handle both naming conventions
    fun getEffectiveCustomerId(): String = Customer_ID
    
    fun getEffectiveServiceId(): String = Service_ID
    
    fun getEffectiveServiceName(): String = Service_Name
    
    fun getEffectiveServiceType(): String = Service_Type
    
    fun getEffectiveDate(): String = Date
    
    fun getEffectiveTime(): String = Time
    
    fun getEffectiveAddress(): String = Address
    
    fun getEffectiveInstructions(): String = Special_Instructions ?: ""
    
    fun getEffectiveStatus(): String = Status
    
    fun getEffectivePropertyType(): String = Property_Type?.toString() ?: "Unknown Property"
    
    fun getEffectiveCreatedAt(): Any? = Created_At
}

