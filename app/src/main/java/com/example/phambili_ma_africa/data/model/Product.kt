package com.example.phambili_ma_africa.data.model

import com.google.firebase.Timestamp

data class Product(
    val ID: String = "",
    val Name: String = "",
    val Description: String = "",
    val Price: Double = 0.0,
    val Stock_Quantity: Int = 0,
    val Category: String = "",
    val Is_Available: Boolean = true,
    val Image_URL: String = ""
)
