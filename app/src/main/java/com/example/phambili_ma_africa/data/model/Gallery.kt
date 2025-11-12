package com.example.phambili_ma_africa.data.model

import com.google.firebase.Timestamp

data class Gallery(
    val id: String = "",
    val filename: String = "",
    val url: String = "",
    val category: String = "general",
    val media_type: String = "image",
    val is_active: Boolean = true,
    val uploaded_at: Timestamp? = null,
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)
