package com.example.campusmarketplace.products

import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    @get:PropertyName("isSold")
    @set:PropertyName("isSold")
    var isSold: Boolean = false
)
