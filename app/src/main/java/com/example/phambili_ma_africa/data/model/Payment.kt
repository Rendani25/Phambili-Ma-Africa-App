package com.example.phambili_ma_africa.data.model

import com.google.firebase.Timestamp

data class Payment(
    val id: String = "",
    val booking_id: String = "",
    val amount: Double = 0.0,
    val method: String = "card",
    val status: String = "pending",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)
