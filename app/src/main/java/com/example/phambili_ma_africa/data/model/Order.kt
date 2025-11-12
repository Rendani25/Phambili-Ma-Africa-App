package com.example.phambili_ma_africa.data.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val customer_id: String = "",
    val product_id: String = "",
    val payment_id: String = "",
    val status: String = "requested",
    val total_amount: Double = 0.0,
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)
