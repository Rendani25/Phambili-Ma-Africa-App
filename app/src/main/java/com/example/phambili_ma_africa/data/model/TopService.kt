// data/model/TopService.kt
package com.example.phambili_ma_africa.data.model

import com.google.firebase.firestore.PropertyName

data class TopService(
    var id: String = "",

    @PropertyName("name")
    val name: String = "",

    @PropertyName("price")
    val price: String = "",

    @PropertyName("imageResId")
    val imageResId: Int = 0,

    @PropertyName("category")
    val category: String = "Cleaning",

    @PropertyName("isAvailable")
    val isAvailable: Boolean = true,

    @PropertyName("description")
    val description: String = ""
)