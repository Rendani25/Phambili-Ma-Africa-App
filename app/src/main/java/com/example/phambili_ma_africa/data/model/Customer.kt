package com.example.phambili_ma_africa.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Customer(
    val ID: String = "",
    val Full_Name: String = "",
    val Email: String = "",
    val Phone: String = "",
    val Address: String = "",
    val Registration_Date: String = ""
)